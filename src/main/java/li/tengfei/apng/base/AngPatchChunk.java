package li.tengfei.apng.base;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch chunk for ANG
 * <p>
 * this chunk is designed for reusability, can be reused as an parser
 *
 * @author ltf
 * @since 16/12/10, 下午12:25
 */
public class AngPatchChunk extends ApngDataChunk {

    byte mData[];
    List<AngPatch> mPatches = new ArrayList<>();
    private DInt mDInt = new DInt();   // DInt parser
    private int mDataOffset;

    @Override
    protected void parseData(ApngDataSupplier data) {
        // get headers length and data length
        while (mDInt.addByte(data.readByte())) ;
        int headersLen = mDInt.getValue();
        int dataLen = length - mDInt.getSize() - headersLen;

        // init data cache, first for parse header, then used to contains patch data
        int i = headersLen < dataLen ? dataLen : headersLen;
        if (mData == null || mData.length < i) {
            mData = new byte[i];
        }

        // first, use data cache to parse headers
        data.read(mData, 0, headersLen);
        parsePatches(mData, 0, headersLen);

        // then, move patch data to cache
        data.read(mData, 0, dataLen);
    }

    /**
     * parse all patches (patchHeaders)
     *
     * @param buf    headers data buf
     * @param offset headers data offset in buf
     * @param len    headers data's len
     */
    private void parsePatches(byte[] buf, int offset, int len) {
        int patchOff = offset;
        int endOff = offset + len;
        mPatches.clear();
        mDataOffset = 0; // reset data offset
        while (patchOff < endOff) {
            patchOff = parsePatch(buf, patchOff);
        }
    }

    /**
     * parse a patch(Header) and add it to mPatches list
     *
     * @param buf    headers data buf
     * @param offset current header offset in buf
     * @return offset of [current_header_data_end + 1],
     * offset just after all parsed data,
     * maybe next header data's begin
     */
    private int parsePatch(byte[] buf, int offset) {
        AngPatch patch = new AngPatch();

        // step 1: get typeCodeHash
        mDInt.reset();
        offset += mDInt.read(buf, offset);
        patch.typeHash = mDInt.getValue();
        patch.typeHashIndex = (byte) (mDInt.getSize() - 1);

        // step 2: get items count
        mDInt.reset();
        offset += mDInt.read(buf, offset);
        int count = mDInt.getValue();

        // step 3: get items
        offset = parsePatchItems(buf, offset, count, patch);
        mPatches.add(patch);

        return offset;
    }

    /**
     * parse all patch items
     *
     * @param buf    headers data buf
     * @param offset patch-items data offset in buf
     * @param count  patch-items' count
     * @param patch  patch to contains all parsed patch-items
     * @return offset just after all parsed data
     */
    private int parsePatchItems(byte[] buf, int offset, int count, AngPatch patch) {
        patch.items = new AngPatchItem[count];
        for (int i = 0; i < count; i++) {
            patch.items[i] = new AngPatchItem();
            offset = parsePatchItem(buf, offset, patch.items[i]);
        }
        return offset;
    }

    /**
     * parse a patch item
     *
     * @param buf       headers data buf
     * @param offset    current patch-item's offset in buf
     * @param patchItem instance to contain the parsed data
     * @return offset just after parsed data
     */
    private int parsePatchItem(byte[] buf, int offset, AngPatchItem patchItem) {
        // step 1: get dstOffset
        mDInt.reset();
        offset += mDInt.read(buf, offset);
        patchItem.dstOffset = mDInt.getValue();

        // step 2: get dataSize
        mDInt.reset();
        offset += mDInt.read(buf, offset);
        patchItem.size = mDInt.getValue();

        // step 3: update srcOffset & data[]
        patchItem.data = this.mData;
        patchItem.srcOffset = mDataOffset;

        if (patchItem.size == 0) {
            // patchItem size == 0,  this is a DELETE patchItem, has 2 byte unsignedWord data
            mDataOffset += 2;
        } else {
            mDataOffset += patchItem.size;
        }

        return offset;
    }
}