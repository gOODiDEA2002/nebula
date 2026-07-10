# 变更日志 -- 审查问题修复（nebula-review-fixes）

> 随开发实时追加；只记有复用价值的技术决策、踩坑与知识发现，不记流水账。

## 技术决策

### D1（设计期）：C-2 选用 Boot 官方 gRPC Starter 而非 spring-grpc 项目 Starter

- 背景：Spring gRPC 1.1.0 起，其 Spring Boot 自动配置与 starter 已整体并入 Spring Boot（官方 What's New 明示）；Boot 4.1 BOM 直接托管 `spring-boot-starter-grpc-server`（`spring-boot-dependencies-4.1.0.pom:2399`）。
- 决策：依赖 `org.springframework.boot:spring-boot-starter-grpc-server`，版本随 Boot 升级自动对齐，避免再维护独立的 `spring-grpc.version`。
- 关键机制：任何 `BindableService` Bean 自动注册到 Server；`@GlobalServerInterceptor` Bean 自动生效；端口 `spring.grpc.server.port`（0=随机，测试配 `@LocalGrpcServerPort`）。

### D2（设计期）：EPP 只登记新键，不做新旧双注册

- 原因：SB4.1 的 `SpringFactoriesEnvironmentPostProcessorsFactory` 同时加载新键与废弃键（`loadDeprecatedPostProcessors`），同一类登记两键会被实例化并执行两次（defaults 注入幂等，但属隐患）。
- 决策：一次性迁到新键 `org.springframework.boot.EnvironmentPostProcessor` + 新接口，靠既有"starter defaults 生效"集成测试兜底。

### D3（设计期）：C-1 修复放在 starter defaults 而非代码硬编码

- 原因：`spring.http.converters.preferred-json-mapper` 必须在转换器自动配置评估前进入 Environment，EPP/defaults 是现成通道；且保持"用户 application.yml 永远可覆盖"的框架契约。
- 代价：裸依赖 `nebula-web`（不经 starter）的应用得不到该默认值，需在 JacksonConfig Javadoc 与使用指南中标注。

## 踩坑记录

（待开发过程中填写）

## 知识发现

- SB 4.1 下 MVC JSON 转换器的选择条件位于 `spring-boot-http-converter` 的 `Jackson2HttpMessageConvertersConfiguration$PreferJackson2OrJacksonUnavailableCondition`：classpath 同时存在 Jackson2/3 时默认用 Jackson 3，除非 `spring.http.converters.preferred-json-mapper=jackson2`。
- `NacosServiceAutoRegistrar` 早已按 `spring.grpc.server.port` 优先读取 gRPC 端口元数据（`NacosServiceAutoRegistrar.java:85-92`），与 Boot 官方属性天然对齐，Task 4 桥接后服务发现元数据链路自动打通。

## Spec-Code 偏差

（实现与 Spec 不一致时，先更新 Spec 再改代码，并在此登记）
