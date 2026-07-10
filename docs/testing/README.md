# Nebula 测试说明

## 验证层级

| 层级 | 目的 | 命令 |
| --- | --- | --- |
| 干净编译 | 排除旧构建产物和模块依赖遗漏 | `mvn clean compile` |
| 整仓测试 | 验证所有单元与集成测试 | `mvn test` |
| 模块测试 | 快速验证局部改动 | `mvn test -pl <module> -am` |
| 单类测试 | 调试一个行为 | `mvn test -pl <module> -Dtest=<TestClass>` |
| 依赖收敛 | 检查实际解析版本 | `mvn dependency:tree -pl <module>` |

升级依赖、修改公共接口、调整自动配置或跨模块修改时，最终验证必须从 `clean` 开始。

## 常用命令

```bash
# 整仓干净测试
mvn clean test

# Web 模块及其上游依赖
mvn test -pl application/nebula-web -am

# 自动配置模块的定向测试
mvn test -pl autoconfigure/nebula-autoconfigure -am \
  -Dtest=HttpRpcClientConfigTest,GrpcRpcAutoConfigurationConditionTest \
  -Dsurefire.failIfNoSpecifiedTests=false

# 查看 Springdoc 和 JJWT 的实际版本
mvn dependency:tree -pl application/nebula-web \
  -Dincludes=org.springdoc:*,io.jsonwebtoken:*
```

## 测试选择

- 纯转换、校验和算法使用单元测试。
- 自动配置使用 `ApplicationContextRunner`，至少覆盖默认状态、显式开启、显式关闭和缺类。
- 序列化组件使用往返测试，包含泛型 payload、时间类型和未知字段。
- RPC、鉴权和消息链路至少保留一个真实客户端到真实服务端的本地集成测试。
- 外部服务使用 Testcontainers 或可控测试替身，不依赖开发机上碰巧运行的服务。
- 安全防护同时测试允许路径和拒绝路径，例如空 Profile、混合 Profile 和生产 Profile。

## 测试输出

JaCoCo 数据和 `target/` 均为构建产物，不提交仓库。增量执行后出现
「classes do not match execution data」时，先运行 `mvn clean`，再判断是否为代码问题。

Mockito 在较新 JDK 上可能提示动态加载 Agent 将来会受限。当前属于构建警告，不影响测试
结果；后续升级测试工具链时应改为显式 Agent 配置。
