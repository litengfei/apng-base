package li.tengfei.apng.base;

/**
 * Patch For ANG Frames
 * <p>
 * <p>
 * <p>
 * <p>
 * codeTypeHash count  PatchItem [...count times]
 *
 * @author ltf
 * @since 16/12/9, 上午8:33
 */
public class AngPatch {
    protected int typeHash;
    protected byte typeHashIndex; // maybe 0,1,2,3, = typeHashLength -1
    protected AngPatchItem[] items;

    /**
     * calculate 1~4 bytes length hash of the typeCode
     *
     * @param typeCode typeCode to calculate the hash array
     * @return 4 int array, [0] contains 1 byte length typeHash, [1] contains 2byte length typeHash ...
     */
    public static int[] typeCodeHashes(int typeCode) {
        int[] hashes = new int[4];
        int hash = typeCodeHash(typeCode);
        hashes[0] = hash & DInt.MAX_1_BYTE_DINT_VALUE;
        hashes[1] = hash & DInt.MAX_2_BYTE_DINT_VALUE;
        hashes[2] = hash & DInt.MAX_3_BYTE_DINT_VALUE;
        hashes[3] = hash & DInt.MAX_4_BYTE_DINT_VALUE;
        return hashes;
    }

    /**
     * calculate type code's hash
     */
    public static int typeCodeHash(int v) {
        v -= (v << 6);
        v ^= (v >> 17);
        v -= (v << 9);
        v ^= (v << 4);
        v -= (v << 3);
        v ^= (v << 10);
        v ^= (v >> 15);
        return v;
    }
}
