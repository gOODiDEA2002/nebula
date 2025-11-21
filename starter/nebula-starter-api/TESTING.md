# Nebula Starter API - 测试指南

> API契约模块的测试指南，包括编译测试、契约测试和兼容性测试。

## 测试概览

- [编译测试](#编译测试)
- [契约测试](#契约测试)
- [兼容性测试](#兼容性测试)
- [文档生成测试](#文档生成测试)

---

## 编译测试

### 基础编译测试

```bash
# 编译检查
mvn clean compile

# 验证依赖
mvn dependency:tree

# 检查冲突
mvn dependency:analyze
```

### Maven测试配置

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.12.1</version>
            <configuration>
                <source>21</source>
                <target>21</target>
                <compilerArgs>
                    <arg>-Xlint:all</arg>
                    <arg>-Werror</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 契约测试

### 使用Spring Cloud Contract

添加依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-verifier</artifactId>
    <scope>test</scope>
</dependency>
```

定义契约 `contracts/user-service.groovy`:

```groovy
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method 'POST'
        url '/api/v1/user'
        body([
            username: 'john',
            email: 'john@example.com'
        ])
        headers {
            contentType('application/json')
        }
    }
    response {
        status 200
        body([
            userId: '123',
            username: 'john',
            message: 'User created successfully'
        ])
        headers {
            contentType('application/json')
        }
    }
}
```

测试契约：

```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.example:user-api:+:stubs",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class UserContractTest {
    
    @Test
    void testUserContract() {
        // 测试契约
    }
}
```

---

## 兼容性测试

### 向后兼容测试

```java
@Test
void testBackwardCompatibility() {
    // 1. 使用旧版本DTO
    CreateUserDtoV1.Request oldRequest = new CreateUserDtoV1.Request();
    oldRequest.setUsername("john");
    
    // 2. 转换为新版本
    CreateUserDtoV2.Request newRequest = convertToV2(oldRequest);
    
    // 3. 验证兼容性
    assertThat(newRequest.getUsername()).isEqualTo("john");
}
```

### 版本升级测试

```java
@Test
void testVersionUpgrade() {
    // 测试从V1到V2的升级
    UserRpcServiceV1 v1 = mock(UserRpcServiceV1.class);
    UserRpcServiceV2 v2 = (UserRpcServiceV2) v1;
    
    // 验证V2扩展的功能
    assertThat(v2).isNotNull();
}
```

---

## 文档生成测试

### 使用Spring REST Docs

```xml
<dependency>
    <groupId>org.springframework.restdocs</groupId>
    <artifactId>spring-restdocs-mockmvc</artifactId>
    <scope>test</scope>
</dependency>
```

生成文档：

```java
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class APIDocumentationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void documentCreateUser() throws Exception {
        mockMvc.perform(post("/api/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"john\",\"email\":\"john@example.com\"}"))
            .andExpect(status().isOk())
            .andDo(document("create-user"));
    }
}
```

---

## 票务系统测试示例

### API编译测试

```bash
cd ticket-api
mvn clean compile

# 验证所有API模块
mvn clean install
```

### 契约测试

```java
@SpringBootTest
class TicketAPIContractTest {
    
    @Test
    void testMovieServiceContract() {
        // 测试电影服务契约
    }
    
    @Test
    void testOrderServiceContract() {
        // 测试订单服务契约
    }
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

