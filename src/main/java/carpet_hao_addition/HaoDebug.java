package carpet_hao_addition;

/**
 * easy place 系列规则的调试输出开关。
 * <p>
 * 这些输出原本是无条件的 {@code System.out.println}:每次放置方块、每次协议解码都会打,
 * 普通玩法下也会往日志里刷 <code>[hao-easyplace]</code>。现在统一走这里,
 * 默认**静默**;需要排查时用启动器 JVM 参数 {@code -Dhao.debug=true}
 * 或环境变量 {@code HAO_DEBUG=true} 打开。
 */
public final class HaoDebug {
	/** 开关状态:{@code -Dhao.debug=true} 或 {@code HAO_DEBUG=true} 时为 true。 */
	public static final boolean ENABLED =
			Boolean.parseBoolean(System.getProperty("hao.debug", "false"))
					|| "true".equalsIgnoreCase(System.getenv("HAO_DEBUG"));

	private HaoDebug() {
	}

	/** 调试输出是否开启(热路径可先判断它以避免拼接字符串)。 */
	public static boolean ruleEnabled() {
		return ENABLED;
	}

	/** 输出一条调试信息;开关关闭时什么都不做。 */
	public static void log(Object message) {
		if (ENABLED) {
			System.out.println(message);
		}
	}

	/** 输出异常堆栈;开关关闭时什么都不做。 */
	public static void stackTrace(Throwable throwable) {
		if (ENABLED) {
			throwable.printStackTrace();
		}
	}

	/**
	 * 输出一条**不受调试开关控制**的错误信息,写入 stderr。
	 * <p>
	 * 只用于「出了这件事就说明有东西没生效」的异常/失败路径 —— 这类信息默认就该能看见,
	 * 套上 {@link #log} 会让问题在默认配置下彻底静默,排查时毫无线索。
	 * 正常流程里的诊断输出仍然一律走 {@link #log}。
	 *
	 * @param message 说明这次失败是什么
	 * @param cause   异常本体;为 {@code null} 时只输出说明
	 */
	public static void error(Object message, Throwable cause) {
		System.err.println("[carpet-hao-addition] " + message);
		if (cause != null) {
			cause.printStackTrace();
		}
	}
}
