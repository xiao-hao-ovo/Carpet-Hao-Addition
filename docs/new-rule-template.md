# 新增一条 Carpet 规则（模板 / 配方）

本项目的规则分散在 **12 个代码基**里，加一条规则要同时照顾它们：

- **1.21.x 的 8 层共享根 `src/`**（1.21 / 1.21.1 / 1.21.2 / 1.21.4 / 1.21.6 / 1.21.8 / 1.21.10 / 1.21.11）
- **26.x 的 3 个独立子工程**：`modern/26.1.2`、`modern/26.2`、`modern/26.3`（各自一份 `src`，互不复用）

> 约定：规则名带 `hao` 前缀。Carpet 的规则名是**全局唯一**的，而我们和其他 Carpet 扩展共存，
> 它们可能注册了同名规则；重名只有一个生效，所以我们的都加 `hao`。

---

## 0. 要改的地方一览

| 要改什么 | 1.21.x（根 src，8 层共享） | 26.1.2 / 26.2 / 26.3（各自独立） |
|---|---|---|
| ① Settings 类 | `src/main/java/carpet_hao_addition/XxxSettings.java` | 各层各一份 |
| ② 语言文件 | `src/main/resources/assets/carpet-hao-addition/lang/{zh_cn,en_us}.json` | 各层各一份 |
| ③ 注册 | 根 `CarpetHaoAdditionExtension` | 各层各一份 |
| ④ 挂 mixin（仅当需要改原版行为） | `versions/<mc>/src/main/resources/carpet-hao-addition.mixins.json`（**8 份**） | 各层一份 |

**要想保持"12 层规则完全一致"，四类都得全铺。** 只铺一部分会让 `/carpet` 列出的规则数不一致。

---

## 1. Settings 类（布尔规则）

`src/main/java/carpet_hao_addition/ExampleNewRuleSettings.java`：

```java
package carpet_hao_addition;

import carpet.api.settings.Rule;

/**
 * 示例规则:一句话说明它干什么。
 * <p>
 * 需要的话再写:为什么需要、和哪条规则互斥、有什么副作用。
 */
public class ExampleNewRuleSettings {
	/** Carpet 规则名(注册在 carpet 默认管理器)。 */
	public static final String RULE_NAME = "haoExampleNewRule";

	/** 是否启用 xxx。 */
	@Rule(categories = {"Hao"})
	public static boolean haoExampleNewRule = false;

	/** 规则是否开启(业务代码里统一用它,别到处直接读静态字段)。 */
	public static boolean isEnabled() {
		return haoExampleNewRule;
	}
}
```

要点：
- `categories = {"Hao"}` —— 所有本项目规则都归在 `Hao` 分类下；
- 字段名 = 规则名（`@Rule` 认字段名），`RULE_NAME` 常量供别处查表用；
- 默认值建议 `false`（新规则默认关闭，避免影响老存档/服务器）。

---

## 2. 多值规则（像 `haoProjectionWaterlogged` / `haoProjectionPlacement`）

用 `enum` 做取值，`/carpet` 里会把枚举常量名转成小写显示，例如
`WITH_COMPOSTER_LEVEL` → `with_composter_level`。

```java
package carpet_hao_addition;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;

public class ExampleNewRuleSettings {
	public static final String RULE_NAME = "haoExampleNewRule";

	/** 规则取值。 */
	public enum Mode {
		/** 关闭。 */
		FALSE,
		/** 开启,做 A。 */
		TRUE,
		/** 开启,做 B。 */
		SOMETHING_ELSE
	}

	@Rule(categories = {"Hao"})
	public static Mode haoExampleNewRule = Mode.FALSE;

	/** 权威读取规则取值(没注册/取值异常时回退默认)。 */
	public static Mode mode() {
		CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(RULE_NAME);
		if (rule != null && rule.value() instanceof Mode value) {
			return value;
		}
		return Mode.FALSE;
	}

	public static boolean isEnabled() {
		return mode() != Mode.FALSE;
	}
}
```

> 为什么走 `settingsManager.getCarpetRule(...)` 而不是直接读静态字段：静态字段在客户端/服务端
> 各自持有一份，规则改动不一定会同步到位；查 manager 拿到的是当前权威值，取值异常时还能回退默认。

---

## 3. 语言文件

在 4 个文件里各加两行（**根 src 的 2 个 + 26.x 每层的 2 个**）：

`zh_cn.json`
```json
  "carpet.rule.haoExampleNewRule.name": "示例新规则",
  "carpet.rule.haoExampleNewRule.desc": "开启后做某某事。默认关闭。",
```

`en_us.json`
```json
  "carpet.rule.haoExampleNewRule.name": "Example New Rule",
  "carpet.rule.haoExampleNewRule.desc": "Does something when enabled. Off by default.",
```

多值规则建议把取值写进 `desc`（用户能在 `/carpet <规则名>` 的帮助里看到），例如：
`取值:false=关闭(默认);true=…;something_else=…。`

---

## 4. 注册（`CarpetHaoAdditionExtension`）

在 `parseSettingsClass(...)` 那一串里加一行即可（**根 src 的 + 26.x 每层各一处**）：

```java
        CarpetServer.settingsManager.parseSettingsClass(ProjectionPlacementSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(ExampleNewRuleSettings.class);   // ← 新增
        CarpetServer.settingsManager.parseSettingsClass(WackoBeaconsSettings.class);
```

---

## 5. 需要改原版行为时才挂 mixin

1. 类放 `carpet_hao_addition/mixin/XxxMixin.java`，写 `@Mixin(目标类.class)`；
2. **在对应的 `carpet-hao-addition.mixins.json` 里登记**：
   - 服务端/双端逻辑放 `mixins` 段；纯客户端类（GUI/渲染）放 `client` 段，**放错段会在另一侧刷 `@Mixin target ... was not found`**；
   - 1.21.x 是 **8 份** json（`versions/<mc>/src/main/resources/`），26.x 各 1 份；
3. 运行期铁律（编译期查不出来，只能实测）：
   - mixin 里的普通方法**必须 `private`**，共用逻辑抽到普通类里；
   - `@Inject` **只能注入目标类自己声明的方法** —— 目标类没覆写就换父类、换方法，写之前先核对：

```powershell
# 例:确认 1.21.8 的 BulbBlock 是否覆写了某方法(0 个就是没覆写)
$J = "C:\Users\Administrator\.gradle\caches\fabric-loom\1.21.8\net.fabricmc.yarn.1_21_8.1.21.8+build.1-v2\common-unpicked.jar"
& "F:\JDK-25\bin\javap.exe" -p -cp $J net.minecraft.block.BulbBlock
```

> 26.x 的 `minecraft-merged.jar` 就是官方名（可直接 javap）；1.21.x 的是混淆名，要走上面这个 named jar。

---

## 6. 验证

```powershell
# 1.21.x 八层(根 src 共享)
.\gradlew.bat build

# 26.x 三个独立子工程
cd modern\26.1.2; .\gradlew.bat build
cd ..\26.2;      .\gradlew.bat build
cd ..\26.3;      .\gradlew.bat build
```

进游戏 `/carpet` 看列表：新规则在、取值个数对、默认值对，就说明四步齐了。

**规则一致性快速检查**（每个代码基各有多少条、名字是否一致）：

```powershell
$roots = @("src") + (Get-ChildItem versions,modern -Directory | ForEach-Object { Join-Path $_.FullName "src" })
foreach ($r in $roots) {
  $names = Get-ChildItem -Recurse -File $r -Filter *.java -ErrorAction SilentlyContinue |
    Select-String -Pattern 'public static [\w<>\[\]]+ (\w+)\s*=' -Context 1,1 |
    Where-Object { $_.Context.PreContext -match '@Rule' } |
    ForEach-Object { $_.Matches[0].Groups[1].Value } | Sort-Object -Unique
  Write-Output ("{0,-24} {1,3} 条: {2}" -f $r, $names.Count, ($names -join ", "))
}
```

---

## 7. 已知的坑

- **规则名重名**：和其他 Carpet 扩展（其它扩展 / 其它扩展 …）重名会互相顶掉，一律加 `hao` 前缀。
- **只改一层**：迭代阶段可以先只改 `versions/1.21.8` 或 `modern/26.3` 验证；但**发布前必须 12 层齐全**。
- **数据包配方的 `result.count`**：26.x 限制在 `[1, 99]`，超限会让**整个 recipe 注册表**加载失败（连原版配方一起消失）。
- **mixin 注入失败是运行期才炸**：`BUILD SUCCESSFUL` 不代表能加载，改完 mixin 一定要实机跑一次。
