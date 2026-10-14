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
 * 除了「是不是快捷键」,配套客户端按快捷键时还会把它自己配置的**一次放置数量**带过来
 * (见 {@link #beginHotkey(int)}):本模组正常的数量来自玩家自己的
 * {@code /easyPlaceEntityCount},而配套客户端的配置界面里也有同一项 —— 两处并存时
 * 玩家改哪一个都该说了算,所以这一次以快捷键带来的值为准。
 * <p>
 * 客户端逻辑跑在渲染/主线程上,这里就是普通的静态字段,不需要同步。
 */
public final class EntityPlacementTrigger {
	/** 本次轻松放置是否由配套客户端的快捷键触发。 */
	private static boolean hotkey;

	/** 本次快捷键指定的「一次放置几个」;-1 表示没指定(用玩家自己的设置)。 */
	private static int hotkeyCount = -1;

	private EntityPlacementTrigger() {
	}

	/**
	 * 配套客户端在反射调用 {@code doEasyPlaceAction} **之前**调用。
	 * <p>
	 * 本模组对它是软依赖(编译期不引用),配套客户端用反射按名字找,所以这里的
	 * 类名与方法名**不要改动**。
	 */
	public static void beginHotkey() {
		beginHotkey(-1);
	}

	/**
	 * 同上,但把配套客户端配置的「一次放置几个」一起带过来。
	 *
	 * @param count 1-64;小于 1 视为「不指定」(用玩家自己的 {@code /easyPlaceEntityCount})
	 */
	public static void beginHotkey(int count) {
		hotkey = true;
		hotkeyCount = count >= 1 ? count : -1;
	}

	/** 配套客户端在调用之后调用(异常路径也要走到),作为兜底清理。 */
	public static void endHotkey() {
		hotkey = false;
		hotkeyCount = -1;
	}

	/** 本模组读一次并清掉:标记只在「当前这一次轻松放置」里有效。 */
	public static boolean consumeHotkey() {
		boolean value = hotkey;
		hotkey = false;
		return value;
	}

	/** 本次快捷键指定的数量;-1 表示没指定。 */
	public static int hotkeyCount() {
		return hotkeyCount;
	}

	/**
	 * 清掉本次快捷键带来的数量。
	 * <p>
	 * 右键路径要调一次:否则上一次快捷键带过来的值会影响这次右键。
	 */
	public static void clearHotkeyCount() {
		hotkeyCount = -1;
	}
}
