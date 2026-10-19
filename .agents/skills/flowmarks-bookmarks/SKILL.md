---
name: flowmarks-bookmarks
description: Analyze a project codebase for one user-requested execution flow, propose bookmarks for confirmation, then write a FlowMarks JSON candidate into that project's .flowmarks/inbox directory. Use when asked to map, trace, bookmark, or document a code workflow; do not use for ordinary code changes.
---

# FlowMarks 代码链路书签生成

本 Skill 用于把一个用户指定的代码流程整理成 FlowMarks 可导入的书签候选。插件本身不运行 AI；插件只负责读取当前项目的候选文件、展示书签和跳转代码。

## 硬性边界

- 一次只处理一个项目、一个用户指定的工作流。用户明确要求多个工作流时才拆分。
- 只操作当前项目根目录，不读取或写入其他项目的 `.flowmarks` 目录。
- 候选文件只能写入当前项目的 `<项目根目录>/.flowmarks/inbox/`。
- 不修改 `.flowmarks.json`，不修改业务源代码，不自动导入书签。
- 不依赖 BookmarkAI 的运行时或内部实现。
- 不生成 Markdown 流程文档、项目地图或其他额外产物。

## 第一阶段：只讨论，不写文件

先阅读实际代码并追踪调用关系。不要只根据类名或方法名猜测行为。

向用户提出一份简短方案，包含：

- 建议的分组名称。
- 按执行顺序排列的书签条目。
- 每条书签的标题、相对文件路径、行号。
- 该行原文的前后文是否足以证明这是正确的链路节点。
- 如果发现不确定之处，明确说明。

在用户明确回复“确认生成”“生成”或同义确认之前，不创建、不修改任何文件。用户仅说“继续”“可以讨论”或提出修改意见时，仍然停留在讨论阶段。

## 第二阶段：确认后生成

用户确认后：

1. 重新核对每个文件路径、行号和 `expectedLineText`，以当前代码为准。
2. 文件路径必须是当前项目根目录下的相对路径。
3. 禁止绝对路径、`..`、项目外路径和生成目录中的临时文件。
4. 行号从 1 开始，`expectedLineText` 必须与目标行原文一致，只去掉行首行尾空白。
5. 按执行顺序组织条目；同一工作流使用一个分组。
6. 生成一个标准 FlowMarks JSON 候选文件。
7. 保存到当前项目的 `.flowmarks/inbox/<workflow-slug>.json`，目录不存在时创建。
8. 不覆盖已有候选文件；如文件名冲突，使用新的带序号文件名，并在回复中说明。
9. 写入后重新读取该 JSON，校验语法、字段、路径边界和引用行。
10. 在回复中给出候选文件的绝对路径、分组名、条目数量和简短链路摘要。

不要声称已经导入。导入由用户在 FlowMarks 工具窗口中完成。

## 输出格式

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

插件也兼容 BookmarkAI 的 `targetGroupName` + `items` 候选格式，但新文件一律使用上面的标准格式。

## 项目隔离

- 每个项目只拥有自己的 `.flowmarks.json`、`.flowmarks/inbox/` 和 `.flowmarks/archive/`。
- 不同项目即使分组同名，也不能合并或复用书签。
- 书签路径始终相对于所属项目根目录解析。
- 如果用户要求分析另一个项目，必须先确认当前工作目录已经切换到该项目，不能把两个项目的书签写入同一个目录。

## 质量要求

- 只保留帮助读者理解该流程的关键节点。
- 标题使用简洁中文，避免每条都重复文件名。
- 优先选择方法声明、关键分支、重要调用、状态变更和返回点。
- 如果同一代码行出现多次或行为无法从代码证明，继续读取上下文或明确说明不确定，不要猜行号。
- 生成前检查 JSON 能被标准 JSON 解析器读取。
