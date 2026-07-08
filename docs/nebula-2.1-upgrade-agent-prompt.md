# Nebula 2.1 升级通用提示词（交给编码代理执行）

> 适用场景：依赖 nebula 框架的业务项目从 nebula 2.0.x（Spring Boot 3.5.x 代际）升级到
> nebula 2.1.0-SNAPSHOT（Spring Boot 4.1.0 + Jackson 3 代际）。
> 使用方式：替换下方提示词中的【】占位符后，原样发给编码代理（在目标项目的工作区新开会话）。
> 模板来源：proud-day-backend 迁移实战（2026-07-08），已知坑清单均为真实踩坑记录。

## 派发前用户自查（每次派发前确认）

- [ ] nebula 仓库 `nebula-remaining-work` 分支（或已合并的主线）已在本机 `mvn clean install -DskipTests`，
      `~/.m2/repository/io/nebula/` 下有 2.1.0-SNAPSHOT 产物（必须包含 commit `188011e4` 的双 Bean 修复）
- [ ] 目标项目本地可编译、工作区干净
- [ ] 目标项目依赖的中间件（MySQL/Redis 等）本地或测试环境可达（启动验证需要）

---

## 提示词正文（复制以下内容发给代理）

```text
你在【项目名称】仓库（路径：【项目绝对路径】）执行 nebula 框架升级任务：
从 nebula 2.0.x（Spring Boot 3.5.x）升级到 nebula 2.1.0-SNAPSHOT
（Spring Boot 4.1.0 + Jackson 3 代际）。

【项目补充说明：一句话描述项目用途、启动方式、冒烟脚本（如有）、
 使用了 nebula 的哪些模块（web/persistence/cache/messaging/websocket 等）】

════════ 第零步：开工准备 ════════

1. 确认工作区干净后，从主分支新建分支 nebula-2.1-upgrade。
2. 通读 nebula 仓库的升级指南：
   /Users/andy/DevOps/SourceCode/nebula-projects/nebula/docs/upgrade-guide-jackson3.md
3. 在本项目创建 docs/changes/nebula-2.1-upgrade/log.md，
   全程记录：每个任务的验证命令关键输出行、技术决策、踩坑、偏差。
   只写"已通过"三个字不算数，要留能对账的证据。
4. 确认 ~/.m2/repository/io/nebula/ 下存在 2.1.0-SNAPSHOT 产物；
   不存在就停下汇报，禁止自行去改 nebula 仓库或降级方案。

════════ 任务清单（顺序执行，一任务一提交） ════════

── Task 1: POM 版本对齐 + SB4 编译适配 ──

1. pom.xml：
   - parent spring-boot-starter-parent 升 4.1.0（这一步是硬前提，
     不升 parent 而只升 nebula 版本，启动时必报
     ClassNotFoundException: JsonMapperBuilderCustomizer 一类的错——
     那是 SB3.5 应用消费 SB4 框架的预期失败，不是 nebula 的 Bug）
   - nebula.version 改 2.1.0-SNAPSHOT
   - 删除与 nebula 传递依赖重复的显式覆盖（常见：mybatis-plus-spring-boot3-starter、
     mybatis-plus-jsqlparser、jackson 系显式版本）——nebula 已传递正确的 boot4 变体
   - 若用了 Spring Cloud 组件，BOM 升 2025.1.2
2. 编译修复：parent 升级后出现的 SB4 API/包名编译错误属本任务范围，
   逐个修复并把修复内容记入 log.md。常见点：
   - WebMvcConfigurer/拦截器相关 API 签名变化
   - jakarta 包名残留（如果项目历史久）
   - 废弃 API 替换（编译告警也顺带看一眼，但只修报错的）
3. 验收：mvn clean compile BUILD SUCCESS；
   mvn dependency:tree -Dincludes=com.baomidou（如用持久化）确认
   mybatis-plus-spring-boot4-starter 3.5.16 经 nebula 传递。

── Task 2: Jackson 2 → 3 应用代码迁移 ──

按升级指南迁移本项目源码中的 Jackson 2 使用：
1. 全项目扫描：rg -l "com.fasterxml.jackson" --type java
2. 迁移规则：
   - import com.fasterxml.jackson.databind/core/datatype/module.* → tools.jackson.*
   - com.fasterxml.jackson.annotation.* 不动（Jackson 3 保留原包名）
   - new ObjectMapper() → JsonMapper.builder().build()
     （import tools.jackson.databind.json.JsonMapper）
   - 删除 JavaTimeModule 注册与 WRITE_DATES_AS_TIMESTAMPS（Jackson 3 内置）
   - JsonProcessingException → tools.jackson.core.JacksonException（已改非受检）
   - node.fields() → node.properties()
   - 自定义 Jackson2ObjectMapperBuilderCustomizer → JsonMapperBuilderCustomizer
3. POM 中显式的 com.fasterxml.jackson.core:jackson-databind 换
   tools.jackson.core:jackson-databind；jackson-datatype-jsr310 直接删。
4. 验收：mvn clean compile && mvn test 全绿。
   注意：jjwt-jackson 传递的 Jackson 2 属已知合规残留，不要试图消除它。

── Task 3: 配置审计（重点，逐 profile 核对，不许只看一个环境） ──

对 application.yml + 每一个 application-{profile}.yml 逐项核对：

1. 【关键坑】缓存 key-prefix：逐 profile 确认
   nebula.data.cache.redis.key-prefix 的实际值。没配的 profile 用的是
   nebula 默认前缀 "nebula:cache:"——发布说明里的清缓存命令必须按
   "该环境的实际前缀"写，别想当然照抄 dev 的前缀（proud-day 迁移时
   发布说明照 dev 写 pd:cache:*，而 prod 实际是 nebula:cache:*，
   差点上线漏清全部旧缓存）。建议借本次必须清缓存的窗口，给所有
   profile 显式声明统一的项目专属前缀。
2. 数据源：若项目还在用 spring.datasource.* 且计划切 nebula 持久化，
   迁移到 nebula.data.persistence.sources.primary.*，删 @MapperScan
   改用 nebula.data.persistence.mapper-packages；Hikari 池参数按
   nebula 的 pool.* 命名映射。注意：原
   initialization-fail-timeout: -1（DB 不可达也启动）在 nebula
   DataSourceManager 无对应参数，迁移后 DB 不可达即启动失败——
   此语义变化必须写进发布说明。若本项目不切持久化则跳过本条。
3. 废弃/失效配置清理：
   - spring.http.converters.preferred-json-mapper=jackson2 若项目自己
     配过，删除（nebula 2.1 已全面 Jackson 3）
   - nebula.security.jwt.secret 必须显式配置且 >= 32 字符
   - CORS allowedOrigins 默认空数组，生产必须显式配置
4. 废弃 API 检查（编译不报错但已废弃）：
   @RpcClient → @RemoteService；@MessageHandler → @MessageListener；
   io.nebula.web.auth.JwtUtils → io.nebula.security.jwt.JwtService
5. 验收：SPRING_PROFILES_ACTIVE=prod 配置绑定 dry-run 无报错
   （不真连生产中间件）。

── Task 4: 全量回归 + 启动验证 + 发布说明 ──

1. mvn clean test 全量通过。
2. mvn spring-boot:run（dev/test profile）启动成功，确认日志中
   数据源/缓存/消息等模块初始化正常；有冒烟脚本就跑冒烟脚本。
   启动因中间件不可达失败属环境问题：记录后重试或汇报，不许改代码绕过。
3. 在 log.md 写发布说明，必须包含：
   - 清 Redis 旧缓存：按 Task 3 核对出的"各环境实际前缀"写清理命令，
     用 redis-cli --scan --pattern '<实际前缀>*' | xargs -r redis-cli DEL
     （禁止用 KEYS 命令，会阻塞生产 Redis）
   - 滚动升级不可行：Jackson 2/3 缓存格式互不兼容，需停机或蓝绿发布
   - DB 不可达启动语义变化（若迁了持久化）
   - 其余本项目特有的行为变化
4. 更新项目的 CLAUDE.md / README 技术栈表（Boot 4.1.0、nebula 2.1.0）。

════════ 执行纪律（全程有效） ════════

- 一任务一提交，提交信息格式：类型(范围): 摘要 (Task N)。
- 每个任务的验证命令必须真实执行且用默认 Maven 配置
  （禁止自定义 settings.xml，产物必须进默认 ~/.m2）。
- 测试失败修代码不修测试；禁止注释/删除/弱化断言或加 @Disabled。
- 同一任务验证连续失败 2 次，停下：把命令、报错关键行、已尝试的修法
  记入 log.md 后向我汇报，不许绕道或跳过。
- 疑似 nebula 框架侧问题（Bean 冲突、自动配置异常等）：不许在本项目
  打补丁绕过，记入 log.md 并停下汇报，问题回流 nebula 仓库处理。
  判断参考：报错栈指向 io.nebula.* 类且本项目配置无误，多半是框架问题。
- 不夹带升级范围外的重构/清理；发现新问题记 log.md，不顺手修。
- 代码与注释不用表情符号，注释用中文。

════════ 完成后汇报格式 ════════

每任务一行：Task 编号 | 提交 hash | 验证命令关键输出（测试数/BUILD SUCCESS/启动秒数）。
另附：发布说明全文、log.md 新增条目索引、遗留问题清单。
```

---

## 已知坑速查（代理汇报异常时用户对照用）

| 症状 | 根因 | 处置 |
|---|---|---|
| `ClassNotFoundException: JsonMapperBuilderCustomizer` | parent 没升 4.1.0，SB3.5 消费 SB4 框架 | 补升 parent，不是 nebula 的 Bug |
| `redisMessageConsumer required a single bean, but 2 were found` | 旧快照缺双 Bean 修复 | 确认 ~/.m2 快照含 nebula commit `188011e4`，重新 install |
| 上线后缓存反序列化 `InvalidTypeIdException` | 旧缓存没清干净（多为前缀核对错） | 按各环境实际 key-prefix 重清 |
| DB 不可达应用起不来（原来能起） | DataSourceManager 无 initialization-fail-timeout 语义 | 预期行为，靠编排层重启补救 |
| `mvn test` 绿但启动失败 | 单测不拉起完整上下文 | 启动验证是独立闸门，两者都必须过 |
