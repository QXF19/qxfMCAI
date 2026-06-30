# qxfMCAI

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.10%2B-DFA86A)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

一个面向 **Minecraft Java 1.20.1 / Forge** 的全中文 AI 生存伙伴模组。

> 当前版本：2.0.0。AI 只执行白名单动作；无敌模式和 API 管理严格要求 OP 权限等级 4。

## V2.0 功能

- 按 **M** 打开自适应分页菜单，不再遮挡热栏
- 直接在菜单输入提供商、API 基础地址、模型和 API 密钥
- 支持 OpenAI、DeepSeek 和自定义 OpenAI 兼容接口
- API 密钥默认掩码；留空保留服务端原值；密钥绝不回传客户端
- AI 伙伴“小麦”：跟随、等待、警戒、拾取、挖矿、跨维度找回
- 挖矿只识别 Forge 矿石标签，兼容大多数模组矿石
- 通过“@小麦”“小麦，”或 `/mcai ask` 聊天
- 保存多轮对话上下文，并可定期主动关心玩家
- 支持 `config/qxfmcai/skins/` 目录中的玩家皮肤 PNG
- 服务端异步调用模型，不阻塞游戏主线程

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 从 [Releases](https://github.com/QXF19/qxfMCAI/releases) 下载 `qxfmcai-2.0.0.jar`。
3. 把 jar 同时放入服务端和客户端的 `mods` 文件夹。
4. 进入世界，确保自己拥有 OP 4，按 **M** 打开菜单。

## 在菜单配置 API

切换到“API 设置（OP4）”页：

1. 选择 `openai`、`deepseek` 或 `custom`。
2. 填写 API 基础地址和模型名。
3. 输入 API 密钥，点击“保存全部 API 设置”。
4. 密钥输入框留空时保留服务端已保存的密钥。
5. 如需删除配置文件中的旧密钥，开启“清除旧密钥”。

默认值：

- OpenAI：`https://api.openai.com/v1` / `gpt-5.2-chat-latest`
- DeepSeek：`https://api.deepseek.com` / `deepseek-v4-pro`
- 自定义：默认示例为本地 Ollama 兼容地址

也支持环境变量：`OPENAI_API_KEY`、`DEEPSEEK_API_KEY`、`QXF_MCAI_API_KEY`。

> API 密钥会通过 Minecraft 连接发送给服务端并写入服务端配置。请只在可信服务器上输入；环境变量仍是公开服务器的推荐方式。

## 命令

- `/mcai summon`：召唤或找回伙伴
- `/mcai follow`：跟随
- `/mcai stay`：等待
- `/mcai guard`：警戒并保护玩家
- `/mcai gather`：拾取附近掉落物
- `/mcai mine`：搜索并挖掘附近矿石
- `/mcai come`：来到身边
- `/mcai ask <内容>`：直接聊天
- `/mcai skin <文件名.png>`：切换皮肤
- `/mcai status`：查看状态
- `/mcai invincible <true|false>`：无敌模式，仅 OP 4
- `/mcai provider <openai|deepseek|custom>`：切换提供商，仅 OP 4
- `/mcai model <模型名>`：切换模型，仅 OP 4

右键伙伴会依次切换“跟随 → 等待 → 警戒 → 拾取 → 挖矿”。

## 挖矿规则

- 只挖 Forge `ores` 标签中的矿石，不会无差别拆家。
- 必须开启世界规则 `mobGriefing`。
- 搜索半径和挖掘速度可在 `qxfmcai-server.toml` 调整。
- 使用标准方块掉落逻辑，模组矿石只要正确加入 Forge 标签即可兼容。
- 服务器管理员可随时把 `miningEnabled` 设为 `false`。

## 皮肤

把标准 Minecraft 玩家皮肤 PNG 放入：

```text
config/qxfmcai/skins/
```

默认读取 `companion.png`。文件名只允许字母、数字、点、下划线和短横线。

## 安全与隐私

- 模型只能返回 `follow/stay/guard/gather/mine/come` 白名单动作。
- 模型不能执行服务器命令、授予 OP、生成物品或修改权限。
- API 配置保存需要 OP 4；密钥不会从服务端同步回客户端。
- 对话、生命值、饱食度、维度和游戏内坐标会发送给所选模型服务。
- 请不要提交包含密钥的 `qxfmcai-server.toml`。

## 开发构建

需要 JDK 17：

```bash
./gradlew build
```

产物位于 `build/libs/`。GitHub 标签 `v*` 会自动创建带 jar 的发行版。

## 许可证

本项目使用 [MIT License（麻省理工学院许可证）](LICENSE)。
