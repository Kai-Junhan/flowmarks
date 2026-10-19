# FlowMarks 中文说明

FlowMarks 是一个纯本地的 IntelliJ IDEA 代码链路书签插件。

用法很简单：

1. agent 使用 `.agents/skills/flowmarks-bookmarks/SKILL.md` 读取项目，生成书签候选。
2. 打开 FlowMarks，点击“刷新”和“导入候选”。
3. 双击书签跳转代码。

每个项目独立保存书签，不会跨项目读取或合并。

安装包：`build/distributions/FlowMarks-0.2.0.zip`。安装路径：`Settings > Plugins > 齿轮 > Install Plugin from Disk...`。

完整说明见 `README.md`。
