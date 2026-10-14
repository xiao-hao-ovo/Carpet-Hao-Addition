package carpet_hao_addition;

/**
 * 记录「这一次轻松放置是快捷键触发的」。
 * <p>
 * 本模组的客户端触发挂在 Litematica 的 {@code WorldUtils.doEasyPlaceAction} 的 HEAD 上,
 * 而配套客户端的快捷键也是**反射调用同一个入口**(两个 mod 各注入一遍会重复放置,所以约定
 * 走同一个入口)。这样一来本模组**自己分辨不出调用来源** —— 只能由配套客户端在调用前后
 * 各打一次标记,本模组据此决定 {@code not_trigger} 时要不要处理:
 * <ul>
 *   <li><b>右键轻松放置</b>:配套客户端不打标记 —— {@code not_trigger} 时不处理
 *       (不放实体、也不弹选择器界面);</li>
 *   <li><b>配套客户端的快捷键</b>:打了标记 —— {@code not_trigger} 时照常处理。</li>
 * </ul>
 * 标记是**一次性**的:{@link #consumeHotkey()} 读一次就清掉,否则上一次快捷键会一直
 * 影响后面的右键。
 * <p>
 * 客户端逻辑跑在渲染/主线程上,这里就是普通的静态字段,不需要同步。
 */
public final class EntityPlacementTrigger {
	/** 本次轻松放置是否由配套客户端的快捷键触发。 */
	private static boolean hotkey;

	private EntityPlacementTrigger() {
	}

	/**
	 * 配套客户端在反射调用 {@code doEasyPlaceAction} **之前**调用。
	 * <p>
	 * 本模组对它是软依赖(编译期不引用),配套客户端用反射按名字找,所以这里的
	 * 类名与方法名**不要改动**。
	 */
	public static void beginHotkey() {
		hotkey = true;
	}

	/** 配套客户端在调用之后调用(异常路径也要走到),作为兜底清理。 */
	public static void endHotkey() {
		hotkey = false;
	}

	/** 本模组读一次并清掉:标记只在「当前这一次轻松放置」里有效。 */
	public static boolean consumeHotkey() {
		boolean value = hotkey;
		hotkey = false;
		return value;
	}
}
