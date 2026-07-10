# Nebula 持久化接入指南

本文面向从标准 Spring Boot + MyBatis-Plus 接入 Nebula 持久化模块的业务应用。

## 最小配置

```yaml
nebula:
  data:
    persistence:
      enabled: true
      primary: primary
      mapper-packages: com.example.user.mapper,com.example.order.mapper
      sources:
        primary:
          url: jdbc:mysql://localhost:3306/app?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: ${DB_USER}
          password: ${DB_PASSWORD}
          driver-class-name: com.mysql.cj.jdbc.Driver
      mybatis-plus:
        mapper-locations: classpath*:/mapper/**/*.xml
        map-underscore-to-camel-case: true
```

`mapper-packages` 接受逗号分隔的 Java 包名。Mapper 可以继续继承
`com.baomidou.mybatisplus.core.mapper.BaseMapper`，无需改成框架专用接口。

## 迁移步骤

1. 引入 `nebula-data-persistence`，或选择包含该模块的 Starter。
2. 将 `spring.datasource.*` 迁移到 `nebula.data.persistence.sources.<name>.*`。
3. 设置 `primary`，并确认 `sources` 中存在同名数据源。
4. 将 `mapper-packages` 指向业务 Mapper 所在包。
5. 删除重复的 `@MapperScan`；如果应用还需要独立扫描规则，应先确认不会重复注册 Mapper。
6. 运行数据层回归测试，覆盖 Mapper 注入、CRUD、事务与分页。

持久化自动配置自身默认关闭。部分 Starter 会通过最低优先级默认值开启该模块，应用配置仍可用
`nebula.data.persistence.enabled=false` 显式关闭。

## 启动校验

启用持久化后，以下问题会在启动阶段直接报错：

- `sources` 为空。
- `primary` 对应的数据源不存在。
- 数据源 URL、驱动或凭据无效。

这种快速失败可以避免应用启动后才在第一条 SQL 上暴露配置错误。

## 可选能力

- 读写分离：启用 `nebula.data.read-write-separation.enabled=true`，并按实际拓扑配置主从数据源。
- 分库分表：启用 `nebula.data.sharding.enabled=true`，并提供 ShardingSphere 规则。
- 多级缓存：缓存配置独立位于 `nebula.data.cache.*`，不与数据源配置共用前缀。

配置键和默认值以 `DataPersistenceAutoConfiguration`、`DataSourceManager` 及对应
`@ConfigurationProperties` 类为准。
