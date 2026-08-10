# qxfMCAI v9

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-DFA86A)](https://files.minecraftforge.net/)
[![Version](https://img.shields.io/badge/version-9.0.0-72e4ff)](https://github.com/QXF19/qxfMCAI/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

面向 Minecraft Java 1.20.1 / Forge 的全中文 AI 生存伙伴。龙龙会让 API 参与任务规划、过程复盘和自主决策，再由本地实体真正移动、挖矿、建造、耕作、战斗和使用工具；网络异常时也有安全执行保底，不会只说“知道了”。

## v9 重点

- API 贯穿任务规划、执行中复盘、完成/失败总结及空闲自主决策
- 明确任务可自动召唤龙龙，模型只回复确认或请求超时也不会丢任务
- 更宽的中文任务理解，支持同义表达、中文数字和连续动作
- 修复地下探索、找洞、巡逻和采集的固定超时；寻路失败会重新规划或安全结束
- 挖矿会选择镐并消耗耐久，真实向下开凿安全通道；战斗会使用剑、弓、斧并消耗箭矢
- API 提出的仓库、农舍、瞭望塔、房屋或桥梁想法会映射成集中、轻量且有限制的实际蓝图
- 每 5 分钟可靠主动聊天；该通道强制为空动作，不会擅自建造、挖矿、战斗或执行命令
- 全新深色高级控制台，命令输入、任务卡片和智能设置不再挤在传统多页选择菜单里
- 核心人格、任务思维、自主决策、五分钟聊天四套提示词均有独立修改入口
- 重做原生 3D 白龙的身体层级、头部跟随、步态、翅膀、尾巴、工作、攻击和坐下动画
- 保留 27 格物资背包、18 格隐藏工具仓、好感度、饰品、骑乘、五子棋、家庭与小龙宝宝
- 保留无敌模式和明确命令任务的 OP4 命令源

> “意识、想法和长期记忆”是游戏角色模拟。家庭玩法不包含露骨内容，任何关系推进都需要玩家明确同意。

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 下载 `qxfmcai-9.0.0.jar`，放入客户端与服务端的 `mods` 文件夹。
3. 不需要 Yes Steve Model 或 Touhou Little Maid；龙龙是独立原生 3D 实体，不修改玩家皮肤。
4. 进入世界后按 `M` 打开控制台，配置 OpenAI、DeepSeek 或兼容接口，再直接输入自然语言任务。

## 使用示例

```text
带上镐子向下寻找铁矿，找到矿洞后告诉我
设计一座集中在基地旁的小仓库并真正建起来
装备武器，清理基地附近的怪物
收获成熟作物，再把小麦放进附近箱子
```

常用命令：

- `/mcai summon`：召唤龙龙
- `/mcai status`：查看活动、任务进度、好感度和存储状态
- `/mcai inventory`：打开 27 格物资背包
- `/mcai accessory`、`/mcai ride`、`/mcai gomoku`、`/mcai family`
- `/mcai command <命令>`：执行明确授权的 OP4 命令任务

## AI 设置与安全

API 密钥保存在 Forge 服务端配置中，不会通过聊天显示。也可使用环境变量 `OPENAI_API_KEY`、`DEEPSEEK_API_KEY` 或 `QXF_MCAI_API_KEY`。自定义接口需兼容 OpenAI Chat Completions 格式。

龙龙的世界修改仍受世界边界、方块实体、流体、建造/挖矿开关和任务队列上限保护。OP4 命令能力风险很高，只应在可信单人世界或受控服务器中启用。

## 构建

需要 JDK 17：

```bash
./gradlew build
```

输出位于 `build/libs/qxfmcai-9.0.0.jar`。
