---
name: ha-addition-i18n
description: Carpet-Hao-Addition 的国际化规范:所有用户可见文本必须走 lang 文件(en_us.json/zh_cn.json 唯一真源并成对同步),键命名三类前缀(carpet.rule.*、<manager>.rule.*、<feature>.commands.*)、%s 占位、canHasTranslations 机制与 en_us 回退、键差集/引用核对校验。写任何规则名/描述/命令文本前先读这份。
---

# Carpet-Hao-Addition 国际化规范

国际化语言(中英双语)。**铁律:任何玩家可见文本(规则名、规则描述、命令输出、提示消息)禁止硬编码进 Java 代码,一律走 lang 文件,代码里只用键。**

## 唯一真源与同步

- lang 只存在共享层:`src/main/resources/assets/carpet-hao-addition/lang/{en_us,zh_cn}.json`(UTF-8,合法 JSON)。
- **每次新增/删除/改键,`en_us.json` 与 `zh_cn.json` 必须同一改动里同步**,不允许只改一份。
- 值语言:en_us 用英文,zh_cn 用简体中文;两条文件键集合必须完全一致(基线 53 键,只增不减且成对增)。

## 键命名(三类,照现状)

1. Carpet 默认 manager 规则(category `Hao`,走 `/carpet`):
   - `carpet.rule.<ruleName>.name` / `carpet.rule.<ruleName>.desc`
   - 分类名:`carpet.category.Hao`
   - 例:`carpet.rule.zoneguard.name`、`carpet.rule.snowyCalcite.desc`
2. 自定义 manager 规则(走 `/haoaddition`):
   - `haoaddition.rule.<ruleName>.name` / `.desc` + `haoaddition.category.haoaddition`
   - 例:`haoaddition.rule.exampleBoolean.name`
3. 命令/消息文本:
   - `<feature>.<层级>.commands.<子命令>.<动作>`,层级从功能名起逐级点分
   - 例:`playerNoEndPortalTeleport.commands.add.success`、`playerNoEndPortalTeleport.commands.help.globalMode`
   - 需要格式化占位用 `%s`(`Added %s to the end portal no-teleport list.`),禁止用字符串拼接拼句子。

规则名键直拼 `<ruleName>`(camelCase,与 `@Rule` 字段名一致);命令键用功能驼峰前缀 + 点分层级,一眼能定位到代码调用点。

## 供给机制(改键前须知)

- 规则 name/desc 经 `CarpetHaoAdditionExtension#canHasTranslations(String)` 供给:按需语言读 lang 资源,**目标语言缺失时自动回退 `en_us`**——所以 en_us 永远必须完整,是任何缺失语言的兜底。
- 同一批键同时作为普通 Minecraft lang 资源供客户端渲染(fabric.mod.json 声明资源路径),服务端/客户端共用;改键后需游戏内重载或重启验证。
- `canHasTranslations` 返回空且非 en_us 时也回退 en_us,保证 Carpet 规则解析器总能找到 name/desc 键。

## 写文本时的流程

1. 先定键名,再在代码里写键引用(`translatable("…")` / `translations.tr("…")` / `Messenger` 文本),**不要先写中文/英文字面量再补键**。
2. 同一次改动里成对加键(en_us + zh_cn)。
3. 提交前核对(可与 git skill 的提交核对合并):
   - 两 lang 文件键差集为空(无"仅 en"/"仅 zh");
   - 代码中 `translatable("…")`、`translations.tr("…")`、`translations.put("…")` 与消息键引用均能在 lang 中找到;
   - lang 中无已无引用的孤儿键(规则键由 Carpet 按名解析,无把握宁留勿删)。
4. Java 注释可用中文写设计意图(非用户可见),用户可见字符串字面量禁止。

## 自查命令(项目根执行)

```powershell
# 两文件键差集
python -c "import json;e=json.load(open('src/main/resources/assets/carpet-hao-addition/lang/en_us.json',encoding='utf-8'));z=json.load(open('src/main/resources/assets/carpet-hao-addition/lang/zh_cn.json',encoding='utf-8'));print('仅en:',sorted(set(e)-set(z)));print('仅zh:',sorted(set(z)-set(e)))"
```

代码引用 vs lang 缺失的完整扫描:用 python 正则抓 `translatable("…")` / `translations.tr("…")` / 消息键与 lang 键集合比对(先剔除注释再抓字符串)。
