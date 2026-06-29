# qxfMCAI

一个面向 **Minecraft Java 1.20.1 / Forge** 的全中文 AI 生存伙伴模组。

> 当前版本：0.1.0。AI 只执行白名单动作；无敌模式和管理配置严格要求 OP 权限等级 4。

## 功能

- 支持 OpenAI、DeepSeek 和自定义 OpenAI 兼容接口
- AI 伙伴“小麦”：跟随、等待、警戒战斗、拾取附近掉落物、跨维度找回
- 通过聊天呼叫：以“@小麦”“小麦，”或“小麦 ”开头
- 按 **M** 打开独立中文控制菜单
- 保存多轮对话上下文，并按间隔主动关心玩家
- 皮肤目录：`config/qxfmcai/skins/`，支持游戏内切换 PNG
- 无敌模式与模型管理只允许 OP 4，模型无法直接执行服务器命令
- 服务端异步请求 API，不阻塞游戏主线程
- API 密钥优先从环境变量读取，不会同步给普通客户端

## 安装

1. 安装 Minecraft 1.20.1 与 Forge 47.4.10 或更高版本。
2. 从 [Releases](https://github.com/QXF19/qxfMCAI/releases) 下载 jar。
3. 把 jar 同时放入服务端和客户端的 `mods` 文件夹。
4. 启动一次后编辑服务端的 `config/qxfmcai-server.toml`。

## API 配置

推荐使用环境变量，避免把密钥写进整合包：

- OpenAI：`OPENAI_API_KEY`
- DeepSeek：`DEEPSEEK_API_KEY`
- 自定义兼容接口：`QXF_MCAI_API_KEY`

也可在服务端配置文件中填写。**请勿把包含密钥的配置文件提交到 GitHub。**

默认地址与模型：

- OpenAI：`https://api.openai.com/v1` / `gpt-5.5`
- DeepSeek：`https://api.deepseek.com` / `deepseek-v4-flash`
- 自定义：默认演示为本地 Ollama 兼容地址，可自行修改

## 使用

- `/mcai summon`：召唤或找回伙伴
- `/mcai follow`：跟随
- `/mcai stay`：等待
- `/mcai guard`：警戒并保护玩家
- `/mcai gather`：拾取附近掉落物
- `/mcai come`：来到身边
- `/mcai ask <内容>`：直接聊天
- `/mcai skin <文件名.png>`：切换皮肤
- `/mcai status`：查看状态
- `/mcai invincible <true|false>`：无敌模式，仅 OP 4
- `/mcai provider <openai|deepseek|custom>`：切换提供商，仅 OP 4
- `/mcai model <模型名>`：切换模型，仅 OP 4

右键伙伴也可依次切换“跟随 → 等待 → 警戒 → 拾取”。

## 皮肤

把标准 Minecraft 玩家皮肤 PNG 放入：

```text
config/qxfmcai/skins/
```

默认读取 `companion.png`。文件名只允许字母、数字、点、下划线和短横线，以防止目录穿越。

## 安全、隐私与兼容性

- 不授予 AI 任意命令执行能力，模型返回动作会经过白名单验证。
- 无敌、提供商和模型管理命令要求 OP 4。
- 网络请求有超时、并发锁和错误处理。
- 不依赖其他模组；服务端和客户端都需要安装。
- 对话内容、生命值、饱食度、维度和游戏内坐标会发送给所选模型服务；不会主动发送真实身份信息。
- 与大型整合包共存时，建议先在测试世界验证实体 AI、渲染与按键冲突。

## 开发构建

需要 JDK 17 与 Gradle 8.8：

```bash
gradle build
```

产物位于 `build/libs/`。每次推送和发行版发布都会由 GitHub Actions 自动构建。

Forge 版本依据官方 1.20.1 下载页，API 请求兼容 OpenAI 与 DeepSeek 的 `/chat/completions`。

## 许可证

MIT

