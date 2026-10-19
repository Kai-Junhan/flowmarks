# FlowMarks

[![Build](https://github.com/Kai-Junhan/flowmarks/actions/workflows/build.yml/badge.svg)](https://github.com/Kai-Junhan/flowmarks/actions/workflows/build.yml)

完全本地、免费、开源的 IntelliJ IDEA 代码链路书签插件。

一句话：**agent 负责读代码并生成书签，FlowMarks 负责导入、展示和跳转。**

插件不内置 AI，不依赖 BookmarkAI 运行时，不使用云端接口、登录、会员、许可证或遥测。

## 怎么用

~~~text
agent + flowmarks-bookmarks Skill
              ↓
     .flowmarks/inbox/*.json
              ↓
      FlowMarks 导入候选
              ↓
   .flowmarks.json → 双击跳转代码
~~~

1. 让 agent 使用 `.agents/skills/flowmarks-bookmarks/SKILL.md` 读取项目，先给出书签方案，确认后生成 JSON。
2. 在 IDEA 打开 FlowMarks，点击“刷新”，插件会自动发现当前项目的候选文件。
3. 点击“导入候选”，书签按分组显示；双击书签跳转到代码。

agent 不修改业务代码，也不写正式书签文件；插件只读取候选、保存书签和负责跳转。

## 项目隔离

每个 IDEA 项目只使用自己根目录下的数据：

~~~text
.flowmarks.json          正式书签
.flowmarks/inbox/        agent 生成的候选
.flowmarks/archive/      已处理的候选
~~~

插件只读取当前项目，不会跨项目合并书签。绝对路径和 `..` 越界路径会被拒绝，数据不会写入 `.idea/workspace.xml`。

## 安装

1. 构建或取得 `FlowMarks-0.2.0.zip`。
2. 打开 `Settings > Plugins > 齿轮 > Install Plugin from Disk...`。
3. 选择 ZIP，重启 IDEA。
4. 从 `View > Tool Windows > FlowMarks` 打开工具窗口。

## 书签格式

agent 生成的标准 JSON：

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

示例文件：`examples/sample-flowmarks.json`。插件也兼容 BookmarkAI 的 `targetGroupName` + `items` 格式。

## 构建

需要 Java 21，项目自带 Gradle Wrapper：

~~~powershell
./gradlew.bat clean test buildPlugin
~~~

安装包生成在 `build/distributions/`。插件兼容 IntelliJ 平台构建号 `253.*`，已在 IntelliJ IDEA `2025.3.6.1` 上验证。

## 开发

- `src/main/java/flowmarks/intellij/model`：书签模型、JSON、路径校验、行定位、持久化和候选扫描。
- `src/main/java/flowmarks/intellij/toolwindow`：FlowMarks 工具窗口。
- `src/test/java`：核心模型和项目隔离测试。
- `.agents/skills/flowmarks-bookmarks`：agent 书签生成 Skill。
- `examples`：JSON 示例。

## 限制

- 当前只兼容 IntelliJ 平台构建号 `253.*`。
- 代码阅读和书签总结由外部本地 agent 完成。
- 尚未发布到 JetBrains Marketplace。

## 许可证

Apache License 2.0，详见 `LICENSE`。
