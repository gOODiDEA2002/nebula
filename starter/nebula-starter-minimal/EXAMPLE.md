# Nebula Starter Minimal - 使用示例

> 最小化Starter的完整使用示例，适用于CLI应用、批处理任务和工具库。

## 示例概览

本文档包含以下示例：

- [示例1：CLI命令行工具](#示例1cli命令行工具)
- [示例2：批处理任务](#示例2批处理任务)
- [示例3：数据处理脚本](#示例3数据处理脚本)
- [示例4：定时任务](#示例4定时任务)
- [示例5：工具库项目](#示例5工具库项目)
- [票务系统应用场景](#票务系统应用场景)

## 前提条件

### 环境要求

- **Java**：21+
- **Maven**：3.8+
- **Spring Boot**：3.2+

### 依赖配置

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-minimal</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

---

## 示例1：CLI命令行工具

### 场景说明

创建一个命令行工具，用于处理用户输入并执行相应操作。

### 实现步骤

#### 步骤1：创建主类

```java
package com.example.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.extern.slf4j.Slf4j;

/**
 * CLI工具主类
 */
@SpringBootApplication
@Slf4j
public class CliApplication implements CommandLineRunner {
    
    public static void main(String[] args) {
        // 禁用Web环境
        SpringApplication app = new SpringApplication(CliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("CLI工具启动成功");
        
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0];
        
        switch (command) {
            case "generate-id":
                generateId();
                break;
            case "encrypt":
                encrypt(args);
                break;
            case "format-date":
                formatDate(args);
                break;
            default:
                log.error("未知命令: {}", command);
                printUsage();
        }
    }
    
    private void printUsage() {
        System.out.println("用法:");
        System.out.println("  java -jar cli-tool.jar generate-id");
        System.out.println("  java -jar cli-tool.jar encrypt <text>");
        System.out.println("  java -jar cli-tool.jar format-date <date>");
    }
    
    private void generateId() {
        String id = IdGenerator.uuid();
        System.out.println("生成的ID: " + id);
    }
    
    private void encrypt(String[] args) {
        if (args.length < 2) {
            System.out.println("请提供要加密的文本");
            return;
        }
        
        String text = args[1];
        String encrypted = CryptoUtils.encrypt(text);
        System.out.println("加密结果: " + encrypted);
    }
    
    private void formatDate(String[] args) {
        if (args.length < 2) {
            System.out.println("请提供日期");
            return;
        }
        
        String dateStr = args[1];
        LocalDate date = DateUtils.parseDate(dateStr);
        String formatted = DateUtils.formatDate(date, "yyyy年MM月dd日");
        System.out.println("格式化结果: " + formatted);
    }
}
```

#### 步骤2：配置文件

`application.yml`:

```yaml
spring:
  application:
    name: cli-tool
  main:
    web-application-type: none  # 禁用Web环境

logging:
  level:
    root: INFO
    com.example: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

#### 步骤3：运行示例

```bash
# 编译打包
mvn clean package

# 运行命令
java -jar target/cli-tool.jar generate-id
java -jar target/cli-tool.jar encrypt "password123"
java -jar target/cli-tool.jar format-date "2025-11-20"
```

### 运行结果

```
生成的ID: 550e8400-e29b-41d4-a716-446655440000
```

---

## 示例2：批处理任务

### 场景说明

创建一个批处理任务，用于处理大量数据。

### 实现步骤

#### 步骤1：创建批处理服务

```java
package com.example.batch;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import io.nebula.core.util.JsonUtils;
import io.nebula.core.util.DateUtils;

/**
 * 批处理服务
 */
@Service
@Slf4j
public class BatchService {
    
    /**
     * 批量处理数据
     */
    public void processBatch(List<String> dataList) {
        log.info("开始批处理, 数据量: {}", dataList.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String data : dataList) {
            try {
                processOne(data);
                successCount++;
            } catch (Exception e) {
                log.error("处理失败: {}", data, e);
                failureCount++;
            }
        }
        
        log.info("批处理完成, 成功: {}, 失败: {}", successCount, failureCount);
    }
    
    private void processOne(String data) {
        // 处理单条数据
        log.debug("处理数据: {}", data);
        
        // 示例：JSON解析
        Map<String, Object> map = JsonUtils.toMap(data);
        
        // 示例：数据转换
        String id = IdGenerator.snowflakeIdString();
        map.put("id", id);
        map.put("processTime", DateUtils.nowDateTime());
        
        // 示例：输出结果
        String result = JsonUtils.toJson(map);
        log.debug("处理结果: {}", result);
    }
}
```

#### 步骤2：创建批处理任务

```java
package com.example.batch;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 批处理应用
 */
@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class BatchApplication {
    
    private final BatchService batchService;
    
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner batchRunner() {
        return args -> {
            log.info("批处理任务启动");
            
            // 读取数据文件
            List<String> dataList = Files.readAllLines(Paths.get("data.txt"));
            
            // 执行批处理
            batchService.processBatch(dataList);
            
            log.info("批处理任务完成");
        };
    }
}
```

#### 步骤3：运行示例

```bash
# 创建测试数据文件
echo '{"name":"test1"}' > data.txt
echo '{"name":"test2"}' >> data.txt
echo '{"name":"test3"}' >> data.txt

# 运行批处理
mvn spring-boot:run
```

---

## 示例3：数据处理脚本

### 场景说明

创建一个数据处理脚本，用于数据清洗、转换和导出。

### 实现代码

```java
package com.example.script;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.extern.slf4j.Slf4j;

import io.nebula.core.util.JsonUtils;
import io.nebula.core.util.DateUtils;
import io.nebula.core.util.CryptoUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据处理脚本
 */
@SpringBootApplication
@Slf4j
public class DataProcessScript implements CommandLineRunner {
    
    public static void main(String[] args) {
        SpringApplication.run(DataProcessScript.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("数据处理脚本启动");
        
        // 1. 读取原始数据
        List<String> lines = Files.readAllLines(Paths.get("input.json"));
        log.info("读取数据: {} 行", lines.size());
        
        // 2. 数据清洗和转换
        List<UserData> processedData = lines.stream()
            .map(this::parseJson)
            .filter(this::validate)
            .map(this::transform)
            .collect(Collectors.toList());
        
        log.info("数据处理完成: {} 行", processedData.size());
        
        // 3. 导出结果
        String output = JsonUtils.toPrettyJson(processedData);
        Files.writeString(Paths.get("output.json"), output);
        
        log.info("数据导出完成: output.json");
    }
    
    /**
     * 解析JSON
     */
    private UserData parseJson(String json) {
        try {
            return JsonUtils.fromJson(json, UserData.class);
        } catch (Exception e) {
            log.error("JSON解析失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * 数据验证
     */
    private boolean validate(UserData data) {
        if (data == null) {
            return false;
        }
        
        if (Strings.isBlank(data.getName())) {
            log.warn("用户名为空，跳过");
            return false;
        }
        
        return true;
    }
    
    /**
     * 数据转换
     */
    private UserData transform(UserData data) {
        // 生成ID
        if (data.getId() == null) {
            data.setId(IdGenerator.snowflakeIdString());
        }
        
        // 加密敏感数据
        if (data.getPhone() != null) {
            String encrypted = CryptoUtils.md5(data.getPhone());
            data.setPhoneHash(encrypted);
            data.setPhone(null);  // 清除原始手机号
        }
        
        // 添加处理时间
        data.setProcessTime(DateUtils.nowDateTime());
        
        return data;
    }
}

@Data
class UserData {
    private String id;
    private String name;
    private String phone;
    private String phoneHash;
    private LocalDateTime processTime;
}
```

---

## 示例4：定时任务

### 场景说明

创建一个定时任务应用，定期执行某些操作。

### 实现代码

```java
package com.example.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import io.nebula.core.util.DateUtils;
import io.nebula.core.util.IdGenerator;

/**
 * 定时任务应用
 */
@SpringBootApplication
@EnableScheduling
public class ScheduleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
    }
}

/**
 * 定时任务
 */
@Component
@Slf4j
class ScheduledTasks {
    
    /**
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * *")
    public void everyMinute() {
        log.info("定时任务执行: {}", DateUtils.formatDateTime(LocalDateTime.now()));
        
        // 执行业务逻辑
        String taskId = IdGenerator.shortId();
        log.info("任务ID: {}", taskId);
    }
    
    /**
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void dailyTask() {
        log.info("每日任务开始执行");
        
        // 数据清理、报表生成等
        cleanupOldData();
        generateReport();
        
        log.info("每日任务执行完成");
    }
    
    private void cleanupOldData() {
        // 清理7天前的数据
        LocalDate cutoffDate = DateUtils.plusDays(LocalDate.now(), -7);
        log.info("清理 {} 之前的数据", cutoffDate);
        
        // 清理逻辑...
    }
    
    private void generateReport() {
        // 生成报表
        String reportId = IdGenerator.orderNo();
        log.info("生成报表: {}", reportId);
        
        // 报表生成逻辑...
    }
}
```

**配置文件**:

```yaml
spring:
  application:
    name: schedule-app
  task:
    scheduling:
      pool:
        size: 5  # 线程池大小
      thread-name-prefix: schedule-

logging:
  level:
    root: INFO
    com.example: DEBUG
```

---

## 示例5：工具库项目

### 场景说明

创建一个工具库，提供常用的业务工具类。

### 实现代码

```java
package com.example.util;

import io.nebula.core.util.IdGenerator;
import io.nebula.core.util.DateUtils;
import io.nebula.core.util.CryptoUtils;
import io.nebula.core.util.JsonUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业务工具类
 */
public class BusinessUtils {
    
    /**
     * 生成业务单号
     * 格式: 前缀 + yyyyMMdd + 8位随机字符
     */
    public static String generateBusinessNo(String prefix) {
        String date = DateUtils.formatDate(LocalDate.now(), "yyyyMMdd");
        String random = IdGenerator.shortId(8);
        return prefix + date + random;
    }
    
    /**
     * 计算年龄
     */
    public static int calculateAge(LocalDate birthDate) {
        LocalDate now = LocalDate.now();
        return (int) DateUtils.yearsBetween(birthDate, now);
    }
    
    /**
     * 手机号脱敏
     * 138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 生成签名
     */
    public static String generateSignature(Map<String, String> params, String secret) {
        // 1. 参数排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        // 2. 拼接字符串
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append("=").append(params.get(key)).append("&");
        }
        sb.append("secret=").append(secret);
        
        // 3. MD5签名
        return CryptoUtils.md5(sb.toString());
    }
    
    /**
     * 对象深拷贝
     */
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        return JsonUtils.deepCopy(obj, clazz);
    }
}
```

**单元测试**:

```java
package com.example.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

class BusinessUtilsTest {
    
    @Test
    void testGenerateBusinessNo() {
        String orderNo = BusinessUtils.generateBusinessNo("ORDER");
        assertNotNull(orderNo);
        assertTrue(orderNo.startsWith("ORDER"));
        assertEquals(21, orderNo.length());  // ORDER + 8位日期 + 8位随机
    }
    
    @Test
    void testCalculateAge() {
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        int age = BusinessUtils.calculateAge(birthDate);
        assertTrue(age >= 34);  // 2025年算，至少34岁
    }
    
    @Test
    void testMaskPhone() {
        String masked = BusinessUtils.maskPhone("13812345678");
        assertEquals("138****5678", masked);
    }
    
    @Test
    void testGenerateSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "12345");
        params.put("amount", "100.00");
        
        String signature = BusinessUtils.generateSignature(params, "mysecret");
        assertNotNull(signature);
        assertEquals(32, signature.length());  // MD5结果长度
    }
}
```

---

## 票务系统应用场景

### 场景1：票务数据导入脚本

```java
package com.ticketsystem.import;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.extern.slf4j.Slf4j;

import io.nebula.core.util.JsonUtils;
import io.nebula.core.util.DateUtils;
import io.nebula.core.util.IdGenerator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 票务数据导入工具
 */
@SpringBootApplication
@Slf4j
public class TicketImportTool implements CommandLineRunner {
    
    public static void main(String[] args) {
        SpringApplication.run(TicketImportTool.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始导入票务数据");
        
        // 1. 读取CSV文件
        List<String> lines = Files.readAllLines(Paths.get("tickets.csv"));
        log.info("读取到 {} 条记录", lines.size() - 1);  // 减去标题行
        
        // 2. 解析并转换数据
        List<TicketData> tickets = new ArrayList<>();
        
        for (int i = 1; i < lines.size(); i++) {  // 跳过标题行
            String line = lines.get(i);
            String[] fields = line.split(",");
            
            TicketData ticket = new TicketData();
            ticket.setId(IdGenerator.snowflakeIdString());
            ticket.setMovieName(fields[0]);
            ticket.setCinemaName(fields[1]);
            ticket.setShowtime(DateUtils.parseDateTime(fields[2], "yyyy-MM-dd HH:mm"));
            ticket.setSeatNo(fields[3]);
            ticket.setPrice(new BigDecimal(fields[4]));
            ticket.setImportTime(DateUtils.nowDateTime());
            
            tickets.add(ticket);
        }
        
        // 3. 导出JSON格式
        String json = JsonUtils.toPrettyJson(tickets);
        Files.writeString(Paths.get("tickets.json"), json);
        
        log.info("数据导入完成: {} 条记录", tickets.size());
    }
}

@Data
class TicketData {
    private String id;
    private String movieName;
    private String cinemaName;
    private LocalDateTime showtime;
    private String seatNo;
    private BigDecimal price;
    private LocalDateTime importTime;
}
```

### 场景2：票务报表生成

```java
package com.ticketsystem.report;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;

import io.nebula.core.util.DateUtils;
import io.nebula.core.util.IdGenerator;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 票务报表生成工具
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
public class ReportGenerator {
    
    public static void main(String[] args) {
        SpringApplication.run(ReportGenerator.class, args);
    }
    
    /**
     * 每天凌晨2点生成前一天的报表
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void generateDailyReport() {
        log.info("开始生成每日报表");
        
        // 1. 统计昨天的数据
        LocalDate yesterday = DateUtils.plusDays(LocalDate.now(), -1);
        String reportDate = DateUtils.formatDate(yesterday);
        
        // 2. 生成报表内容
        StringBuilder report = new StringBuilder();
        report.append("票务系统每日报表\n");
        report.append("日期: ").append(reportDate).append("\n");
        report.append("生成时间: ").append(DateUtils.formatDateTime(LocalDateTime.now())).append("\n");
        report.append("\n");
        report.append("统计数据:\n");
        report.append("- 总销售额: ¥XXX\n");
        report.append("- 售出票数: XXX张\n");
        report.append("- 场次数量: XXX场\n");
        report.append("- 活跃用户: XXX人\n");
        
        // 3. 保存报表
        String filename = "report-" + reportDate + ".txt";
        try {
            Files.writeString(Paths.get(filename), report.toString());
            log.info("报表生成成功: {}", filename);
        } catch (Exception e) {
            log.error("报表生成失败", e);
        }
    }
}
```

### 场景3：数据清理脚本

```java
package com.ticketsystem.cleanup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.extern.slf4j.Slf4j;

import io.nebula.core.util.DateUtils;

import java.time.LocalDateTime;

/**
 * 数据清理工具
 */
@SpringBootApplication
@Slf4j
public class DataCleanupTool implements CommandLineRunner {
    
    public static void main(String[] args) {
        SpringApplication.run(DataCleanupTool.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始清理过期数据");
        
        // 1. 清理90天前的订单日志
        LocalDateTime cutoffTime = DateUtils.plusDays(LocalDateTime.now(), -90);
        log.info("清理时间点: {}", DateUtils.formatDateTime(cutoffTime));
        
        // 2. 执行清理
        int deletedCount = cleanupOrders(cutoffTime);
        log.info("清理订单日志: {} 条", deletedCount);
        
        // 3. 清理临时文件
        int deletedFiles = cleanupTempFiles();
        log.info("清理临时文件: {} 个", deletedFiles);
        
        log.info("数据清理完成");
    }
    
    private int cleanupOrders(LocalDateTime cutoffTime) {
        // 清理逻辑（实际应该连接数据库）
        log.info("执行SQL: DELETE FROM t_order_log WHERE create_time < ?", cutoffTime);
        return 0;  // 模拟返回删除数量
    }
    
    private int cleanupTempFiles() {
        // 清理临时文件
        return 0;  // 模拟返回删除数量
    }
}
```

---

## 最佳实践

### 实践1：禁用Web环境

对于CLI应用，应该禁用Web环境以减少内存占用：

```java
@SpringBootApplication
public class CliApplication {
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);  // 禁用Web
        app.run(args);
    }
}
```

或在配置文件中：

```yaml
spring:
  main:
    web-application-type: none
```

### 实践2：优雅退出

```java
@Component
class GracefulShutdown {
    
    @PreDestroy
    public void cleanup() {
        log.info("应用正在关闭，执行清理工作...");
        // 清理资源、关闭连接等
    }
}
```

### 实践3：参数验证

```java
@Override
public void run(String... args) throws Exception {
    if (args.length < 2) {
        throw new IllegalArgumentException("参数不足，需要至少2个参数");
    }
    
    // 业务逻辑
}
```

### 实践4：异常处理

```java
@Override
public void run(String... args) {
    try {
        // 业务逻辑
        process();
    } catch (BusinessException e) {
        log.error("业务处理失败: {}", e.getMessage());
        System.exit(1);
    } catch (Exception e) {
        log.error("系统错误", e);
        System.exit(2);
    }
}
```

---

## 完整示例项目

参考示例项目：`examples/starter-minimal-example`

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

