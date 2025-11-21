# Nebula Starter Minimal - 配置参考

> 最小化Starter的完整配置说明，适用于CLI应用、批处理任务和工具库。

## 配置概览

本文档包含以下配置内容：

- [基础配置](#基础配置)
- [应用配置](#应用配置)
- [日志配置](#日志配置)
- [任务调度配置](#任务调度配置)
- [性能优化配置](#性能优化配置)
- [票务系统配置示例](#票务系统配置示例)

---

## 基础配置

### Maven依赖

在 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- Nebula Minimal Starter -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-minimal</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 最小配置文件

`application.yml`:

```yaml
spring:
  application:
    name: minimal-app
  main:
    web-application-type: none  # 禁用Web环境
```

---

## 应用配置

### 应用基本配置

`application.yml`:

```yaml
spring:
  application:
    name: minimal-app
    
  main:
    # 禁用Web环境（默认）
    web-application-type: none
    
    # 允许Bean定义覆盖
    allow-bean-definition-overriding: false
    
    # 懒加载
    lazy-initialization: false
    
    # 是否注册ShutdownHook
    register-shutdown-hook: true
    
  # Banner配置
  banner:
    location: classpath:banner.txt
    charset: UTF-8
```

### Spring Boot配置

```yaml
spring:
  # 配置文件位置
  config:
    location: classpath:/,file:./config/
    
  # Profile配置
  profiles:
    active: dev
    
  # 输出配置
  output:
    ansi:
      enabled: detect  # 彩色输出
```

---

## 日志配置

### 基础日志配置

`application.yml`:

```yaml
logging:
  # 日志级别
  level:
    root: INFO
    io.nebula: DEBUG
    com.mycompany: DEBUG
    
  # 日志格式
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
    
  # 日志文件
  file:
    name: logs/app.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
```

### Logback完整配置

创建 `logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志文件路径 -->
    <property name="LOG_HOME" value="./logs"/>
    <property name="APP_NAME" value="minimal-app"/>
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_HOME}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_HOME}/${APP_NAME}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 错误日志单独输出 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_HOME}/${APP_NAME}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_HOME}/${APP_NAME}-error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 根日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>
    
    <!-- 特定包日志级别 -->
    <logger name="io.nebula" level="DEBUG"/>
    <logger name="com.mycompany" level="DEBUG"/>
</configuration>
```

---

## 任务调度配置

### 启用定时任务

`application.yml`:

```yaml
spring:
  task:
    # 定时任务调度配置
    scheduling:
      pool:
        size: 5  # 线程池大小
      thread-name-prefix: task-  # 线程名称前缀
      shutdown:
        await-termination: true
        await-termination-period: 30s
```

### 定时任务示例

```java
@Configuration
@EnableScheduling
public class ScheduleConfig {
    
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
```

---

## 性能优化配置

### JVM参数

```bash
# 生产环境推荐配置
java -Xms256m \
     -Xmx512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=./logs/heap-dump.hprof \
     -jar app.jar
```

### 启动优化

`application.yml`:

```yaml
spring:
  main:
    lazy-initialization: true  # 启用懒加载，加快启动速度
    
  jmx:
    enabled: false  # 禁用JMX，减少内存占用
```

---

## 票务系统配置示例

### 场景1：数据导入脚本配置

`application.yml`:

```yaml
spring:
  application:
    name: ticket-import-tool
  main:
    web-application-type: none

# 导入配置
import:
  # 数据源文件
  source-file: tickets.csv
  # 输出文件
  output-file: tickets.json
  # 批量处理大小
  batch-size: 1000
  # 错误处理策略
  error-strategy: skip  # skip/abort

# 业务配置
business:
  # ID生成配置
  id:
    worker-id: 1
    datacenter-id: 1
    
logging:
  level:
    root: INFO
    com.ticketsystem: DEBUG
  file:
    name: logs/import.log
```

**自定义配置类**:

```java
@Configuration
@ConfigurationProperties(prefix = "import")
@Data
public class ImportConfig {
    private String sourceFile;
    private String outputFile;
    private int batchSize = 1000;
    private String errorStrategy = "skip";
}
```

### 场景2：报表生成工具配置

`application.yml`:

```yaml
spring:
  application:
    name: ticket-report-generator
  main:
    web-application-type: none
  task:
    scheduling:
      pool:
        size: 3

# 报表配置
report:
  # 报表输出目录
  output-dir: ./reports
  # 报表格式
  format: pdf  # pdf/excel/csv
  # 统计周期
  period: daily  # daily/weekly/monthly
  # 生成时间
  schedule-time: "0 0 2 * * *"  # 每天凌晨2点

# 邮件通知配置（可选）
notification:
  enabled: true
  recipients:
    - admin@example.com
    - manager@example.com

logging:
  level:
    root: INFO
    com.ticketsystem: DEBUG
  file:
    name: logs/report.log
```

**自定义配置类**:

```java
@Configuration
@ConfigurationProperties(prefix = "report")
@Data
public class ReportConfig {
    private String outputDir = "./reports";
    private String format = "pdf";
    private String period = "daily";
    private String scheduleTime;
}

@Configuration
@ConfigurationProperties(prefix = "notification")
@Data
public class NotificationConfig {
    private boolean enabled = false;
    private List<String> recipients = new ArrayList<>();
}
```

### 场景3：数据清理工具配置

`application.yml`:

```yaml
spring:
  application:
    name: ticket-data-cleanup
  main:
    web-application-type: none

# 清理配置
cleanup:
  # 清理策略
  strategies:
    - type: order-log
      retention-days: 90
      enabled: true
    - type: temp-files
      retention-days: 7
      enabled: true
    - type: session-data
      retention-days: 30
      enabled: true
      
  # 执行模式
  mode: auto  # auto/manual
  # 定时执行
  schedule: "0 0 3 * * *"  # 每天凌晨3点
  # 安全模式（先备份再删除）
  safe-mode: true
  # 备份目录
  backup-dir: ./backups

logging:
  level:
    root: INFO
    com.ticketsystem: DEBUG
  file:
    name: logs/cleanup.log
```

**自定义配置类**:

```java
@Configuration
@ConfigurationProperties(prefix = "cleanup")
@Data
public class CleanupConfig {
    
    private List<Strategy> strategies = new ArrayList<>();
    private String mode = "auto";
    private String schedule;
    private boolean safeMode = true;
    private String backupDir = "./backups";
    
    @Data
    public static class Strategy {
        private String type;
        private int retentionDays;
        private boolean enabled = true;
    }
}
```

### 场景4：批量处理工具配置

`application.yml`:

```yaml
spring:
  application:
    name: ticket-batch-processor
  main:
    web-application-type: none

# 批处理配置
batch:
  # 数据源
  source:
    type: file  # file/database/api
    path: data/input
    pattern: "*.json"
    
  # 处理配置
  processor:
    threads: 4  # 并发处理线程数
    batch-size: 100
    retry-times: 3
    timeout-seconds: 300
    
  # 输出配置
  output:
    type: file  # file/database/api
    path: data/output
    format: json
    
  # 错误处理
  error:
    continue-on-error: true
    error-log-file: logs/errors.log
    max-error-rate: 0.05  # 最大错误率5%

logging:
  level:
    root: INFO
    com.ticketsystem: DEBUG
  file:
    name: logs/batch.log
```

**自定义配置类**:

```java
@Configuration
@ConfigurationProperties(prefix = "batch")
@Data
public class BatchConfig {
    
    private SourceConfig source;
    private ProcessorConfig processor;
    private OutputConfig output;
    private ErrorConfig error;
    
    @Data
    public static class SourceConfig {
        private String type;
        private String path;
        private String pattern;
    }
    
    @Data
    public static class ProcessorConfig {
        private int threads = 1;
        private int batchSize = 100;
        private int retryTimes = 3;
        private int timeoutSeconds = 300;
    }
    
    @Data
    public static class OutputConfig {
        private String type;
        private String path;
        private String format;
    }
    
    @Data
    public static class ErrorConfig {
        private boolean continueOnError = true;
        private String errorLogFile;
        private double maxErrorRate = 0.05;
    }
}
```

---

## 环境配置

### 开发环境

`application-dev.yml`:

```yaml
logging:
  level:
    root: DEBUG
    io.nebula: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### 测试环境

`application-test.yml`:

```yaml
logging:
  level:
    root: INFO
    io.nebula: DEBUG
  file:
    name: logs/test-app.log
```

### 生产环境

`application-prod.yml`:

```yaml
logging:
  level:
    root: INFO
    io.nebula: INFO
  file:
    name: /var/log/app/app.log
    max-size: 100MB
    max-history: 30
```

---

## 配置文件优先级

配置文件的加载优先级（从高到低）：

1. 命令行参数：`--spring.application.name=myapp`
2. 外部配置文件：`./config/application.yml`
3. 内部配置文件：`classpath:application.yml`
4. 默认配置

---

## 配置加密

### 使用Jasypt加密敏感配置

1. 添加依赖：

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

2. 配置加密密钥：

```yaml
jasypt:
  encryptor:
    password: ${JASYPT_PASSWORD}  # 通过环境变量提供
    algorithm: PBEWithMD5AndDES
```

3. 加密敏感信息：

```bash
# 使用Jasypt CLI工具加密
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
     input="mysecret" \
     password="mykey" \
     algorithm=PBEWithMD5AndDES
```

4. 在配置文件中使用：

```yaml
database:
  password: ENC(加密后的字符串)
```

---

## 最佳实践

### 实践1：配置外部化

将敏感配置外部化，避免硬编码：

```bash
# 通过环境变量
export SPRING_PROFILES_ACTIVE=prod

# 通过命令行参数
java -jar app.jar --spring.profiles.active=prod

# 通过外部配置文件
java -jar app.jar --spring.config.location=file:./config/
```

### 实践2：配置分层

```
config/
├── application.yml           # 通用配置
├── application-dev.yml       # 开发环境
├── application-test.yml      # 测试环境
└── application-prod.yml      # 生产环境
```

### 实践3：配置验证

使用 `@Validated` 和 `@Value` 注解验证配置：

```java
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
@Data
public class AppConfig {
    
    @NotBlank(message = "应用名称不能为空")
    private String name;
    
    @Min(value = 1, message = "线程数至少为1")
    private int threads = 1;
    
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "ID格式不正确")
    private String workerId;
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

