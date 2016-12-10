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
    protected byte typeHashLen; // 1,2,3,4
    protected AngPatchItem[] items;
}
