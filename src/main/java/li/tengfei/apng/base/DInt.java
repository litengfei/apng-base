package li.tengfei.apng.base;

/**
 * Dynamic Integer
 * <p>
 * range: 0 ~ 536870911, max 4 byte;
 * 1 byte: 0 ~ 127
 * 2 byte: 128 ~ 16383
 * 3 byte: 16384 ~ 2097151
 * 4 byte: 2097152 ~ 536870911
 *
 * @author ltf
 * @since 16/12/9, 下午2:43
 */
public class DInt {

    public static final int VALUE_MASK = 0x7f;
    public static final byte END_BYTE_MASK = -128;
    public static final int MIN_DINT_VALUE = 0;
    public static final int MAX_1_BYTE_DINT_VALUE = 127;
    public static final int MAX_2_BYTE_DINT_VALUE = 16383;
    public static final int MAX_3_BYTE_DINT_VALUE = 2097151;
    public static final int MAX_4_BYTE_DINT_VALUE = 536870911;
    public static final int MAX_DINT_VALUE = MAX_4_BYTE_DINT_VALUE;

    protected int value;

    protected byte size;

    public int getValue() {
        return value;
    }

    public byte getSize() {
        return size;
    }

    /**
     * add one byte to the DInt
     *
     * @param data data byte
     * @return true if the DInt need next byte, false if not
     */
    public boolean addByte(byte data) {
        if (size > 3) throw new IllegalStateException("DInt read more than 4 bytes data");
        boolean notLastByte = size != 3;
        value |= notLastByte ? (data & VALUE_MASK) << (7 * size++) : (data & 0xff) << (7 * size++);
        return (data & END_BYTE_MASK) != 0 && notLastByte;
    }

    /**
     * read value from array by offset
     *
     * @param data   data array
     * @param offset DInt start offset
     * @return size of DInt
     */
    public byte read(byte[] data, int offset) {
        while (addByte(data[offset + size])) ;
        return size;
    }

    /**
     * read value from array by offset, return
     *
     * @param data        data array
     * @param offset      DInt start offset
     * @param readedBytes use the readedSize[0] to return count of readed bytes
     * @return the DInt value
     */
    public int readValue(byte[] data, int offset, byte[] readedBytes) {
        while (addByte(data[offset + size])) ;
        readedBytes[0] = size;
        return value;
    }

    /**
     * reset current value
     */
    public void reset() {
        value = 0;
        size = 0;
    }
}
