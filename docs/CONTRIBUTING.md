# Nebula 框架贡献指南

> 感谢您对 Nebula 框架的关注！本文档将帮助您参与到项目中来。

## 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [文档规范](#文档规范)
- [测试规范](#测试规范)

## 行为准则

### 基本原则

1. **尊重他人**：尊重所有贡献者的意见和建议
2. **友好沟通**：使用友好、专业的语言
3. **建设性反馈**：提供具体、可操作的建议
4. **包容多样**：欢迎不同背景、经验的贡献者

### 不可接受的行为

- 使用攻击性或歧视性语言
- 发布他人的私人信息
- 其他不专业的行为

## 如何贡献

### 报告问题 (Issue)

发现问题时，请创建 Issue 并提供以下信息：

**Bug 报告模板**：
```markdown
## 问题描述
简要描述遇到的问题

## 复现步骤
1. 第一步
2. 第二步
3. ...

## 期望行为
应该发生什么

## 实际行为
实际发生了什么

## 环境信息
- Nebula 版本：
- JDK 版本：
- Spring Boot 版本：
- 操作系统：

## 相关日志
```java
// 粘贴相关日志
```

## 其他信息
其他有助于诊断问题的信息
```

**功能建议模板**：
```markdown
## 功能描述
简要描述建议的功能

## 使用场景
为什么需要这个功能？谁会使用它？

## 期望的 API 设计
```java
// 期望的 API 示例
```

## 替代方案
是否考虑过其他实现方式？

## 其他信息
其他补充信息
```

### 提交代码

#### 1. Fork 项目

点击页面右上角的 "Fork" 按钮，将项目复制到您的 GitHub 账号下。

#### 2. 克隆到本地

```bash
git clone https://github.com/YOUR_USERNAME/nebula.git
cd nebula
```

#### 3. 创建分支

```bash
# 创建功能分支
git checkout -b feature/your-feature-name

# 或创建修复分支
git checkout -b fix/issue-number-description
```

**分支命名规范**：
- `feature/xxx`：新功能
- `fix/xxx`：Bug 修复
- `docs/xxx`：文档更新
- `refactor/xxx`：代码重构
- `test/xxx`：测试相关
- `chore/xxx`：构建、配置等

#### 4. 进行开发

按照[开发流程](#开发流程)和[代码规范](#代码规范)进行开发。

#### 5. 提交代码

```bash
git add .
git commit -m "feat: add xxx feature"
```

提交信息需遵循[提交规范](#提交规范)。

#### 6. 推送到远程

```bash
git push origin feature/your-feature-name
```

#### 7. 创建 Pull Request

在 GitHub 上创建 Pull Request，并填写以下信息：

**PR 模板**：
```markdown
## 变更类型
- [ ] 新功能
- [ ] Bug 修复
- [ ] 文档更新
- [ ] 代码重构
- [ ] 测试
- [ ] 其他

## 变更描述
简要描述本次变更的内容和目的

## 关联 Issue
- Closes #xxx
- Relates to #xxx

## 测试情况
- [ ] 已添加单元测试
- [ ] 已添加集成测试
- [ ] 已手动测试

## 检查清单
- [ ] 代码遵循项目规范
- [ ] 已更新相关文档
- [ ] 已通过所有测试
- [ ] 提交信息符合规范
```

## 开发流程

### 1. 环境准备

**必需工具**：
- JDK 21+
- Maven 3.9+
- Git

**推荐工具**：
- IntelliJ IDEA
- Docker Desktop

**开发环境搭建**：

```bash
# 1. 克隆代码
git clone https://github.com/nebula/nebula.git
cd nebula

# 2. 构建项目
mvn clean install -DskipTests

# 3. 启动开发环境（可选）
cd nebula-data
docker-compose up -d
```

### 2. 开发步骤

1. **理解需求**：仔细阅读 Issue 或功能需求
2. **设计方案**：对于复杂功能，先设计方案
3. **编写代码**：遵循代码规范
4. **编写测试**：确保代码质量
5. **更新文档**：同步更新相关文档
6. **本地验证**：运行所有测试
7. **提交代码**：遵循提交规范

### 3. 代码审查

所有 PR 都需要通过代码审查才能合并：

**审查标准**：
- 代码质量
- 测试覆盖率
- 文档完整性
- 是否符合规范

**审查流程**：
1. 提交 PR
2. 自动检查（CI/CD）
3. 代码审查
4. 修改反馈
5. 批准合并

## 代码规范

### Java 代码规范

#### 命名规范

**类命名**：
```java
// 使用大驼峰命名法（PascalCase）
public class UserService { }
public class OrderController { }

// 接口使用 I 前缀（可选）
public interface IUserRepository { }

// 抽象类使用 Abstract 前缀
public abstract class AbstractCacheManager { }

// 异常类使用 Exception 后缀
public class BusinessException extends RuntimeException { }
```

**方法命名**：
```java
// 使用小驼峰命名法（camelCase）
public void getUserById() { }
public boolean isActive() { }
public User findByUsername() { }

// 布尔类型方法使用 is/has/can 等前缀
public boolean isValid() { }
public boolean hasPermission() { }
public boolean canAccess() { }
```

**变量命名**：
```java
// 使用小驼峰命名法
private String userName;
private int orderCount;

// 常量使用全大写 + 下划线
public static final String DEFAULT_ENCODING = "UTF-8";
public static final int MAX_RETRY_COUNT = 3;

// 集合类型变量使用复数形式
List<User> users;
Map<String, Order> orderMap;
```

#### 代码格式

**缩进**：使用 4 个空格，不使用 Tab

**行长度**：每行不超过 120 个字符

**导入顺序**：
```java
// 1. Java 标准库
import java.util.*;

// 2. 第三方库
import org.springframework.stereotype.Service;

// 3. 项目内部
import com.andy.nebula.foundation.Result;
```

**注释规范**：
```java
/**
 * 用户服务
 * 
 * @author andy
 * @since 1.0.0
 */
@Service
public class UserService {
    
    /**
     * 根据 ID 获取用户
     * 
     * @param id 用户 ID
     * @return 用户信息
     * @throws NotFoundException 用户不存在
     */
    public User getUserById(Long id) {
        // 实现逻辑
    }
}
```

#### 最佳实践

**使用 Lombok 简化代码**：
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
}
```

**使用 Optional 处理空值**：
```java
// 好的写法
public Optional<User> findById(Long id) {
    return Optional.ofNullable(userRepository.findById(id));
}

// 避免的写法
public User findById(Long id) {
    User user = userRepository.findById(id);
    if (user == null) {
        return null;  // 不推荐返回 null
    }
    return user;
}
```

**使用 Stream API**：
```java
// 好的写法
List<String> activeUserNames = users.stream()
    .filter(User::isActive)
    .map(User::getUsername)
    .collect(Collectors.toList());

// 避免的写法
List<String> activeUserNames = new ArrayList<>();
for (User user : users) {
    if (user.isActive()) {
        activeUserNames.add(user.getUsername());
    }
}
```

**异常处理**：
```java
// 好的写法
try {
    processOrder(order);
} catch (BusinessException e) {
    log.error("订单处理失败: orderId={}", order.getId(), e);
    throw e;
} catch (Exception e) {
    log.error("系统错误: orderId={}", order.getId(), e);
    throw new SystemException("订单处理失败", e);
}

// 避免的写法
try {
    processOrder(order);
} catch (Exception e) {
    e.printStackTrace();  // 不要使用 printStackTrace
}
```

**资源管理**：
```java
// 好的写法：使用 try-with-resources
try (InputStream is = new FileInputStream(file)) {
    // 处理流
}

// 避免的写法
InputStream is = null;
try {
    is = new FileInputStream(file);
    // 处理流
} finally {
    if (is != null) {
        is.close();
    }
}
```

## 提交规范

### Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型 (type)**：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档变更
- `style`: 代码格式（不影响代码运行的变动）
- `refactor`: 重构（既不是新增功能，也不是修改 bug 的代码变动）
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动
- `perf`: 性能优化

**范围 (scope)**：
- 模块名称，如 `data`, `messaging`, `rpc`, `security` 等

**主题 (subject)**：
- 简短描述，不超过 50 个字符
- 使用祈使句，如 "add" 而不是 "added"
- 首字母小写
- 结尾不加句号

**示例**：

```bash
# 新功能
git commit -m "feat(data): add multi-level cache support"

# Bug 修复
git commit -m "fix(security): resolve JWT token expiration issue"

# 文档
git commit -m "docs(rpc): update gRPC configuration guide"

# 重构
git commit -m "refactor(messaging): simplify message handler registration"

# 性能优化
git commit -m "perf(cache): optimize cache hit rate"
```

**完整示例**：
```
feat(data): add support for MongoDB transactions

Add transaction support for MongoDB operations:
- Implement TransactionTemplate for MongoDB
- Add @Transactional support
- Update documentation

Closes #123
```

## 文档规范

### 文档类型

每个模块需要提供以下文档：

1. **README.md**：模块介绍
2. **CONFIG.md**：配置说明
3. **EXAMPLE.md**：使用示例
4. **TESTING.md**：测试指南
5. **ROADMAP.md**：发展路线图
6. **API.md**：API 文档（如适用）

### 文档格式

**使用 Markdown**：
- 清晰的层级结构
- 使用代码块和语法高亮
- 提供完整的示例代码

**示例**：
````markdown
## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
</dependency>
```

### 配置

```yaml
nebula:
  data:
    persistence:
      enabled: true
```

### 使用

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```
````

## 测试规范

### 测试覆盖率

- 核心模块：≥ 80%
- 工具类：≥ 90%
- 配置类：≥ 60%

### 测试类型

**单元测试**：
```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void shouldReturnUserWhenIdExists() {
        // Given
        Long userId = 1L;
        
        // When
        User user = userService.findById(userId);
        
        // Then
        assertNotNull(user);
        assertEquals(userId, user.getId());
    }
}
```

**集成测试**：
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldReturnUserDetails() {
        // When
        ResponseEntity<User> response = restTemplate.getForEntity(
            "/api/users/1", 
            User.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
```

### 测试命名

使用 `shouldXxxWhenYyy` 格式：
- `shouldReturnUserWhenIdExists`
- `shouldThrowExceptionWhenUserNotFound`
- `shouldUpdateCacheWhenDataChanged`

## 版本发布

### 版本号规范

遵循语义化版本（Semantic Versioning）：

```
MAJOR.MINOR.PATCH
```

- **MAJOR**：不兼容的 API 变更
- **MINOR**：向下兼容的功能新增
- **PATCH**：向下兼容的问题修正

**示例**：
- `1.0.0`：首个正式版本
- `1.1.0`：新增功能
- `1.1.1`：Bug 修复
- `2.0.0`：重大变更

### 发布流程

1. 更新版本号
2. 更新 CHANGELOG
3. 创建 Git Tag
4. 发布到 Maven 仓库
5. 发布 Release Notes

## 获取帮助

如有疑问，可以通过以下方式获取帮助：

- **GitHub Issues**：报告问题或提出建议
- **GitHub Discussions**：技术讨论
- **官方文档**：查阅详细文档

## 致谢

感谢所有为 Nebula 框架做出贡献的开发者！

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

