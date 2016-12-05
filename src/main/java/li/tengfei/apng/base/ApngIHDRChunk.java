package li.tengfei.apng.base;

/**
 * IDHR Chunk
 *
 * @author ltf
 * @since 16/11/30, 下午4:45
 */
public class ApngIHDRChunk extends ApngDataChunk {
    private int width;
    private int height;
    private int bitDepth;
    private int colorType;
    private int compressMethod;
    private int filterMethod;
    private int interlaceMethod;

    @Override
    protected void parseData(ApngDataSupplier data) {
        this.width = data.readInt();
        this.height = data.readInt();
        this.bitDepth = data.readByte();
        this.colorType = data.readByte();
        this.compressMethod = data.readByte();
        this.filterMethod = data.readByte();
        this.interlaceMethod = data.readByte();
    }
}
