# Nebula Starter Minimal - 测试指南

> 最小化Starter的完整测试指南，包括单元测试、集成测试和最佳实践。

## 测试概览

本文档包含以下测试内容：

- [测试环境准备](#测试环境准备)
- [单元测试](#单元测试)
- [集成测试](#集成测试)
- [工具类测试](#工具类测试)
- [CLI应用测试](#cli应用测试)
- [批处理任务测试](#批处理任务测试)
- [票务系统测试示例](#票务系统测试示例)

---

## 测试环境准备

### Maven依赖

在 `pom.xml` 中添加测试依赖：

```xml
<dependencies>
    <!-- Nebula Minimal Starter -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-minimal</artifactId>
        <version>2.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 测试配置文件

创建 `src/test/resources/application-test.yml`:

```yaml
spring:
  application:
    name: test-app
  main:
    web-application-type: none

logging:
  level:
    root: INFO
    io.nebula: DEBUG
    com.example: DEBUG
```

---

## 单元测试

### 测试基础工具类

#### 测试ID生成器

```java
package com.example.util;

import io.nebula.core.util.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * ID生成器测试
 */
@DisplayName("ID生成器测试")
class IdGeneratorTest {
    
    @Test
    @DisplayName("生成UUID")
    void testUuid() {
        String uuid = IdGenerator.uuid();
        
        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(36);
        assertThat(uuid).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }
    
    @Test
    @DisplayName("生成短ID")
    void testShortId() {
        String shortId = IdGenerator.shortId();
        
        assertThat(shortId).isNotNull();
        assertThat(shortId).hasSizeBetween(8, 12);
    }
    
    @Test
    @DisplayName("生成Snowflake ID")
    void testSnowflakeId() {
        long id1 = IdGenerator.snowflakeId();
        long id2 = IdGenerator.snowflakeId();
        
        assertThat(id1).isGreaterThan(0);
        assertThat(id2).isGreaterThan(id1);  // ID递增
    }
    
    @Test
    @DisplayName("生成订单号")
    void testOrderNo() {
        String orderNo = IdGenerator.orderNo();
        
        assertThat(orderNo).isNotNull();
        assertThat(orderNo).startsWith("ORD");
        assertThat(orderNo).hasSize(23);  // ORD + 20位数字
    }
    
    @Test
    @DisplayName("并发生成ID不重复")
    void testConcurrentGeneration() throws InterruptedException {
        int threadCount = 10;
        int idsPerThread = 1000;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(IdGenerator.snowflakeId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 验证生成的ID数量正确且无重复
        assertThat(ids).hasSize(threadCount * idsPerThread);
    }
}
```

#### 测试加密工具

```java
package com.example.util;

import io.nebula.core.util.CryptoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * 加密工具测试
 */
@DisplayName("加密工具测试")
class CryptoUtilsTest {
    
    @Test
    @DisplayName("MD5哈希")
    void testMd5() {
        String text = "hello";
        String hash = CryptoUtils.md5(text);
        
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(32);
        assertThat(hash).isEqualTo("5d41402abc4b2a76b9719d911017c592");
    }
    
    @Test
    @DisplayName("密码加密验证")
    void testPasswordEncryption() {
        String password = "myPassword123";
        
        // 加密
        String encrypted = CryptoUtils.encryptPassword(password);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(password);
        
        // 验证
        boolean matches = CryptoUtils.matchesPassword(password, encrypted);
        assertThat(matches).isTrue();
        
        // 错误密码
        boolean wrongMatches = CryptoUtils.matchesPassword("wrongPassword", encrypted);
        assertThat(wrongMatches).isFalse();
    }
    
    @Test
    @DisplayName("AES加密解密")
    void testAesEncryption() {
        String text = "sensitive data";
        String key = "mySecretKey12345";
        
        // 加密
        String encrypted = CryptoUtils.aesEncrypt(text, key);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(text);
        
        // 解密
        String decrypted = CryptoUtils.aesDecrypt(encrypted, key);
        assertThat(decrypted).isEqualTo(text);
    }
    
    @Test
    @DisplayName("Base64编码解码")
    void testBase64() {
        String text = "Hello World";
        
        // 编码
        String encoded = CryptoUtils.base64Encode(text);
        assertThat(encoded).isNotNull();
        assertThat(encoded).isEqualTo("SGVsbG8gV29ybGQ=");
        
        // 解码
        String decoded = CryptoUtils.base64Decode(encoded);
        assertThat(decoded).isEqualTo(text);
    }
}
```

#### 测试日期工具

```java
package com.example.util;

import io.nebula.core.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * 日期工具测试
 */
@DisplayName("日期工具测试")
class DateUtilsTest {
    
    @Test
    @DisplayName("格式化日期")
    void testFormatDate() {
        LocalDate date = LocalDate.of(2025, 11, 20);
        
        String formatted1 = DateUtils.formatDate(date);
        assertThat(formatted1).isEqualTo("2025-11-20");
        
        String formatted2 = DateUtils.formatDate(date, "yyyy年MM月dd日");
        assertThat(formatted2).isEqualTo("2025年11月20日");
    }
    
    @Test
    @DisplayName("解析日期")
    void testParseDate() {
        LocalDate date = DateUtils.parseDate("2025-11-20");
        
        assertThat(date).isNotNull();
        assertThat(date.getYear()).isEqualTo(2025);
        assertThat(date.getMonthValue()).isEqualTo(11);
        assertThat(date.getDayOfMonth()).isEqualTo(20);
    }
    
    @Test
    @DisplayName("日期加减")
    void testDateCalculation() {
        LocalDate date = LocalDate.of(2025, 11, 20);
        
        LocalDate plus7Days = DateUtils.plusDays(date, 7);
        assertThat(plus7Days).isEqualTo(LocalDate.of(2025, 11, 27));
        
        LocalDate minus3Days = DateUtils.plusDays(date, -3);
        assertThat(minus3Days).isEqualTo(LocalDate.of(2025, 11, 17));
    }
    
    @Test
    @DisplayName("计算两个日期之间的天数")
    void testDaysBetween() {
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 20);
        
        long days = DateUtils.daysBetween(start, end);
        assertThat(days).isEqualTo(19);
    }
    
    @Test
    @DisplayName("判断日期范围")
    void testDateRange() {
        LocalDate target = LocalDate.of(2025, 11, 15);
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 30);
        
        boolean inRange = DateUtils.isBetween(target, start, end);
        assertThat(inRange).isTrue();
        
        LocalDate outOfRange = LocalDate.of(2025, 12, 1);
        boolean notInRange = DateUtils.isBetween(outOfRange, start, end);
        assertThat(notInRange).isFalse();
    }
}
```

#### 测试JSON工具

```java
package com.example.util;

import io.nebula.core.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * JSON工具测试
 */
@DisplayName("JSON工具测试")
class JsonUtilsTest {
    
    @Test
    @DisplayName("对象转JSON")
    void testToJson() {
        User user = new User("1", "张三", 25);
        String json = JsonUtils.toJson(user);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("\"id\":\"1\"");
        assertThat(json).contains("\"name\":\"张三\"");
        assertThat(json).contains("\"age\":25");
    }
    
    @Test
    @DisplayName("JSON转对象")
    void testFromJson() {
        String json = "{\"id\":\"1\",\"name\":\"张三\",\"age\":25}";
        User user = JsonUtils.fromJson(json, User.class);
        
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("1");
        assertThat(user.getName()).isEqualTo("张三");
        assertThat(user.getAge()).isEqualTo(25);
    }
    
    @Test
    @DisplayName("JSON转Map")
    void testToMap() {
        String json = "{\"name\":\"张三\",\"age\":25}";
        Map<String, Object> map = JsonUtils.toMap(json);
        
        assertThat(map).isNotNull();
        assertThat(map.get("name")).isEqualTo("张三");
        assertThat(map.get("age")).isEqualTo(25);
    }
    
    @Test
    @DisplayName("JSON转List")
    void testToList() {
        String json = "[{\"id\":\"1\",\"name\":\"张三\"},{\"id\":\"2\",\"name\":\"李四\"}]";
        List<User> users = JsonUtils.toList(json, User.class);
        
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getName()).isEqualTo("张三");
        assertThat(users.get(1).getName()).isEqualTo("李四");
    }
    
    @Test
    @DisplayName("深拷贝")
    void testDeepCopy() {
        User original = new User("1", "张三", 25);
        User copy = JsonUtils.deepCopy(original, User.class);
        
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getName()).isEqualTo(original.getName());
        
        // 修改副本不影响原对象
        copy.setName("李四");
        assertThat(original.getName()).isEqualTo("张三");
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class User {
        private String id;
        private String name;
        private int age;
    }
}
```

---

## 集成测试

### Spring Boot集成测试

```java
package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * Spring Boot集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationIntegrationTest {
    
    @Test
    void contextLoads() {
        // 测试Spring上下文加载
    }
}
```

### 测试CommandLineRunner

```java
package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.*;

/**
 * CommandLineRunner测试
 */
@SpringBootTest
class CommandLineRunnerTest {
    
    @MockBean
    private CommandLineRunner runner;
    
    @Test
    void testRunner() throws Exception {
        String[] args = {"arg1", "arg2"};
        runner.run(args);
        
        verify(runner, times(1)).run(args);
    }
}
```

---

## CLI应用测试

### 测试CLI命令处理

```java
package com.example.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.*;

/**
 * CLI应用测试
 */
@SpringBootTest
@DisplayName("CLI应用测试")
class CliApplicationTest {
    
    @Test
    @DisplayName("测试生成ID命令")
    void testGenerateIdCommand() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        CliApplication app = new CliApplication();
        app.run("generate-id");
        
        String result = output.toString();
        assertThat(result).contains("生成的ID:");
    }
    
    @Test
    @DisplayName("测试无参数显示用法")
    void testNoArgsShowsUsage() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        
        CliApplication app = new CliApplication();
        app.run();
        
        String result = output.toString();
        assertThat(result).contains("用法:");
    }
    
    @Test
    @DisplayName("测试未知命令")
    void testUnknownCommand() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setErr(new PrintStream(output));
        
        CliApplication app = new CliApplication();
        app.run("unknown-command");
        
        String result = output.toString();
        assertThat(result).contains("未知命令");
    }
}
```

---

## 批处理任务测试

### 测试批处理服务

```java
package com.example.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 批处理服务测试
 */
@SpringBootTest
@DisplayName("批处理服务测试")
class BatchServiceTest {
    
    @Autowired
    private BatchService batchService;
    
    @Test
    @DisplayName("测试批量处理")
    void testProcessBatch() {
        List<String> dataList = Arrays.asList(
            "{\"name\":\"test1\"}",
            "{\"name\":\"test2\"}",
            "{\"name\":\"test3\"}"
        );
        
        assertThatCode(() -> batchService.processBatch(dataList))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("测试空数据批处理")
    void testProcessEmptyBatch() {
        List<String> emptyList = Arrays.asList();
        
        assertThatCode(() -> batchService.processBatch(emptyList))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("测试异常数据处理")
    void testProcessInvalidData() {
        List<String> invalidData = Arrays.asList(
            "invalid json",
            "{\"name\":\"valid\"}",
            "another invalid"
        );
        
        // 应该继续处理，不抛出异常
        assertThatCode(() -> batchService.processBatch(invalidData))
            .doesNotThrowAnyException();
    }
}
```

---

## 票务系统测试示例

### 场景1：票务数据导入测试

```java
package com.ticketsystem.import;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * 票务数据导入测试
 */
@SpringBootTest
@DisplayName("票务数据导入测试")
class TicketImportToolTest {
    
    @Autowired
    private TicketImportTool importTool;
    
    @Test
    @DisplayName("测试CSV文件导入")
    void testCsvImport(@TempDir Path tempDir) throws Exception {
        // 准备测试数据
        Path csvFile = tempDir.resolve("tickets.csv");
        List<String> lines = Arrays.asList(
            "movieName,cinemaName,showtime,seatNo,price",
            "阿凡达2,万达影城,2025-11-20 19:30,A01,50.00",
            "流浪地球3,CGV影城,2025-11-20 20:00,B02,45.00"
        );
        Files.write(csvFile, lines);
        
        // 执行导入
        importTool.run(csvFile.toString());
        
        // 验证输出文件
        Path outputFile = tempDir.resolve("tickets.json");
        assertThat(outputFile).exists();
        
        String content = Files.readString(outputFile);
        assertThat(content).contains("阿凡达2");
        assertThat(content).contains("流浪地球3");
    }
    
    @Test
    @DisplayName("测试空文件导入")
    void testEmptyFileImport(@TempDir Path tempDir) throws Exception {
        Path emptyFile = tempDir.resolve("empty.csv");
        Files.createFile(emptyFile);
        
        assertThatCode(() -> importTool.run(emptyFile.toString()))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("测试无效数据处理")
    void testInvalidDataImport(@TempDir Path tempDir) throws Exception {
        Path csvFile = tempDir.resolve("invalid.csv");
        List<String> lines = Arrays.asList(
            "movieName,cinemaName,showtime,seatNo,price",
            "电影1,影城1,invalid-date,A01,50.00",  // 无效日期
            "电影2,影城2,2025-11-20 19:30,B02,45.00"  // 有效数据
        );
        Files.write(csvFile, lines);
        
        // 应该跳过无效数据，继续处理
        assertThatCode(() -> importTool.run(csvFile.toString()))
            .doesNotThrowAnyException();
    }
}
```

### 场景2：报表生成测试

```java
package com.ticketsystem.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * 报表生成测试
 */
@SpringBootTest
@DisplayName("报表生成测试")
class ReportGeneratorTest {
    
    @Autowired
    private ReportGenerator generator;
    
    @Test
    @DisplayName("测试每日报表生成")
    void testGenerateDailyReport(@TempDir Path tempDir) throws Exception {
        // 执行报表生成
        generator.generateDailyReport();
        
        // 验证报表文件生成
        String filename = "report-" + DateUtils.formatDate(LocalDate.now().minusDays(1)) + ".txt";
        Path reportFile = tempDir.resolve(filename);
        
        // 注意：实际测试需要配置输出目录
        // assertThat(reportFile).exists();
    }
}
```

### 场景3：数据清理测试

```java
package com.ticketsystem.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据清理测试
 */
@SpringBootTest
@DisplayName("数据清理测试")
class DataCleanupToolTest {
    
    @Autowired
    private DataCleanupTool cleanupTool;
    
    @Test
    @DisplayName("测试数据清理执行")
    void testCleanupExecution() {
        assertThatCode(() -> cleanupTool.run())
            .doesNotThrowAnyException();
    }
}
```

---

## 测试覆盖率

### 生成测试覆盖率报告

在 `pom.xml` 中配置JaCoCo：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

运行测试并生成报告：

```bash
mvn clean test
mvn jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

---

## 最佳实践

### 实践1：使用@DisplayName

```java
@Test
@DisplayName("用户登录成功")
void testLoginSuccess() {
    // 测试代码
}
```

### 实践2：使用AssertJ进行断言

```java
assertThat(user.getName()).isEqualTo("张三");
assertThat(users).hasSize(3);
assertThat(result).isNotNull();
```

### 实践3：使用@TempDir处理临时文件

```java
@Test
void testFileProcessing(@TempDir Path tempDir) throws Exception {
    Path testFile = tempDir.resolve("test.txt");
    Files.writeString(testFile, "test content");
    // 测试代码
}
```

### 实践4：使用参数化测试

```java
@ParameterizedTest
@ValueSource(strings = {"hello", "world", "test"})
void testWithDifferentInputs(String input) {
    assertThat(input).isNotEmpty();
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置参考
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

