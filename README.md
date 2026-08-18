# BlockWiz（方块巫师）

> 🧙 Minecraft 1.21.11（Fabric）本地 AI 建造与拆除助手

BlockWiz 是运行在 Minecraft Fabric 上的 AI 助手模组：玩家在游戏内用自然语言描述需求（"在这里建一座现代风格的房子""把这座房子拆掉"），AI 理解意图、扫描环境、制定方案，由模组在玩家自己的世界里安全地执行。AI 请求直接从玩家电脑发送到玩家自行配置的 API 地址，**不依赖任何项目方远程服务**。

## ✨ 特性

- **🗣️ 自然语言驱动** — 聊天框直接描述需求，由 /blockwiz 命令发起
- **🔌 本地 API 配置** — Mod Menu + Cloth Config 图形化配置，支持 OpenAI 兼容接口
- **⚡ 快速预设** — OpenAI / Claude（兼容网关）/ 通义千问 / 智谱 / Ollama（本地）一键套用
- **🌐 双语 i18n** — 界面与聊天文案支持中文（zh_cn）/ 英文（en_us）切换
- **🛡️ 安全可控** — Base URL 白名单（https 或本机 http）、API Key 界面掩码、日志脱敏、配置原子写入
- **🏠 单人/局域网** — V1.0 支持单人游戏与自建局域网（集成服务器执行），无远程后端

## 🏛️ 架构

V1.0 采用「客户端交互 + 集成服务器执行」的本地架构，无 Python 服务、无 WebSocket、无远程后端：

- 玩家聊天 / 命令 → Fabric 模组（客户端：交互、配置、扫描、AI 请求、安全确认）→ 集成服务器（单人/局域网主机，同进程：执行任务、修改方块）→ 玩家自配 API（OpenAI 兼容 /chat/completions，https 或本机 http）

> **V1.0 边界**：仅支持单人游戏与自建局域网（集成服务器）；专用服务器 / 远程多人服务器支持列为 V1.1。

## 📦 当前功能（Sprint 1 已交付）

| 模块 | 说明 |
| --- | --- |
| 配置页（Mod Menu） | Base URL / Chat Completions 路径 / API Key（掩码）/ 鉴权头 / 前缀 / 模型 / 温度 / 超时 / 最大输出 / 自定义请求头；语言、每 tick 方块数、扫描半径、确认超时、重试次数、玩家位置保护 |
| 快速预设 | 五类服务商预设，保存时应用（可覆盖） |
| 连接测试 | /blockwiz test 或配置页保存后测试，错误分类可读（鉴权/限流/超时/网络/格式） |
| i18n | zh_cn / en_us 双语，配置热更新 |
| 命令框架 | /blockwiz 命令树已注册（完整子命令随 Sprint 2 起交付） |

## 🚀 快速开始

### 环境要求

- Minecraft **1.21.11**（Fabric）
- Java **21+**
- Fabric Loader 0.19.3+
- Fabric API（开发构建时自动引入）

### 编译与安装

```bash
./gradlew build
```

产物位于 build/libs/blockwiz-1.0.0.jar，放入 Minecraft mods 文件夹。首次启动后：

1. 打开 Mod Menu → **BlockWiz 配置**，填写 Base URL / 模型（可选用预设）
2. 回到游戏输入 /blockwiz test 验证连接
3. 输入 /blockwiz + 建造描述发起任务（建造功能随后续 Sprint 交付）

### 本地测试假服务

仓库提供 OpenAI 兼容的假服务脚本，用于无外网/无真实 Key 时验证连接：

```bash
python scripts/mock-openai-server.py              # 默认 127.0.0.1:18080
python scripts/mock-openai-server.py --port 11434 # 指定端口（如验证 Ollama 预设）
```

配置页 Base URL 填 http://127.0.0.1:18080/v1、模型随意，然后 /blockwiz test 即可。

## 🗺️ V1.0 路线图

| Sprint | 内容 | 状态 |
| --- | --- | --- |
| Sprint 1 | 本地 API 配置、连接测试、i18n、命令框架 | ✅ 已完成（v1.0.0 构建） |
| Sprint 2 | 命令体系（confirm/cancel/pause/resume）、任务状态机、环境扫描、手动范围、64³ 校验 | 🚧 待开始 |
| Sprint 3 | AI 建造：意图识别、建筑风格、方案规划 | 📋 待开始 |
| Sprint 4 | 建造执行：分阶段执行、复检与修正、进度反馈 | 📋 待开始 |
| Sprint 5 | 安全拆除、范围确认、30 秒强确认、玩家位置保护 | 📋 待开始 |
| Sprint 6 | 容错重试、任务持久化与恢复、任务记录、验收 | 📋 待开始 |

**V1.0 核心约束**：单次操作任一边长 ≤64 且体积 ≤262144；破坏性操作必须经玩家确认；玩家所处方块及上方一格默认受保护。

## 📄 许可

本项目基于 [CC0 1.0 Universal](LICENSE) 开源，欢迎学习参考与二次开发。
