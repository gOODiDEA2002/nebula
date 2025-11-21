# Nebula Storage Aliyun OSS

> 阿里云对象存储服务（OSS）集成模块

## 模块简介

`nebula-storage-aliyun-oss` 是 Nebula 框架针对阿里云对象存储（OSS）的官方实现模块，提供了稳定、高性能的云端存储接入能力。该模块实现了 `nebula-storage-core` 定义的统一存储接口，让开发者能够无缝使用阿里云OSS的强大功能。

### 核心特性

- **完整OSS支持**：支持阿里云OSS的所有基础功能
- **自定义域名**：支持CNAME绑定和自定义域名访问
- **STS临时授权**：支持STS临时访问凭证
- **图片处理**：支持OSS图片处理服务
- **CDN加速**：支持阿里云CDN加速访问
- **跨域访问**：支持CORS跨域配置
- **防盗链**：支持Referer白名单防盗链

### 适用场景

- 公有云部署环境
- 阿里云生态应用
- 需要CDN加速的场景
- 图片处理和缩略图生成
- 大规模文件存储
- 静态资源托管

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-storage-aliyun-oss</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  storage:
    type: aliyun-oss
    aliyun-oss:
      enabled: true
      
      # OSS访问端点（根据Bucket所在地域选择）
      endpoint: oss-cn-hangzhou.aliyuncs.com
      
      # 访问密钥
      access-key-id: ${ALIYUN_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
      
      # 默认Bucket
      default-bucket: my-app-storage
```

### 基础使用

```java
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final StorageService storageService;
    
    /**
     * 上传文件到阿里云OSS
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String key = "uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        StorageResult result = storageService.upload(
                "my-app-storage", 
                key, 
                file.getInputStream(), 
                metadata
        );
        
        return result.isSuccess() ? key : null;
    }
    
    /**
     * 生成临时访问URL
     */
    public String getFileUrl(String key) {
        return storageService.generatePresignedUrl(
                "my-app-storage", 
                key, 
                Duration.ofHours(1)
        );
    }
}
```

---

## OSS端点说明

### 地域端点

| 地域 | 外网端点 | 内网端点 |
|------|---------|---------|
| 华东1（杭州） | oss-cn-hangzhou.aliyuncs.com | oss-cn-hangzhou-internal.aliyuncs.com |
| 华东2（上海） | oss-cn-shanghai.aliyuncs.com | oss-cn-shanghai-internal.aliyuncs.com |
| 华北1（青岛） | oss-cn-qingdao.aliyuncs.com | oss-cn-qingdao-internal.aliyuncs.com |
| 华北2（北京） | oss-cn-beijing.aliyuncs.com | oss-cn-beijing-internal.aliyuncs.com |
| 华北3（张家口） | oss-cn-zhangjiakou.aliyuncs.com | oss-cn-zhangjiakou-internal.aliyuncs.com |
| 华南1（深圳） | oss-cn-shenzhen.aliyuncs.com | oss-cn-shenzhen-internal.aliyuncs.com |
| 西南1（成都） | oss-cn-chengdu.aliyuncs.com | oss-cn-chengdu-internal.aliyuncs.com |

> 提示：如果应用部署在阿里云ECS上，使用内网端点可以免流量费用。

---

## 高级特性

### 1. 自定义域名（CNAME）

如果已在阿里云控制台绑定自定义域名：

```yaml
nebula:
  storage:
    aliyun-oss:
      # 使用自定义域名作为端点
      endpoint: cdn.example.com
      
      # 启用CNAME支持
      support-cname: true
      
      # 是否使用HTTPS
      secure: true
```

### 2. STS临时授权

适用于移动端或临时授权场景：

```yaml
nebula:
  storage:
    aliyun-oss:
      # STS临时访问凭证
      security-token: ${ALIYUN_SECURITY_TOKEN}
      
      # 注意：使用STS时，access-key-id和access-key-secret是临时凭证
      access-key-id: ${ALIYUN_STS_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_STS_ACCESS_KEY_SECRET}
```

### 3. 图片处理

阿里云OSS提供强大的图片处理功能：

```java
@Service
@RequiredArgsConstructor
public class ImageProcessingService {
    
    private final StorageService storageService;
    
    /**
     * 获取缩略图URL
     */
    public String getThumbnailUrl(String imageKey, int width, int height) {
        String baseUrl = storageService.generatePresignedUrl(
                "my-bucket", 
                imageKey, 
                Duration.ofHours(1)
        );
        
        // 添加OSS图片处理参数
        String processParams = String.format("?x-oss-process=image/resize,m_fill,w_%d,h_%d", 
                width, height);
        
        return baseUrl + processParams;
    }
    
    /**
     * 获取水印图片URL
     */
    public String getWatermarkedUrl(String imageKey, String watermarkText) {
        String baseUrl = storageService.generatePresignedUrl(
                "my-bucket", 
                imageKey, 
                Duration.ofHours(1)
        );
        
        // Base64编码水印文本
        String encodedText = Base64.getEncoder().encodeToString(
                watermarkText.getBytes(StandardCharsets.UTF_8)
        );
        
        String processParams = String.format(
                "?x-oss-process=image/watermark,text_%s,type_d3F5LXplbmhlaQ,size_30,color_FFFFFF",
                encodedText
        );
        
        return baseUrl + processParams;
    }
}
```

### 4. CDN加速

配置CDN加速域名：

```yaml
nebula:
  storage:
    aliyun-oss:
      # CDN加速域名
      endpoint: cdn.example.com
      support-cname: true
      secure: true
      
      # CDN配置
      cdn:
        enabled: true
        # CDN刷新配置
        refresh-enabled: true
```

---

## 配置详解

### 完整配置项

```yaml
nebula:
  storage:
    type: aliyun-oss
    aliyun-oss:
      # 是否启用
      enabled: true
      
      # OSS端点
      endpoint: oss-cn-hangzhou.aliyuncs.com
      
      # 访问密钥
      access-key-id: ${ALIYUN_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
      
      # STS安全令牌（可选）
      security-token: ${ALIYUN_SECURITY_TOKEN:}
      
      # 默认Bucket
      default-bucket: my-app-storage
      
      # 连接配置
      connection-timeout: 50000  # 连接超时（毫秒）
      socket-timeout: 50000      # Socket超时（毫秒）
      max-connections: 1024      # 最大连接数
      max-error-retry: 3         # 最大重试次数
      
      # 自定义域名
      support-cname: false       # 是否支持CNAME
      
      # 是否使用HTTPS
      secure: true
      
      # 是否自动创建默认Bucket
      auto-create-default-bucket: false
      
      # 代理配置（可选）
      proxy:
        enabled: false
        host: proxy.example.com
        port: 8080
        username: ${PROXY_USERNAME:}
        password: ${PROXY_PASSWORD:}
```

---

## 最佳实践

### 1. 访问权限配置

- **私有读写**：默认推荐，通过预签名URL控制访问
- **公共读**：适用于静态资源（CSS、JS、图片）
- **公共读写**：强烈不推荐

### 2. Bucket命名规范

- 使用小写字母、数字和短横线
- 建议格式：`{公司}-{项目}-{环境}-{用途}`
- 示例：`acme-ticket-prod-assets`

### 3. 文件命名规范

- 使用目录结构组织文件
- 包含日期路径便于管理
- 使用UUID避免命名冲突
- 示例：`avatars/2024/01/15/550e8400-e29b-41d4-a716-446655440000.jpg`

### 4. 成本优化

- 使用内网端点节省流量费用
- 配置生命周期规则自动转换存储类型
- 删除不用的文件和碎片
- 启用CDN减少OSS访问次数

### 5. 安全建议

- 不要在代码中硬编码AccessKey
- 使用环境变量或配置中心管理密钥
- 定期轮换AccessKey
- 使用RAM子账号，遵循最小权限原则
- 启用防盗链和IP白名单

---

## 与MinIO对比

| 特性 | 阿里云OSS | MinIO |
|------|----------|-------|
| 部署方式 | 云服务 | 私有部署 |
| 运维成本 | 低（托管服务） | 高（自行运维） |
| 扩展性 | 自动扩展 | 手动扩展 |
| 成本 | 按量付费 | 硬件成本 |
| CDN | 原生支持 | 需要额外配置 |
| 图片处理 | 原生支持 | 需要额外服务 |
| 数据安全 | 多重备份 | 需要自行保障 |

**选择建议**：
- 公有云环境 → 阿里云OSS
- 私有云环境 → MinIO
- 混合云环境 → 可同时使用

---

## 故障排查

### 1. 连接超时

**问题**：`SocketTimeoutException: connect timed out`

**解决方案**：
- 检查endpoint配置是否正确
- 检查网络连接
- 增加连接超时时间

### 2. 权限错误

**问题**：`AccessDenied: Access denied`

**解决方案**：
- 检查AccessKey是否正确
- 检查RAM权限配置
- 检查Bucket访问权限

### 3. Bucket不存在

**问题**：`NoSuchBucket: The specified bucket does not exist`

**解决方案**：
- 确认Bucket名称正确
- 确认Bucket所在地域与endpoint匹配
- 手动创建Bucket或启用自动创建

---

## 相关文档

- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图
- [nebula-storage-core](../nebula-storage-core/README.md) - 核心抽象

---

## 技术栈

- **Spring Boot** 3.x
- **Java** 21+
- **Aliyun OSS SDK** 3.x

---

## 参考资料

- [阿里云OSS官方文档](https://help.aliyun.com/product/31815.html)
- [OSS Java SDK文档](https://help.aliyun.com/document_detail/32008.html)
- [OSS图片处理指南](https://help.aliyun.com/document_detail/44686.html)

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

