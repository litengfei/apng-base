package li.tengfei.apng.base;

/**
 * @author ltf
 * @since 16/11/29, 下午12:32
 */
public interface ApngDataSupplier {

    /**
     * read int from data and move the pointer 4 byte ahead
     */
    int readInt();

    /**
     * read int from data and move the pointer 2 byte ahead
     */
    short readShort();

    /**
     * read int from data and move the pointer 1 byte ahead
     */
    byte readByte();

    /**
     * read byte arrays from data and move the pointer ahead by readed data length
     *
     * @param dst       destination array
     * @param dstOffset first byte offset in destination array
     * @param size      wanted size
     * @return readed bytes count
     */
    int read(byte[] dst, int dstOffset, int size);

    /**
     * move the pointer ahead by distance bytes
     */
    void move(int distance);
}
