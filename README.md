# qxfMCAI v3

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-DFA86A)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Version](https://img.shields.io/badge/version-3.0.0-ff77aa)](https://github.com/QXF19/qxfMCAI/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

面向 **Minecraft Java 1.20.1 / Forge** 的全中文智能生存伙伴模组。伙伴现名为 **龙龙（ロンロン）**：他会记住经历、形成想法、安排任务，并在世界里真正移动、采集、使用工具、战斗和建造。

> 模组模拟“独立人格与长期记忆”，并不宣称模型拥有真实意识。全命令能力必须由 OP4 在菜单中明确授权。

## v3 核心功能

- 四页中文控制菜单：伙伴、任务、外观、API/权限
- OpenAI、DeepSeek、自定义 OpenAI 兼容接口；默认 DeepSeek 模型 `deepseek-v4-pro`
- 结构化 JSON 计划，经服务端校验后进入实际任务队列
- 27 格独立背包，物品、装备、等级、记忆、想法和 YSM 选择随世界保存
- 龙龙拥有自己的等级、经验、已完成任务数、长期目标和近期记忆
- 会装备剑、斧、弓和镐；弓消耗箭，武器与工具正常损耗耐久
- 向下搜索矿脉，无法直接寻路时会开凿双格高通道逐步深入地下
- 使用真实工具与方块掉落规则；没有镐子或材料时会停止并说明原因
- 使用背包里的真实方块建造庇护所、小屋、桥梁和照明
- 头顶聊天气泡、当前动作标签、日系颜文字表情、龙耳和动态尾巴
- PNG 皮肤热切换，以及 YSM 2.4.1+ 模型/材质选择数据与模型包目录
- OP4 可开启无敌模式和“AI 全命令”危险权限

## 27 种动作

`follow`、`stay`、`guard`、`gather`、`mine`、`come`、`explore`、`patrol`、`hunt`、`chop`、`harvest`、`plant`、`farm`、`fish`、`build_shelter`、`build_house`、`build_bridge`、`place_torch`、`eat`、`sleep`、`deposit`、`equip_weapon`、`equip_pickaxe`、`craft`、`command`、`emote`、`stop`。

动作可以由 AI 按玩家自然语言要求组合，也能从菜单或 `/mcai task <动作> <数量>` 手动测试。

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 从 [Releases](https://github.com/QXF19/qxfMCAI/releases) 下载 `qxfmcai-3.0.0.jar`。
3. 把 jar 同时放入客户端和服务端的 `mods` 文件夹。
4. 进入世界后按 **M** 打开菜单，使用 `/mcai summon` 召唤龙龙。

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
- 把镐子、斧、剑、弓、箭、食物、火把和建筑方块放入他的背包。
- 龙龙会按工具等级与剩余耐久选择装备。
- 支持的建材包括木板、原木、圆石、石头、泥土和砖块。
- “整理到附近箱子”会把背包物资放入五格内的容器。

## 挖矿与建造

- 挖矿会优先搜索当前位置以下、配置深度范围内的 Forge `ores` 标签方块。
- 无路可走时，龙龙会朝目标开凿两格高的下降通道。
- 必须持有能正确挖掘目标方块的镐子，并遵守 `mobGriefing`。
- 庇护所、小屋和桥梁按蓝图逐块放置，消耗背包里的真实材料。
- 管理员可在 `qxfmcai-server.toml` 关闭挖矿、建造或自主行为。

## YSM 与外观

v3 会检测可选模组 **Yes Steve Model 2.4.1+**，保存龙龙的 `model_id` 与 `texture_id`，并创建：

```text
config/qxfmcai/ysm_models/
```

YSM 模型包本身请按 YSM 官方格式放入 `config/yes_steve_model` 并在客户端、服务端同时安装 YSM。YSM 官方公开命令主要面向玩家模型；龙龙作为独立驯服实体时始终保留内置 PNG 玩家模型、龙耳和尾巴安全回退，避免未安装 YSM 或接口变化导致崩溃。

普通 PNG 皮肤放在：

```text
config/qxfmcai/skins/
```

## API 与权限

按 M 切换到“API/权限”页，填写提供商、基础地址、模型与密钥。密钥留空会保留服务端旧值。

- OpenAI：`https://api.openai.com/v1` / `gpt-5.2-chat-latest`
- DeepSeek：`https://api.deepseek.com` / `deepseek-v4-pro`
- 自定义：`http://127.0.0.1:11434/v1` / `qwen2.5:7b`

“全命令”默认关闭。只有 OP4 能保存并开启；开启后，模型产生的 `command` 动作会以该 OP4 玩家身份执行任意 Minecraft/模组命令。请只在私人、已备份的世界使用。

## 常用命令

- `/mcai summon`、`come`、`follow`、`stay`、`guard`
- `/mcai inventory`：打开龙龙背包
- `/mcai mine`、`chop`、`farm`、`hunt`、`explore`、`patrol`
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
