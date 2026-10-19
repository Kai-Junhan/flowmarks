# 贡献说明

FlowMarks 仍处于早期阶段。贡献请聚焦于核心流程、数据安全和跨机器可复现构建。

## 本地验证

提交前运行：

~~~text
./gradlew clean test buildPlugin
~~~

Windows PowerShell 使用 ./gradlew.bat。运行测试 IDE 时，通过 -PideaPath 或 FLOWMARKS_IDEA_PATH 提供本机 IDEA 安装目录。

## 代码要求

- 所有书签处理默认在本地完成。
- 不增加云端接口、遥测、登录、会员、许可证校验或 BookmarkAI 运行时依赖。
- 不把业务项目或 .idea/workspace.xml 纳入插件工程。
- 路径变更必须保留项目根目录边界校验。
- JSON 损坏、文件不存在和跳转失败应显示清晰状态，不应导致插件崩溃。
- 新增行为应补充相应单元测试。

变更说明应列出目的、验证命令，以及是否影响 JSON 格式或兼容性。不要提交构建产物、IDE 沙箱或个人配置。
