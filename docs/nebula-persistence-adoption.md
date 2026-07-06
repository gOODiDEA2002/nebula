# Nebula 持久化接入指南

> 面向从"标准 Spring Boot MyBatis-Plus"迁移到 Nebula 持久化的业务应用（如 proud-day 走 B）。
> 前提：已合入 T-A4-1（mapper 扫描可配置）、T-A4-2（数据源 fail-fast）。

## 1. 最小配置

```yaml
nebula:
  data:
    persistence:
      enabled: true                       # 显式开启(默认关闭)
      primary: primary                    # 主数据源名(默认 primary)
      mapper-packages: com.myapp.module   # 逗号分隔; 指向你自己的 mapper 包(默认 io.nebula)
      sources:                            # 数据源集合(Map)
        primary:
          url: jdbc:mysql://host:3306/db?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: ${DB_USER}
          password: ${DB_PASS}
          driver-class-name: com.mysql.cj.jdbc.Driver
      # mybatis-plus 相关(可选)
      mybatis-plus:
        mapper-locations: classpath*:/mapper/**/*.xml
        map-underscore-to-camel-case: true
```

要点：
- **Mapper 不必改继承**：`mapper-packages` 指向你的 mapper 包后，继承标准 `com.baomidou.mybatisplus.core.mapper.BaseMapper` 的 Mapper 也会被扫描（markerInterface 已改用 MP 原生 BaseMapper）。
- **移除自建 `@MapperScan`**：交给 Nebula 的 `MapperScannerConfigurer` 统一扫描，避免重复扫描。
- **数据源配置从 `spring.datasource.*` 迁到 `nebula.data.persistence.sources.<name>.*`**。
- 未配置数据源却开了 `enabled=true` 会 **fail-fast**（明确报错），而非启动后报与根因脱节的错误。

## 2. 从标准 Spring Boot MyBatis-Plus 迁移（proud-day B 的步骤）

1. `pom.xml`：确认依赖 `nebula-data-persistence`（proud-day 已依赖）。
2. 配置：
   - 删除/迁移 `spring.datasource.*` → `nebula.data.persistence.sources.primary.*`。
   - 新增 `nebula.data.persistence.enabled: true` 与 `nebula.data.persistence.mapper-packages: com.proudday.module`。
3. 代码：
   - 移除启动类上的 `@MapperScan("com.proudday.module.**.mapper")`（改由 Nebula 扫描）。
   - 33 个 Mapper **无需改动**（继续继承 MyBatis-Plus 原生 BaseMapper）。
4. 回归：本地/预发跑全量数据层用例，确认 Mapper 注入、CRUD、分页正常。

## 3. 可选能力

- **读写分离**：`nebula.data.read-write-separation.enabled: true` + `dynamic-routing: true`（此模式下 Nebula 会用动态数据源接管，见 T-A4-5）。proud-day 当前不需要。
- **分库分表**：`nebula.data.sharding.enabled: true`（ShardingSphere）。

## 4. 注意事项

- Nebula 持久化默认关闭（`matchIfMissing=false`）；只用标准 MyBatis-Plus 的应用**不要**设 `enabled=true`，或干脆不依赖本模块。
- 与其他应用共用同一 Redis/DB 库时，缓存 key 前缀（`nebula.data.cache.redis.key-prefix`）与本数据源配置互不影响。
