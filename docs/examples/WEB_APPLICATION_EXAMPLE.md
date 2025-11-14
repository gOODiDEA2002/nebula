# Nebula Web 应用示例

本示例演示如何使用 `nebula-starter-web` 创建一个功能完整的 Web 应用。

## 项目结构

```
blog-api/
├── pom.xml
└── src/main/java/com/example/blog/
    ├── BlogApplication.java
    ├── config/
    │   └── SecurityConfig.java
    ├── controller/
    │   ├── ArticleController.java
    │   └── UserController.java
    ├── service/
    │   ├── ArticleService.java
    │   └── UserService.java
    ├── repository/
    │   ├── ArticleRepository.java
    │   └── UserRepository.java
    └── model/
        ├── Article.java
        └── User.java
```

## 1. 项目配置

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>blog-api</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Blog API</name>
    <description>博客 API 服务示例</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.12</version>
    </parent>

    <properties>
        <java.version>21</java.version>
        <nebula.version>2.0.0-SNAPSHOT</nebula.version>
    </properties>

    <dependencies>
        <!-- Nebula Web Starter -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-web</artifactId>
            <version>${nebula.version}</version>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### application.yml

```yaml
spring:
  application:
    name: blog-api

  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:your_password}
    driver-class-name: com.mysql.cj.jdbc.Driver

  # Redis 配置（用于缓存）
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0

server:
  port: 8080

# Nebula 配置
nebula:
  # 安全配置
  security:
    jwt:
      enabled: true
      secret-key: ${JWT_SECRET:your-secret-key-change-in-production}
      expiration: 86400000 # 24小时
    rbac:
      enabled: true

  # 缓存配置
  data:
    cache:
      enabled: true
      default-ttl: 3600
      caffeine:
        enabled: true
        max-size: 1000
      redis:
        enabled: true
        key-prefix: "blog:"

  # Web 配置
  web:
    response:
      unified-result: true
    exception:
      global-handler: true
    cors:
      enabled: true
      allowed-origins: "http://localhost:3000"
      allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    com.example.blog: DEBUG
    io.nebula: INFO
```

## 2. 实体类

### User.java

```java
package com.example.blog.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String password;
    
    private String email;
    
    private String nickname;
    
    private String avatar;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

### Article.java

```java
package com.example.blog.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("articles")
public class Article {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String content;
    
    private String summary;
    
    private String cover;
    
    private Long authorId;
    
    private String category;
    
    private String tags;
    
    private Integer viewCount;
    
    private Integer likeCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

## 3. Repository 层

### UserRepository.java

```java
package com.example.blog.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRepository extends BaseMapper<User> {
    // BaseMapper 已提供基础 CRUD，无需额外定义
}
```

### ArticleRepository.java

```java
package com.example.blog.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.model.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleRepository extends BaseMapper<Article> {
}
```

## 4. Service 层

### ArticleService.java

```java
package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.model.Article;
import com.example.blog.repository.ArticleRepository;
import io.nebula.data.cache.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CacheManager cacheManager;

    private static final String CACHE_KEY_PREFIX = "article:";

    /**
     * 创建文章
     */
    @Transactional
    public Article createArticle(Article article) {
        log.info("Creating article: {}", article.getTitle());
        articleRepository.insert(article);
        return article;
    }

    /**
     * 根据 ID 获取文章（带缓存）
     */
    public Optional<Article> getArticleById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 尝试从缓存获取
        Optional<Article> cached = cacheManager.get(cacheKey, Article.class);
        if (cached.isPresent()) {
            log.debug("Article found in cache: {}", id);
            return cached;
        }
        
        // 从数据库查询
        Article article = articleRepository.selectById(id);
        if (article != null) {
            // 写入缓存
            cacheManager.set(cacheKey, article, 3600);
        }
        
        return Optional.ofNullable(article);
    }

    /**
     * 分页查询文章
     */
    public Page<Article> listArticles(int pageNum, int pageSize, String category) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Article::getCategory, category);
        }
        
        wrapper.orderByDesc(Article::getCreateTime);
        return articleRepository.selectPage(page, wrapper);
    }

    /**
     * 更新文章
     */
    @Transactional
    public boolean updateArticle(Article article) {
        log.info("Updating article: {}", article.getId());
        boolean success = articleRepository.updateById(article) > 0;
        
        if (success) {
            // 清除缓存
            String cacheKey = CACHE_KEY_PREFIX + article.getId();
            cacheManager.delete(cacheKey);
        }
        
        return success;
    }

    /**
     * 删除文章（逻辑删除）
     */
    @Transactional
    public boolean deleteArticle(Long id) {
        log.info("Deleting article: {}", id);
        boolean success = articleRepository.deleteById(id) > 0;
        
        if (success) {
            // 清除缓存
            String cacheKey = CACHE_KEY_PREFIX + id;
            cacheManager.delete(cacheKey);
        }
        
        return success;
    }

    /**
     * 增加浏览次数
     */
    @Transactional
    public void incrementViewCount(Long id) {
        Article article = articleRepository.selectById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() + 1);
            articleRepository.updateById(article);
            
            // 更新缓存
            String cacheKey = CACHE_KEY_PREFIX + id;
            cacheManager.set(cacheKey, article, 3600);
        }
    }
}
```

## 5. Controller 层

### ArticleController.java

```java
package com.example.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.model.Article;
import com.example.blog.service.ArticleService;
import io.nebula.foundation.common.Result;
import io.nebula.web.controller.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Validated
public class ArticleController extends BaseController {

    private final ArticleService articleService;

    /**
     * 创建文章
     */
    @PostMapping
    public Result<Article> createArticle(@RequestBody @Validated Article article) {
        Article created = articleService.createArticle(article);
        return Result.success(created);
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{id}")
    public Result<Article> getArticle(@PathVariable @NotNull Long id) {
        // 增加浏览次数
        articleService.incrementViewCount(id);
        
        return articleService.getArticleById(id)
                .map(Result::success)
                .orElse(Result.error("文章不存在"));
    }

    /**
     * 分页查询文章
     */
    @GetMapping
    public Result<Page<Article>> listArticles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category) {
        
        Page<Article> page = articleService.listArticles(pageNum, pageSize, category);
        return Result.success(page);
    }

    /**
     * 更新文章
     */
    @PutMapping("/{id}")
    public Result<Void> updateArticle(
            @PathVariable Long id,
            @RequestBody @Validated Article article) {
        
        article.setId(id);
        boolean success = articleService.updateArticle(article);
        
        return success ? Result.success() : Result.error("更新失败");
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        boolean success = articleService.deleteArticle(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
```

## 6. 启动类

### BlogApplication.java

```java
package com.example.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
```

## 7. 数据库脚本

### schema.sql

```sql
CREATE DATABASE IF NOT EXISTS blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE blog_db;

-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文章表
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    summary VARCHAR(500),
    cover VARCHAR(255),
    author_id BIGINT NOT NULL,
    category VARCHAR(50),
    tags VARCHAR(200),
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_author_id (author_id),
    INDEX idx_category (category),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (author_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';
```

## 8. 运行应用

### 启动服务

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar target/blog-api-1.0.0.jar
```

### 测试 API

```bash
# 创建文章
curl -X POST http://localhost:8080/api/articles \
  -H "Content-Type: application/json" \
  -d '{
    "title": "我的第一篇博客",
    "content": "这是内容...",
    "summary": "这是摘要",
    "authorId": 1,
    "category": "技术",
    "tags": "Java,Spring Boot"
  }'

# 获取文章列表
curl http://localhost:8080/api/articles?pageNum=1&pageSize=10

# 获取文章详情
curl http://localhost:8080/api/articles/1

# 更新文章
curl -X PUT http://localhost:8080/api/articles/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "更新后的标题",
    "content": "更新后的内容..."
  }'

# 删除文章
curl -X DELETE http://localhost:8080/api/articles/1
```

## 9. 核心特性说明

### ✅ 自动配置
- **数据持久化**: MyBatis-Plus 自动配置，无需手动配置 Mapper
- **缓存**: Redis + Caffeine 多级缓存自动启用
- **Web**: 统一响应格式、全局异常处理、CORS 支持

### ✅ 统一响应格式
所有 API 返回 `Result<T>` 格式：
```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": { ... }
}
```

### ✅ 多级缓存
- **L1 缓存**: Caffeine (本地内存)
- **L2 缓存**: Redis (分布式)
- **自动失效**: 更新/删除时自动清除缓存

### ✅ 全局异常处理
框架自动捕获并处理异常，返回友好的错误信息。

## 10. 扩展功能

### 添加 JWT 认证

参考 `nebula-security` 模块文档，添加用户认证。

### 添加搜索功能

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-search-elasticsearch</artifactId>
</dependency>
```

### 添加文件上传

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-storage-minio</artifactId>
</dependency>
```

---

**完整代码**: 参见 `nebula-example` 项目  
**相关文档**: 
- [Nebula Web 模块文档](../nebula-web/README.md)
- [Nebula Cache 文档](../nebula-data-cache/README.md)
- [Nebula Security 文档](../nebula-security/README.md)

