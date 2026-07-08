# 变更日志 -- 剩余工作总体（nebula-remaining-work）

> 随开发实时追加；只记有复用价值的技术决策、踩坑与知识发现。

## 技术决策

### D1（设计期）：剩余工作分四阶段推进，阶段一/二/三串行为主

- 阶段一（审查修复）优先：两个 CRITICAL 属安全/可用性问题，是合入主干的前置条件。
- 阶段二（治理）在阶段一后：gRPC 形态定型（Boot 官方 starter）后才能裁决 `GrpcRpcProperties.ServerConfig` 遗留参数的去留；认证收敛依赖 Filter 注册机制稳定。
- 阶段三（Jackson 3）最后：错误码/认证收敛先做完，减少迁移期间的返工面；且依赖阶段一 Task 2 的 MockMvc 哨兵测试作为回归闸门。

### D2（设计期）：死配置采用"先盘点、后裁决、再动手"三步走

- 原因：审查报告点名的清单写于 A 批修复之前，部分项（如缓存 keyPrefix）已被后续修复接通；直接按旧清单删会误伤。Task 2-1 产出裁决表并经用户确认后才执行删除/接通。

### D3（2026-07-07，用户拍板 Q5/Q6）：阶段三缓存兼容与阶段四启动

- Q5 拍板：Jackson 3 迁移的 Redis 缓存序列化采用"升级清一次缓存"，不做双读兼容层（省一个兼容层的开发与回收成本）；升级指南写明清缓存步骤，proud-day 侧对应清 `pd:cache:*`。
- Q6 落地：proud-day 仓库 `/Users/andy/DevOps/SourceCode/business-projects/proud-day-projects/proud-day-backend`，已完成现状调研并在该仓库另立三件套 `docs/changes/nebula-persistence-adoption/`（spec + tasks + log）。
- Q7（2026-07-07 已确认）：b 分支 `revision` 升 `2.1.0-SNAPSHOT`。调研发现 proud-day 依赖坐标 `io.nebula:*:2.0.1-SNAPSHOT` 且 parent 为 Boot 3.5.8，与 b 分支（Boot 4.1）版本号相同——快照仓库一旦被 b 产物覆盖，proud-day 重构建即静默吃到跨代际类库。升代号是最低成本的隔离手段。

## 知识发现（proud-day 调研，2026-07-07）

- 33 个 Mapper 全部继承 MP 原生 `BaseMapper`——T-A4-1 的 markerInterface 设计生效，"零改动迁移"成立。
- proud-day pom 显式覆盖 `mybatis-plus-spring-boot3-starter:3.5.12`（注释注明为压制 nebula 传递依赖），与 nebula 新版的 MP 3.5.16 boot4 starter 冲突，迁移时必删。
- prod Hikari `initialization-fail-timeout: -1`（DB 不可达仍完成启动）在 `DataSourceManager.PoolConfig` 无对应参数，迁移后启动语义变化，已列入 proud-day spec 待澄清 Q2。
- proud-day 自有 `MybatisPlusConfig`（分页）/`MybatisAuditConfig`（审计兜底）与 nebula 侧同类 Bean 均为 `@ConditionalOnMissingBean` 关系，可共存不冲突，迁移期不必删。

## 死配置盘点表（Task 2-1 产出，待填写）

| 配置项 | 定义位置 | 消费点 | 裁决 | 理由 |
|--------|----------|--------|------|------|
| （盘点时填写） | | | | |

## 踩坑记录

（待开发过程中填写）

## 知识发现

（待开发过程中填写）

## Spec-Code 偏差

（实现与 Spec 不一致时，先更新 Spec 再改代码，并在此登记）
