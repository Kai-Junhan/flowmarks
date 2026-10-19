# FlowMarks

[![Build](https://github.com/Kai-Junhan/flowmarks/actions/workflows/build.yml/badge.svg)](https://github.com/Kai-Junhan/flowmarks/actions/workflows/build.yml)

一个纯本地、免费、开源的 IntelliJ IDEA 代码链路书签插件。

它不内置 AI。真正的工作方式是：

- **agent 负责读代码**，用仓库里的 Skill 生成书签 JSON。
- **FlowMarks 负责读书签**，在 IDEA 里按分组展示，双击跳转。

所以你要装的只有两样：**Skill** 和 **插件**。

## 安装

### 第 1 步：把 Skill 喂给 agent

Skill 文件在本仓库：

```text
.agents/skills/flowmarks-bookmarks/SKILL.md
```

也可以直接从 Releases 下载打包好的 Skill：

**[FlowMarks-AgentSkill-0.3.0.zip](https://github.com/Kai-Junhan/flowmarks/releases/download/v0.3.0/FlowMarks-AgentSkill-0.3.0.zip)**

Skill 自带 `scripts/validate_candidate.py`，生成候选后会校验文件路径、行号和原文是否一致。

> **IDEA 只能安装 `FlowMarks-0.3.0.zip`。** `FlowMarks-AgentSkill-*.zip` 是给 agent 的 Skill，不能通过 `Install Plugin from Disk` 安装。

三种方式任选：

**方式 A：agent 直接读本项目**

如果你的 agent 能读当前项目（比如 Codex），打开仓库后它会自动发现 `.agents/skills` 下的 `flowmarks-bookmarks`，无需复制。

**方式 B：复制到 agent 的 skills 目录**

| Runtime | 复制到 |
|---|---|
| Codex | `~/.codex/skills/flowmarks-bookmarks/` |
| Cursor | `~/.cursor/skills/flowmarks-bookmarks/` |
| Claude Code | `~/.claude/skills/flowmarks-bookmarks/` |
| 其他 | 对应 runtime 的 `skills/` 目录 |

Windows 的 `~` 指 `C:\Users\你的用户名`。

先下载仓库：

```bash
git clone https://github.com/Kai-Junhan/flowmarks.git
```

然后把 `.agents/skills/flowmarks-bookmarks/` 整个目录复制到上表对应位置。

**方式 C：直接粘贴**

把 `SKILL.md` 的完整内容粘贴进对话，它本质就是一段 Markdown 指令。

### 第 2 步：装好插件

先拿到安装包 `FlowMarks-0.3.0.zip`。

**方式 A：自己构建**

```bash
git clone https://github.com/Kai-Junhan/flowmarks.git
cd flowmarks
./gradlew clean test buildPlugin
```

Windows 使用 `./gradlew.bat`。构建产物在 `build/distributions/FlowMarks-0.3.0.zip`。

**方式 B：从 Releases 下载**

到 [Releases](https://github.com/Kai-Junhan/flowmarks/releases) 页面下载最新 `FlowMarks-*.zip`。

然后在 IDEA 安装：

```text
Settings > Plugins > 齿轮图标 > Install Plugin from Disk...
```

选择 ZIP，重启 IDEA，从 `View > Tool Windows > FlowMarks` 打开工具窗口。

## 使用

1. 告诉 agent 整理某条链路，例如“帮我整理登录链路”。
2. agent 先给出书签方案，你确认后，它把 JSON 写入当前项目的 `.flowmarks/inbox/`。
3. 打开 FlowMarks，点“刷新”，再点“导入候选”。
4. 双击书签跳转代码；行号变化时会按 `expectedLineText` 自动重新定位。

书签支持 `step`/`level`：主步骤显示为 `1、2、3`，子步骤显示为 `2.1、2.2` 并自动缩进；树中每行分两行显示标题和文件名:行号。

agent 不修改业务代码；插件只读取候选、保存书签和跳转。

## 数据放在哪

每个 IDEA 项目独立保存，不跨项目合并：

```text
.flowmarks.json          正式书签
.flowmarks/inbox/        agent 生成的候选
.flowmarks/archive/      已处理的候选
```

## 仓库结构

```text
flowmarks/
├── .agents/skills/flowmarks-bookmarks/   # 喂给 agent 的 Skill
│   └── SKILL.md
├── src/main/java/flowmarks/intellij/     # 插件源码
├── src/main/resources/META-INF/plugin.xml # 插件注册
├── src/test/java/                        # 单元测试
├── examples/sample-flowmarks.json        # 书签 JSON 示例
├── build.gradle.kts                      # 构建配置
└── .github/workflows/build.yml           # CI 构建检查
```

## 开发

需要 Java 21，项目自带 Gradle Wrapper，兼容 IntelliJ 平台构建号 `253.*`。

## 许可证

Apache License 2.0，详见 `LICENSE`。
