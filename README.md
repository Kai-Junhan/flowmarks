# FlowMarks

[![Build](https://github.com/Kai-Junhan/flowmarks/actions/workflows/build.yml/badge.svg)](https://github.com/Kai-Junhan/flowmarks/actions/workflows/build.yml)

FlowMarks 是一个完全本地、免费、开源的 IntelliJ IDEA 代码链路书签插件。

它不内置 AI，不依赖 BookmarkAI 运行时，不使用云端接口、登录、会员、许可证或遥测。代码理解由本地 agent 完成，FlowMarks 负责读取书签、按分组展示并跳转到代码。

## 工作流程

1. 使用仓库内的 `flowmarks-bookmarks` Skill 分析当前项目的指定代码流程。
2. agent 先给出书签方案，用户确认后才生成 JSON。
3. JSON 写入当前项目的 `.flowmarks/inbox/`。
4. 打开 FlowMarks，工具窗口自动发现候选文件。
5. 点击“导入候选”，书签合并到当前项目的 `.flowmarks.json`。
6. 候选文件移动到当前项目的 `.flowmarks/archive/`。
7. 双击书签跳转到代码；行号变化时按 `expectedLineText` 重新定位。

## 项目隔离

FlowMarks 的所有数据都属于当前 IntelliJ 项目：

~~~text
<项目根目录>/.flowmarks.json          正式书签
<项目根目录>/.flowmarks/inbox/        agent 生成的候选
<项目根目录>/.flowmarks/archive/      已处理的候选
~~~

插件只扫描当前打开项目的目录，不会读取其他项目的书签，也不会跨项目合并同名分组。书签路径必须位于当前项目根目录内，绝对路径和 `..` 越界路径会被拒绝。数据不会写入 `.idea/workspace.xml`。

## 安装

1. 取得 `build/distributions/FlowMarks-0.2.0.zip`。
2. 打开 `Settings > Plugins > 齿轮 > Install Plugin from Disk...`。
3. 选择 ZIP 并重启 IDEA。
4. 通过 `View > Tool Windows > FlowMarks` 打开工具窗口。

## 使用

- “导入候选”读取当前项目 `.flowmarks/inbox/*.json`，预览后确认导入。
- “忽略”把候选文件归档，不导入书签。
- “导入文件”用于手动选择任意 JSON 文件。
- “刷新”重新读取正式书签和候选目录。
- “新增”收藏当前编辑器光标所在行。
- 搜索框支持标题、分组名称和文件路径。

导入前会显示正式书签保存位置。JSON 损坏时只报告错误，不覆盖原文件。保存使用 UTF-8 和临时文件替换。

## Agent Skill

Skill 文件：`.agents/skills/flowmarks-bookmarks/SKILL.md`

Skill 的固定流程是：先讨论方案，用户明确确认后才写入当前项目的 `.flowmarks/inbox/`。它不会修改业务代码，不会写正式书签文件，也不会跨项目输出。

标准候选格式：

~~~json
{
  "version": 1,
  "groups": [
    {
      "name": "登录处理链路",
      "items": [
        {
          "title": "接收登录请求并校验参数",
          "filePath": "src/main/java/example/LoginController.java",
          "line": 42,
          "expectedLineText": "public LoginResult login(LoginRequest request) {"
        }
      ]
    }
  ]
}
~~~

示例文件：`examples/sample-flowmarks.json`。插件同时兼容 BookmarkAI 的 `targetGroupName` + `items` 格式。

## 构建

项目自带 Gradle Wrapper，不要求安装全局 Gradle。Java 21 下运行：

~~~powershell
./gradlew.bat clean test buildPlugin
~~~

启动独立测试 IDE 时，通过参数或环境变量提供本机安装目录，不把路径写入源码：

~~~powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=NUL'
./gradlew.bat -PideaPath="D:\IntelliJ IDEA 2025.2.3" runIde
~~~

当前插件兼容 IntelliJ 平台构建号 `253.*`，已在 IntelliJ IDEA `2025.3.6.1`（构建号 `253.33813.55`）和 Java 21 上验证。

## 测试

单元测试覆盖标准 JSON、BookmarkAI 候选格式、重复导入去重、UTF-8 持久化、相对路径、越界路径拒绝、原始行号跳转、`expectedLineText` 重定位、候选扫描、项目隔离、损坏候选和归档。

## 项目结构

- `src/main/java/flowmarks/intellij/model`：书签模型、JSON 编解码、路径校验、行定位、持久化和候选扫描。
- `src/main/java/flowmarks/intellij/toolwindow`：FlowMarks 工具窗口和用户操作。
- `src/test/java`：核心模型和隔离测试。
- `.agents/skills/flowmarks-bookmarks`：agent 生成书签的 Skill。
- `examples`：书签 JSON 示例。

## 限制

- 当前只兼容 IntelliJ 平台构建号 `253.*`。
- agent 代码阅读和书签总结由外部本地 agent 完成。
- 尚未发布到 JetBrains Marketplace。

## 许可证

Apache License 2.0，详见 `LICENSE`。
