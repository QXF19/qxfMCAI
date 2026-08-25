# qxfMCAI v11

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-DFA86A)](https://files.minecraftforge.net/)
[![Version](https://img.shields.io/badge/version-11.1.0-72e4ff)](https://github.com/QXF19/qxfMCAI/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

面向 Minecraft Java 1.20.1 / Forge 的全中文 AI 毛毛龙生存伙伴。龙龙始终称绑定玩家为“主人”，能理解现场、产生喜怒哀乐、聊天互动、跟随与骑乘，并把 AI 规划落实为真实移动、挖矿、建造、耕作、战斗和工具使用。

## v11 重点

- 修复跟随和骑乘：召唤/召回默认恢复跟随，主人可直接操纵龙龙移动，潜行下马
- 新增显示/隐藏和“一键无敌/普通生存”控制；隐藏不会停止思考或已接受任务
- 新增可放在任意方块上的四合一实体棋桌；五子棋、中国象棋、13×13 围棋和四人麻将均在独立界面操作，不再刷聊天框
- 麻将使用136张牌，支持吃、碰、明杠、暗杠、胡、自摸、过、七对与十三幺；主人对战龙龙和两名AI
- 喜怒哀乐会响应主人说的话、主人攻击、骑乘、隐藏、棋盘放拆及龙龙储物空间的取放，并进入长期记忆
- AI 请求会读取生物群系、昼夜、天气、光照、脚下与视线方块、附近敌怪/动物/掉落物，用当前场景组织对话
- 无需 `/mcai ask`：聊天输入 `U：你好` 即可，玩家显示为 U，龙龙回应显示为 `AI·龙龙：`
- DeepSeek 设置提供官方 `deepseek-v4-pro` 与 `deepseek-v4-flash` 快速切换
- 动作加入跨姿态平滑过渡；继续使用标准 64×64 二维皮肤与原版玩家骨骼，保持轻量
- 控制台提供“传送到 qxf1975”：其他玩家点击后将自己传送至在线玩家 qxf1975，不移动目标玩家

> “意识、想法和长期记忆”是游戏角色模拟。家庭玩法不含露骨内容，关系推进需要主人明确同意。

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 下载 `qxfmcai-11.1.0.jar`，放入客户端与服务端的 `mods` 文件夹。
3. 不需要 Yes Steve Model 或 Touhou Little Maid；v11 不会改动主人的玩家皮肤。
4. 进入世界后按 `M` 打开紧凑控制台，配置 OpenAI、DeepSeek 或兼容 OpenAI Chat Completions 的接口。

## 使用

控制台最上方可直接输入自然语言，也可输入以 `/` 开头的命令。普通聊天还可直接在聊天框加 `U：` 前缀。示例：

```text
带上镐子向下寻找铁矿，找到矿洞后告诉主人
设计一座集中在基地旁的小仓库并真正建起来
U：龙龙，你觉得这里适合安家吗？
/mcai play dance
/mcai board
```

常用命令：

- `/mcai summon`：召唤龙龙
- `/mcai status`：查看任务、好感度、存储和棋类战绩
- `/mcai inventory`：打开 27 格物资背包
- `/mcai ride`：骑乘并直接操纵龙龙
- `/mcai hide [true|false]`：隐藏或显示龙龙
- `/mcai board`：领取可放置的五子棋/象棋/围棋/麻将四合一棋桌
- `/mcai play [动作]`：在主人面前表演；支持 `wave/dance/cheer/bow/shy/stretch/nod/look/spin/hop`
- `/mcai invincible [true|false]`：OP4 一键切换无敌与普通生存
- `/mcai special teleport_to_qxf1975`：操作者本人传送到在线的 qxf1975；无需 OP，不会移动目标玩家
- `/mcai command <命令>`：执行主人明确授权的 OP4 命令任务

## AI 与安全

API 密钥保存在 Forge 服务端配置中，不通过聊天回传。也可使用环境变量 `OPENAI_API_KEY`、`DEEPSEEK_API_KEY` 或 `QXF_MCAI_API_KEY`。

网络失败时，本地规划器会保底执行已明确交付的生存任务。五分钟聊天通道强制为空动作，不会自动建造、挖矿、战斗或执行命令。世界修改仍受边界、方块实体、流体和任务队列限制；OP4 能力只应在可信且已备份的世界使用。

## 构建

需要 JDK 17：

```bash
./gradlew clean build --no-daemon
```

输出：`build/libs/qxfmcai-11.1.0.jar`。
