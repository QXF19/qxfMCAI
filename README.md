# qxfMCAI v10

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-DFA86A)](https://files.minecraftforge.net/)
[![Version](https://img.shields.io/badge/version-10.0.0-72e4ff)](https://github.com/QXF19/qxfMCAI/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

面向 Minecraft Java 1.20.1 / Forge 的全中文 AI 毛毛龙生存伙伴。龙龙始终称绑定玩家为“主人”，会聊天、在主人面前做动作、玩五子棋和中国象棋，也会把 AI 规划落实为真实移动、挖矿、建造、耕作、战斗和工具使用。

## v10 重点

- 删除旧 YSM、加密模型和自定义复杂 3D 骨骼，直接使用用户提供的标准 64×64 二维皮肤
- 复用原版玩家骨骼，动作只驱动原版肢体，降低模型维护量、渲染开销和动画断裂风险
- 龙龙空闲时会走到主人视线前挥手、跳舞、鞠躬、害羞、伸展、点头、观察、转圈或跳跃
- 新增中国象棋；保留 9×9 五子棋，棋盘、胜负和进度均可保存
- AI 固定称呼“主人”；任务提示词只拼接一次，减少重复 token 和无效请求
- 五分钟主动聊天只发送一次：API 成功时不追加本地重复消息，失败时才使用零动作保底
- 设置与控制合并为一个紧凑界面：模型、聊天、自主思考、密钥和四类提示词集中管理
- 保留真实任务、27 格物资背包、18 格隐藏装备仓、好感度、饰品、骑乘、家庭系统、无敌与明确任务的 OP4 命令源

> “意识、想法和长期记忆”是游戏角色模拟。家庭玩法不含露骨内容，关系推进需要主人明确同意。

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 下载 `qxfmcai-10.0.0.jar`，放入客户端与服务端的 `mods` 文件夹。
3. 不需要 Yes Steve Model 或 Touhou Little Maid；v10 不会改动主人的玩家皮肤。
4. 进入世界后按 `M` 打开紧凑控制台，配置 OpenAI、DeepSeek 或兼容 OpenAI Chat Completions 的接口。

## 使用

控制台最上方可直接输入自然语言，也可输入以 `/` 开头的命令。示例：

```text
带上镐子向下寻找铁矿，找到矿洞后告诉主人
设计一座集中在基地旁的小仓库并真正建起来
/mcai play dance
/mcai gomoku move 4 4
/mcai chess move 0 6 0 5
```

常用命令：

- `/mcai summon`：召唤龙龙
- `/mcai status`：查看任务、好感度、存储和棋类战绩
- `/mcai inventory`：打开 27 格物资背包
- `/mcai play [动作]`：在主人面前表演；支持 `wave/dance/cheer/bow/shy/stretch/nod/look/spin/hop`
- `/mcai gomoku start|board|move x y`：五子棋
- `/mcai chess start|board|move 起点x 起点y 终点x 终点y`：中国象棋，主人执红
- `/mcai command <命令>`：执行主人明确授权的 OP4 命令任务

## AI 与安全

API 密钥保存在 Forge 服务端配置中，不通过聊天回传。也可使用环境变量 `OPENAI_API_KEY`、`DEEPSEEK_API_KEY` 或 `QXF_MCAI_API_KEY`。

网络失败时，本地规划器会保底执行已明确交付的生存任务。五分钟聊天通道强制为空动作，不会自动建造、挖矿、战斗或执行命令。世界修改仍受边界、方块实体、流体和任务队列限制；OP4 能力只应在可信且已备份的世界使用。

## 构建

需要 JDK 17：

```bash
./gradlew clean build --no-daemon
```

输出：`build/libs/qxfmcai-10.0.0.jar`。
