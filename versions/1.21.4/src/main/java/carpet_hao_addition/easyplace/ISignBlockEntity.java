package carpet_hao_addition.easyplace;

/** 给标牌方块实体加"待打蜡"标记的接口(由 mixin 实现)。 */
public interface ISignBlockEntity {
	/** 是否标记了"待打蜡"。 */
	boolean hao$isPendingWaxed();

	/** 设置"待打蜡"标记。 */
	void hao$setPendingWaxed(boolean pending);
}
