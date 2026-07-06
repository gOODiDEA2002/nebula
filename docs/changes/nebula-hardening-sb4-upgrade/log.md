# 变更日志：Nebula 硬化 + 升级 SB4.1

> 随开发实时追加。只记有复用价值的技术决策、踩坑、知识发现、Spec-Code 偏差，不记流水账。

---

## 技术决策

- **2026-07-06｜先修后升，全模块在范围**：用户确认新项目依赖全部模块，故按严重级别全局排序修复；先在 3.5.16 上做工作流 A（阻断级）再升 4.1，避免带未修的机制性失效跨版本。
- **2026-07-06｜ImportFilter 直接删除（Spec Q1）**：`NebulaAutoConfigurationImportFilter` 当前经 `.imports` 注册本就不生效（字节码已证），且其无条件排除 DataSource/MyBatis-Plus 行为过激，删除零风险。
- **2026-07-06｜密码哈希新增而非改旧（Spec Q6）**：`CryptoUtils.encrypt` 保留并 `@Deprecated`，新增 `hashPassword`(BCrypt)，避免破坏已落库的旧哈希。

## 知识发现

- **2026-07-06｜Spring Boot 3.5 的 `.imports` 只服务 AutoConfiguration**：反编译 spring-boot-3.5.8.jar 确认，`EnvironmentPostProcessor` 与 `AutoConfigurationImportFilter` 均只经 `SpringFactoriesLoader`（spring.factories）加载；`ImportCandidates.load`（读 `META-INF/spring/%s.imports`）全框架仅被 `AutoConfigurationImportSelector` 以 `AutoConfiguration.class` 调用。自建框架用 `.imports` 注册这两类处理器会静默失效。→ 值得沉淀到团队知识库。

## 踩坑记录

- **2026-07-06｜验证 task 的模块作用域是陷阱（来自 Codex 对抗性审查）**：T-A1-3 原把 RBAC 验证写成 `-pl core/nebula-security -am test`，但 Filter 的真实注册点 `SecurityAutoConfiguration` 在 `autoconfigure/nebula-autoconfigure`（imports:36），core 模块单测过了、Bean 却可能没被 starter 装配，链路照样断。已修正为 starter 级 Web 集成测试，并在阶段 A1 加了共用的"验证纪律"。教训：机制性失效的验证必须走 autoconfigure/starter 路径，不能停在拥有实现类的模块。

## 外部审查核实

- **2026-07-06｜Codex "版本不可解析" [critical] 质疑不成立**：Codex 称 Maven Central 无 3.5.16 / 4.1.0 GA / spring-cloud 2025.1.2（其执行日志无任何网络请求，系凭训练数据）。对照权威 `maven-metadata.xml` 核实：`spring-boot-starter-parent` release=4.1.0（3.5.16、4.0.7 均在列），`spring-cloud-dependencies` release=2025.1.2。目标版本均可直接消费。已在升级设计报告 §1 补版本复核声明。

## Spec-Code 偏差

_（开发中追加：实现与 Spec 不一致时，先更新 Spec 再改代码，并在此记录）_
