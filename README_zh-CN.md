# FlowMarks 中文说明

FlowMarks 是完全本地运行的 IntelliJ IDEA 代码链路书签插件。

推荐流程：使用 `.agents/skills/flowmarks-bookmarks/SKILL.md` 让本地 agent 先讨论书签方案，确认后写入当前项目的 `.flowmarks/inbox/`；在 FlowMarks 中点击“导入候选”，书签按分组展示，双击跳转代码。

每个项目独立使用自己的 `.flowmarks.json`、`.flowmarks/inbox/` 和 `.flowmarks/archive/`，不会跨项目读取或合并。

安装包：`build/distributions/FlowMarks-0.2.0.zip`。安装路径：`Settings > Plugins > 齿轮 > Install Plugin from Disk...`。

完整说明见 `README.md`。
