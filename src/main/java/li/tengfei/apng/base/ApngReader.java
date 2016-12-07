package li.tengfei.apng.base;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Apng加载器（从Apng文件中读取每一帧的控制块及图像）
 *
 * @author ltf
 * @since 16/11/25, 上午8:14
 */
public class ApngReader {

    /**
     * chunks should be copied to each frame
     */
    public static final int[] COPIED_TYPE_CODES = {
            ApngConst.CODE_iCCP,
            ApngConst.CODE_sRGB,
            ApngConst.CODE_sBIT,
            ApngConst.CODE_gAMA,
            ApngConst.CODE_cHRM,

            ApngConst.CODE_PLTE,

            ApngConst.CODE_tRNS,
            ApngConst.CODE_hIST,
            ApngConst.CODE_bKGD,
            ApngConst.CODE_pHYs,
            ApngConst.CODE_sPLT
    };

    static {
        Arrays.sort(COPIED_TYPE_CODES);
    }

    private MappedByteBuffer mBuffer;
    private ApngMmapParserChunk mChunk;
    private int mNextPosAfterActl; // next chunk's offset after actl
    private PngStream pngStream = new PngStream();
    private int TEST_BUF_SIZE = 1024 * 300;
    private byte[] testBuf = new byte[TEST_BUF_SIZE];
    private int id = 0;

    public ApngReader(String apngFile) throws IOException, FormatNotSupportException {
        RandomAccessFile f = new RandomAccessFile(apngFile, "r");
        mBuffer = f.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        f.close();
        if (mBuffer.getInt() != ApngConst.PNG_SIG
                && mBuffer.getInt(4) != ApngConst.PNG_SIG_VER
                && mBuffer.getInt(8) != ApngConst.CODE_IHDR) {
            throw new FormatNotSupportException("Not a png/apng file");
        }
    }

    /**
     * prepare to read frames' data from the file
     *
     * @return animation control info
     * @throws IOException
     * @throws FormatNotSupportException
     */
    public ApngACTLChunk prepare() throws IOException, FormatNotSupportException {
        mChunk = new ApngMmapParserChunk(mBuffer);

        // locate IHDR
        mChunk.parsePrepare(8);
        mChunk.parse();
        pngStream.setIHDR(mChunk.duplicateData());

        // locate ACTL chunk
        while (mChunk.typeCode != ApngConst.CODE_acTL) {
            if (mChunk.typeCode == ApngConst.CODE_IEND || (mNextPosAfterActl = mChunk.parseNext()) < 0) {
                throw new FormatNotSupportException("No ACTL chunk founded, not an apng file. (maybe it's a png only)");
            }
        }

        ApngACTLChunk mActlChunk = new ApngACTLChunk();
        mChunk.assignTo(mActlChunk);
        return mActlChunk;
    }

    /**
     * handle other's chunk
     */
    private void handleOtherChunk(ApngMmapParserChunk chunk) throws IOException {
        if (Arrays.binarySearch(COPIED_TYPE_CODES, chunk.typeCode) >= 0) {
            pngStream.setHeadData(chunk.getTypeCode(), chunk.duplicateData());
        }
    }

    /**
     * get next frame control info & bitmap
     * <p>
     * !!! MUST RECYCLE THE BITMAP AFTER USED THE FRAME !!!
     *
     * @return next frame control info, or null if no next FCTL chunk & IDAT/FDAT chunk exists
     * @throws IOException
     */
    public ApngFrame nextFrame() throws IOException {
        pngStream.clearDataChunks();
        // locate next FCTL chunk
        while (mChunk.typeCode != ApngConst.CODE_fcTL) {
            if (mChunk.typeCode == ApngConst.CODE_IEND || mChunk.parseNext() < 0) {
                return null;
            }
            handleOtherChunk(mChunk); // check and handle PLTE before FCTL (always the first one just after acTL)
        }
        ApngFrame frame = new ApngFrame();
        mChunk.assignTo(frame);

        // locate next IDAT or fdAt chunk
        while (mChunk.typeCode != ApngConst.CODE_IDAT && mChunk.typeCode != ApngConst.CODE_fdAT) {
            if (mChunk.typeCode == ApngConst.CODE_IEND || mChunk.parseNext() < 0) {
                return null;
            }
            handleOtherChunk(mChunk); // check and handle PLTE before FCTL (always the first one just after acTL)
        }
        // collect all consecutive dat chunks
        boolean needUpdateIHDR = true;
        int dataOffset = mChunk.getOffset();
        while (mChunk.typeCode == ApngConst.CODE_fdAT || mChunk.typeCode == ApngConst.CODE_IDAT) {
            if (needUpdateIHDR) {
                pngStream.updateIHDR(frame.getWidth(), frame.getHeight());
                needUpdateIHDR = false;
            }
            if (mChunk.typeCode == ApngConst.CODE_fdAT) {
                pngStream.addDataChunk(new Fdat2IdatChunk(mChunk));
            } else {
                pngStream.addDataChunk(new ApngMmapParserChunk(mChunk));
            }
            mChunk.parseNext();
        }

//        pngStream.resetPos();
//        mChunk.lockRead(dataOffset);
//        saveToFile(pngStream, "/sdcard/gen" + id + ".png");
//        mChunk.unlockRead();

//        long pre = System.currentTimeMillis();
        pngStream.resetPos();
        mChunk.lockRead(dataOffset);
        int len = pngStream.read(testBuf, 0, TEST_BUF_SIZE);
        mChunk.unlockRead();
//        FileOutputStream fos = new FileOutputStream("/sdcard/gen" + id + "x.png");
//        fos.write(testBuf, 0, len);
//        fos.flush();
//        fos.close();
//        long read = System.currentTimeMillis();
//        frame.bitmap = BitmapFactory.decodeByteArray(testBuf, 0, len);
//        Log.d("ApngSurfaceView", String.format("r: %d, d: %d, l: %d", read - pre, System.currentTimeMillis() - read, len));

        id++;

        return frame;
    }

    private void saveToFile(InputStream is, String fn) {
        try {
            FileOutputStream fos = new FileOutputStream(fn);
            byte[] buf = new byte[1];
            int n = 0;
            while ((n = is.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reset read pointer to the ACTL chunk's position
     */
    public void reset() {
        if (mNextPosAfterActl > 0) {
            mChunk.parsePrepare(mNextPosAfterActl);
            mChunk.parse();
        }
    }
}
