# Nebula Starter Web

Web应用专用Starter，适用于REST API、管理后台等Web应用。

## 适用场景

- 🌐 REST API服务
- 📊 Admin管理后台
- 💼 企业级Web应用
- 🏪 电商平台后端

## 包含模块

- `nebula-foundation` - 基础工具
- `nebula-web` - Web框架
- `nebula-data-cache` - 多级缓存
- `nebula-data-persistence` - 数据库访问(可选)
- `nebula-security` - 安全认证(可选)

## 功能特性

- ✅ REST API支持
- ✅ JWT认证
- ✅ 统一异常处理
- ✅ 参数验证
- ✅ 限流保护
- ✅ 多级缓存
- ✅ 监控(Actuator)

## 内存占用

**~400MB**

## 快速开始

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
    <version>2.1.0-SNAPSHOT</version>
</dependency>
```

```java
@SpringBootApplication
public class WebApp {
    public static void main(String[] args) {
        SpringApplication.run(WebApp.class, args);
    }
}

@RestController
@RequestMapping("/api")
public class MyController extends BaseController {
    
    @GetMapping("/hello")
    public Result<String> hello() {
        return success("Hello Nebula Web!");
    }
}
```

详见: [Nebula Web模块文档](../../application/nebula-web/README.md)

---

**版本**: 2.1.0-SNAPSHOT
