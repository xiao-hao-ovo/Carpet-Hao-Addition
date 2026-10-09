# 命令

命令注册在 Carpet 的默认命令树里。除 `/hao` 外，各命令都受**对应规则**门控：规则关闭时整棵命令树（含 `help`）不可见、不可执行。

## 示例规则 (`/hao`)

### 语法

`/hao`

### 效果

- 打印 Carpet-Hao-Addition 的两个示例规则（`exampleBoolean` / `exampleString`）的当前值

## 局部侦测器禁用 (`/zoneguard`)

需要规则 `/carpet zoneguard true` 开启；关闭时整棵命令树（含 `help`）不可见。

### 语法

- `/zoneguard set <id> <from> <to>` —— 设置立方禁用区域
- `/zoneguard view` —— 列出已配置的区域
- `/zoneguard clear <id>` —— 清除指定区域
- `/zoneguard op add|remove <player>` —— 增删白名单玩家
- `/zoneguard op list` —— 查看白名单
- `/zoneguard help` —— 显示使用说明

### 效果

- `set` 把 `<from>` 到 `<to>` 两个对角坐标围成的立方体设为禁用区（自动归一化两角，不必按顺序给），配置随世界存档持久化
- `clear` 删除区域后，会给区域内已加载区块中“面对面卡住的侦测器对”各计划一次刻，让它们恢复运作（反馈里会报告恢复的对数，0 表示该区域没有已加载的这类侦测器对）
- 参数简化写法以 `/zoneguard help` 输出的为准

## 末地门不传送 (`/playerNoEndPortalTeleport`)

需要规则 `/carpet noEndPortalTeleport true` 开启时可见、可用。

### 语法

- `/playerNoEndPortalTeleport globalMode <true|false>` —— 切换全局模式
- `/playerNoEndPortalTeleport add <player>` —— 把玩家加入名单
- `/playerNoEndPortalTeleport remove <player>` —— 把玩家移出名单
- `/playerNoEndPortalTeleport clear` —— 清空名单
- `/playerNoEndPortalTeleport list` —— 查看名单
- `/playerNoEndPortalTeleport help` —— 显示用法

### 效果

- `globalMode true`：**所有**玩家都不被末地门传送；`globalMode false`：只有**名单内**的玩家不被传送
- 名单与全局模式保存在**内存**里，重启服务器后清空

## 火箭潜影盒补给位置 (`/rocketShulker`)

设置**自己**的火箭潜影盒补给位置（每个玩家各一份，互不影响），配合规则 `/carpet rocketShulker true` 使用。

### 语法

- `/rocketShulker` —— 查看自己当前的设置
- `/rocketShulker offhand` —— 补到自己的副手
- `/rocketShulker mainhand <1-9>` —— 补到自己的主手快捷栏第 1~9 格

### 效果

- 命令对所有人开放（只能改自己的设置），不需要 OP
- 总开关是 Carpet 规则，命令只调整各自的落点

## 轻松放置实体 (`/easyPlaceEntityCount` `/easyPlaceEntityUi`)

配合规则 `/carpet easyPlaceEntity true` 使用。

### 语法

- `/easyPlaceEntityCount` —— 查看自己当前一次放置的数量
- `/easyPlaceEntityCount <1-64>` —— 设置自己一次放置几个实体
- `/easyPlaceEntityUi` —— 查看/切换自己的选择器界面开关
- `/easyPlaceEntityUi <true|false>` —— 直接设置界面开关

### 效果

- 数量只存在**内存**里，重启服务器后恢复默认
- 命令对所有人开放（只能改自己的设置），不需要 OP
