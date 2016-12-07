package li.tengfei.apng.base;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import static li.tengfei.apng.base.ApngConst.*;

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
            CODE_iCCP,
            CODE_sRGB,
            CODE_sBIT,
            CODE_gAMA,
            CODE_cHRM,

            CODE_PLTE,

            CODE_tRNS,
            CODE_hIST,
            CODE_bKGD,
            CODE_pHYs,
            CODE_sPLT
    };

    static {
        Arrays.sort(COPIED_TYPE_CODES);
    }

    private MappedByteBuffer mBuffer;
    private ApngMmapParserChunk mChunk;
    private int mNextPosAfterActl; // next chunk's offset after actl
    private PngStream mPngStream = new PngStream();

    public ApngReader(String apngFile) throws IOException, FormatNotSupportException {
        RandomAccessFile f = new RandomAccessFile(apngFile, "r");
        mBuffer = f.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        f.close();
        if (mBuffer.getInt() != PNG_SIG
                && mBuffer.getInt(4) != PNG_SIG_VER
                && mBuffer.getInt(8) != CODE_IHDR) {
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
        mPngStream.setIHDR(mChunk.duplicateData());

        // locate ACTL chunk
        while (mChunk.typeCode != CODE_acTL) {
            if (mChunk.typeCode == CODE_IEND || (mNextPosAfterActl = mChunk.parseNext()) < 0) {
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
            mPngStream.setHeadData(chunk.getTypeCode(), chunk.duplicateData());
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
        mPngStream.clearDataChunks();
        mChunk.unlockRead();
        // locate next FCTL chunk
        while (mChunk.typeCode != CODE_fcTL) {
            if (mChunk.typeCode == CODE_IEND || mChunk.parseNext() < 0) {
                return null;
            }
            handleOtherChunk(mChunk); // check and handle PLTE before FCTL (always the first one just after acTL)
        }
        ApngFrame frame = new ApngFrame();
        mChunk.assignTo(frame);

        // locate next IDAT or fdAt chunk
        while (mChunk.typeCode != CODE_IDAT && mChunk.typeCode != CODE_fdAT) {
            if (mChunk.typeCode == CODE_IEND || mChunk.parseNext() < 0) {
                return null;
            }
            handleOtherChunk(mChunk); // check and handle PLTE before FCTL (always the first one just after acTL)
        }
        // collect all consecutive dat chunks
        boolean needUpdateIHDR = true;
        int dataOffset = mChunk.getOffset();
        while (mChunk.typeCode == CODE_fdAT || mChunk.typeCode == CODE_IDAT) {
            if (needUpdateIHDR) {
                mPngStream.updateIHDR(frame.getWidth(), frame.getHeight());
                needUpdateIHDR = false;
            }
            if (mChunk.typeCode == CODE_fdAT) {
                mPngStream.addDataChunk(new Fdat2IdatChunk(mChunk));
            } else {
                mPngStream.addDataChunk(new ApngMmapParserChunk(mChunk));
            }
            mChunk.parseNext();
        }

        mPngStream.resetPos();
        mChunk.lockRead(dataOffset);
        frame.imageStream = mPngStream;
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
