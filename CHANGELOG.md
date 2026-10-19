# 变更记录

## 0.3.0 - 2026-10-19

- 书签 JSON 增加可选的 `step` 和 `level`，支持主步骤与子步骤编号。
- 树视图改为两行显示：标题在上，文件名和行号在下；子步骤自动缩进。
- 分组显示书签数量，旧版没有 `step/level` 的 JSON 保持兼容。
- Skill 输出格式加入步骤编号，并要求讨论结果使用简短模板。

## 0.2.1 - 2026-10-19

- 强化 `flowmarks-bookmarks` Skill：强制逐行验证锚点、拆分工作流边界、要求行为结论有代码证据。
- 增加 `validate_candidate.py`，自动校验候选路径、行号范围和 `expectedLineText`。
- 插件扫描 inbox 时同步校验文件存在、行号范围和 `expectedLineText`，坏候选不再进入导入列表。
- Skill 增加简短回复模板，讨论结果限制为结论、锚点表和关键约束。

## 0.2.0 - 2026-10-19

- 增加当前项目 `.flowmarks/inbox/` 候选目录自动扫描。
- 增加候选预览、导入、忽略和归档流程。
- 强制按 IntelliJ 项目隔离书签、候选和归档目录。
- 新增候选路径边界校验和损坏 JSON 错误提示。
- 新增 agent Skill 的“先讨论、确认后生成”流程。
- 增加候选扫描、项目隔离和归档测试。

## 0.1.0 - 2026-10-19

- 创建 FlowMarks 独立 IntelliJ IDEA 插件工程。
- 增加 FlowMarks 工具窗口和本地书签树。
- 支持标准 FlowMarks JSON 与 BookmarkAI 候选 JSON。
- 支持文件跳转、行号变化后的文本重定位、搜索、导入、导出、刷新和删除。
- 增加相对路径边界校验与原子保存。
- 增加核心模型单元测试和 Gradle Wrapper 构建检查。
- 增加 flowmarks-bookmarks agent Skill，规范代码链路书签 JSON 生成。
