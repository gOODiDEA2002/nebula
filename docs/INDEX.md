# Nebula 文档索引

本目录只把仍与当前实现一致的文档放在主入口。已完成的变更记录、旧版本评审和
Spring Boot 3 时期的长篇说明统一放在 [`archive/`](archive/README.md)，不再作为开发依据。

## 当前基线

| 项目 | 版本 |
| --- | --- |
| Nebula | `2.1.0` |
| Java | `21` |
| Spring Boot | `4.1.0` |
| Spring Framework | `7.x`，由 Spring Boot 管理 |
| Spring Cloud | `2025.1.2` |
| Spring AI | `2.0.0` |
| JSON | Jackson 3；仅第三方兼容边界保留 Jackson 2 |

依赖版本以根目录 [`pom.xml`](../pom.xml) 为准，文档中的版本表仅用于快速确认。

## 开始使用

- [快速开始](framework/QUICK_START.md)：创建应用、运行示例和最小配置。
- [Starter 选择指南](Nebula%20Starter%20选择指南.md)：按项目类型选择入口依赖。
- [配置说明](Nebula框架配置说明.md)：启用开关、安全配置和配置源码位置。
- [RAG 使用指南](framework/RAG_USAGE_GUIDE.md)：检索增强生成双管线架构、设计原理解析与接入示例。
- [示例应用](../examples/README.md)：可直接运行的真实工程，不再维护重复的文档版示例。

## 设计与维护

- [架构说明](framework/ARCHITECTURE.md)：模块边界、调用方向和关键约束。
- [自动配置指南](framework/AUTO_CONFIGURATION_GUIDE.md)：启用策略、Starter 默认值和扩展检查表。
- [测试说明](testing/README.md)：整仓、模块和定向测试命令。
- [贡献指南](CONTRIBUTING.md)：开发、验证和提交约定。
- [排查指南](Nebula%20Framework%20排查指南.md)：启动摘要与常见故障定位。
- [多 GitLab CI 配置](operations/MULTI_GITLAB_CI_SETUP.md)：多远端流水线配置。

## 升级资料

- [2.1 业务项目升级提示词](nebula-2.1-upgrade-agent-prompt.md)：用于依赖 Nebula 的业务项目升级。
- [Jackson 3 升级指南](upgrade-guide-jackson3.md)：包名迁移、边界处理和验证方式。
- [持久化接入说明](nebula-persistence-adoption.md)：持久化模块接入要点。

## 审查记录

- [Nebula 2.1 实现审查](reviews/nebula-2.1-implementation-review-2026-07.md)：本次升级后的代码审查、修复与验证结果。
- [历史资料](archive/README.md)：旧评审、已完成变更和 Boot 3 文档。

## 文档维护规则

1. 版本号只从根 `pom.xml` 读取，避免在多份文档里各自维护。
2. 配置项以对应的 `@ConfigurationProperties` 类为准。
3. 示例代码优先放在 `examples/`，文档只保留最短可运行片段。
4. 已完成的变更文档移入 `archive/changes/`，不继续混在当前使用手册中。
5. 旧版本资料不得从主索引链接为推荐阅读。
