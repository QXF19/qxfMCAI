# qxfMCAI v7

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-DFA86A)](https://files.minecraftforge.net/)
[![Version](https://img.shields.io/badge/version-7.0.0-ff77aa)](https://github.com/QXF19/qxfMCAI/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

面向 Minecraft Java 1.20.1 / Forge 的全中文智能生存伙伴。龙龙会在世界中真正移动、挖矿、建造、耕作、战斗、使用工具并消耗耐久，而不是只回复聊天。

## v7 重点

- 单实体原生 3D 白龙：不再使用车万女仆代理实体，不再高频传送同步
- 原生头部、口鼻、龙角、兽耳、翅膀、双段尾巴和蓝金日系 Furry 饰纹
- 平滑待机、走路、挖掘、砍伐、战斗、坐下、持物与幼体动画
- 限制大范围扫描预算，降低寻路重算频率
- 27 格物资背包、18 格隐藏工具仓、四格饰品
- 好感度、骑乘、9×9 五子棋与持久胜负记录
- 家庭系统采用明确同意并可撤回；达到好感度80且共同完成10项任务后，龙龙会主动提出组建家庭
- 同意后可迎接会成长、跟随玩家的小龙宝宝
- OpenAI、DeepSeek 和自定义 OpenAI 兼容 API 直接在中文菜单配置
- 保留无敌模式和 OP4 命令执行能力

> “独立人格、意识与长期记忆”属于游戏角色模拟。家庭玩法不包含露骨内容，任何后续行为都需要玩家明确同意。

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 下载 `qxfmcai-7.0.0.jar` 放入客户端与服务端的 `mods` 文件夹。
3. 不再需要 Yes Steve Model 或 Touhou Little Maid。
4. 进入世界后按 M 打开菜单，使用 `/mcai summon` 召唤龙龙。

## 常用操作

- `/mcai mine`、`cave`、`farm`、`chop`、`hunt`、`build house`
- `/mcai inventory`：打开27格物资背包
- `/mcai accessory`：把主手一件物品作为饰品赠送，最多四件
- `/mcai ride`：骑乘龙龙，潜行键下马
- `/mcai gomoku start`，随后 `/mcai gomoku x y`（坐标0到8）
- `/mcai family accept|decline|child`
- `/mcai status`：查看任务、好感度、饰品、棋局与家庭状态
- `/mcai command <命令>`：使用龙龙的 OP4 命令源

聊天示例：

```text
@龙龙 带上镐子向下寻找铁矿，找到矿洞后告诉我
@龙龙 用背包材料在基地集中区域建一间小屋
@龙龙 装备武器，清理基地附近的怪物
```

## 构建

需要 JDK 17：

```bash
./gradlew build
```

输出位于 `build/libs/qxfmcai-7.0.0.jar`。
