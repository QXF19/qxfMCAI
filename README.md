# qxfMCAI v5

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-DFA86A)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Version](https://img.shields.io/badge/version-5.0.0-ff77aa)](https://github.com/QXF19/qxfMCAI/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

面向 **Minecraft Java 1.20.1 / Forge** 的全中文智能生存伙伴模组。伙伴现名为 **龙龙（ロンロン）**：他会记住经历、形成想法、安排任务，并在世界里真正移动、采集、使用工具、战斗和建造。

> 模组模拟“独立人格与长期记忆”，并不宣称模型拥有真实意识。v5 按项目要求为龙龙固定提供 OP4 命令源，仅建议在私人且已备份的世界使用。

## v5 核心功能

- 四页中文控制菜单：伙伴、任务、外观、API/权限
- OpenAI、DeepSeek、自定义 OpenAI 兼容接口；默认 DeepSeek 模型 `deepseek-v4-pro`
- 结构化 JSON 计划，经服务端校验后进入实际任务队列
- 27 格独立物资背包与18格隐藏装备仓，物品、等级、记忆、想法和 YSM 选择随世界保存
- 龙龙拥有自己的等级、经验、已完成任务数、长期目标和近期记忆
- 会装备剑、斧、弓和镐；弓消耗箭，武器与工具正常损耗耐久
- 向下搜索矿脉，无法直接寻路时会开凿双格高通道逐步深入地下
- 使用真实工具与方块掉落规则；没有镐子或材料时会停止并说明原因
- 使用背包里的真实方块建造庇护所、小屋、桥梁和照明
- 头顶聊天气泡、当前动作标签、日系颜文字表情、龙耳和动态尾巴
- PNG 外观热切换，以及必需的 YSM 2.6.5 附属 Mod 与独立模型包目录
- 下界合金工具、武器和箭存放在龙龙的18格隐藏装备仓，不占玩家可见的27格物资背包
- 任务意图先由服务端立即执行，API 只补充聊天和复杂规划；网络失败也不会吞掉基础任务
- 默认自主巡逻而非强制跟随；会形成习惯、主动提出建议、完成后汇报
- 可寻找地下天然洞穴和矿脉，发现后发送“允许传送”按钮
- 自主建筑集中排列在基地建筑区，不会夜晚走到哪里就把庇护所建到哪里
- 保留可切换无敌模式，并固定使用 OP4 命令源执行获准任务

## 27 种动作

`follow`、`stay`、`guard`、`gather`、`mine`、`come`、`explore`、`patrol`、`hunt`、`chop`、`harvest`、`plant`、`farm`、`fish`、`build_shelter`、`build_house`、`build_bridge`、`place_torch`、`eat`、`sleep`、`deposit`、`equip_weapon`、`equip_pickaxe`、`craft`、`command`、`emote`、`stop`。

动作可以由 AI 按玩家自然语言要求组合，也能从菜单或 `/mcai task <动作> <数量>` 手动测试。

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 安装必需附属模组 **Yes Steve Model 2.6.5 Forge 1.20.1**。
3. 从 [Releases](https://github.com/QXF19/qxfMCAI/releases) 下载 `qxfmcai-5.0.0.jar`。
4. 把两个 jar 同时放入客户端和服务端的 `mods` 文件夹。
5. 进入世界后按 **M** 打开菜单，使用 `/mcai summon` 召唤龙龙。

## 对话和真正执行任务

在聊天中输入：

```text
@龙龙 带上镐子向下找铁矿，回来后把矿物放进附近箱子
@龙龙 用背包里的木板在这里建一间房子
@龙龙 装备武器，帮我清理基地周围的怪物
```

龙龙会回复、显示表情和当前动作，并把计划拆成任务。完成一个任务后才会记录经验；背包没有工具、箭矢或建材时不会假装成功。

## 背包、武器和工具

- Shift + 右键龙龙，或点击菜单“打开27格背包”。
- 无需玩家提供工具：最高级工具和武器位于隐藏装备仓；首次只发放一次箭矢和基础物资，不会无限复制材料。
- 龙龙会按工具等级与剩余耐久选择装备。
- 支持的建材包括木板、原木、圆石、石头、泥土和砖块。
- “整理到附近箱子”会把背包物资放入五格内的容器。

## 挖矿与建造

- 挖矿会优先搜索当前位置以下、配置深度范围内的 Forge `ores` 标签方块。
- 无路可走时，龙龙会朝目标开凿两格高的下降通道。
- 龙龙会切换正确镐子并真实消耗耐久；v5 的最高权限任务不再受 `mobGriefing` 阻断。
- 庇护所、小屋和桥梁按蓝图逐块放置，消耗背包里的真实材料。
- 管理员可在 `qxfmcai-server.toml` 关闭挖矿、建造或自主行为。

## 龙龙专属 YSM 附属资源与外观

v5 内置用户提供的 `001.ysm`（白龙）原始加密包，并固定设为龙龙默认附属资源。启动时会原样安装到 YSM 的 `custom/001.ysm`，同时备份在：

```text
config/qxfmcai/ysm_models/
```

该文件的 YSGP 加密内容由 YSM 2.6.5 运行时读取，YSM 是 v5 的必需附属模组。qxfMCAI 不选择、不替换也不修改玩家皮肤。若 YSM 无法把加密包绑定到自定义实体，龙龙会继续显示内置的完整 3D 白龙头部、角、口鼻、耳、翅膀和动态尾巴；回退皮肤使用透明外层和蓝金鳞纹，不再出现纯白模型。第三方模型许可见 [THIRD_PARTY_ASSETS.md](THIRD_PARTY_ASSETS.md)。

普通 PNG 皮肤放在：

```text
config/qxfmcai/skins/
```

## API 与权限

按 M 切换到“API/权限”页，填写提供商、基础地址、模型与密钥。密钥留空会保留服务端旧值。

- OpenAI：`https://api.openai.com/v1` / `gpt-5.2-chat-latest`
- DeepSeek：`https://api.deepseek.com` / `deepseek-v4-pro`
- 自定义：`http://127.0.0.1:11434/v1` / `qwen2.5:7b`

v5 的最高命令权限固定开启；中文“执行命令 …”会在等待 API 回复前立即提交到 OP4 命令源。请只在私人、已备份的世界使用。

## 常用命令

- `/mcai summon`、`come`、`follow`、`stay`、`guard`
- `/mcai inventory`：打开龙龙背包
- `/mcai mine`、`cave`、`chop`、`farm`、`hunt`、`explore`、`patrol`
- `/mcai permit teleport`：批准前往龙龙发现的位置
- `/mcai command <命令>`：用龙龙的 OP4 命令源执行
- `/mcai build shelter|house|bridge`
- `/mcai equip weapon|pickaxe`
- `/mcai task <动作> <数量>`
- `/mcai ask <内容>`
- `/mcai skin <文件.png>`
- `/mcai ysm <模型ID> <材质ID>`
- `/mcai status`
- `/mcai invincible <true|false>`：仅 OP4

## 构建与发行

需要 JDK 17：

```bash
./gradlew build
```

产物位于 `build/libs/`。推送 `v*` 标签时，GitHub Actions 会自动构建并附加 jar 到发行版。

## 许可证

本项目使用 [MIT License（麻省理工学院许可证）](LICENSE)。
