# Nebula Storage MinIO 模块

##  模块简介

`nebula-storage-minio` 是 Nebula 框架的对象存储模块，提供了基于 MinIO 的统一对象存储服务实现MinIO 是一个高性能S3 兼容的开源对象存储解决方案，非常适合存储非结构化数据，如图片视频文档等

##  功能特性

###  核心功能
- **文件上传**: 支持流式上传和字节数组上传，自动处理大文件
- **文件下载**: 支持流式下载，节省内存
- **文件删除**: 安全删除对象
- **文件复制**: 跨存储桶复制对象
- **文件列表**: 支持前缀过滤和分页查询
- **预签名 URL**: 生成临时访问 URL，安全分享文件
- **Bucket 管理**: 创建删除检查存储桶
- **元数据管理**: 支持自定义元数据和系统元数据

###  增强特性
- **自动配置**: Spring Boot 自动配置，零配置启动
- **连接池管理**: 集成 OkHttp 连接池，高性能
- **异常处理**: 统一的异常处理机制
- **健康检查**: 启动时自动测试连接
- **默认存储桶**: 自动创建默认存储桶
- **灵活配置**: 支持超时时间文件大小限制等配置

##  快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-storage-minio</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<!-- 如果使用自动配置 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-autoconfigure</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 基础配置

在 `application.yml` 中配置 MinIO：

```yaml
nebula:
  storage:
    minio:
      # 启用 MinIO 存储
      enabled: true
      # MinIO 服务器地址
      endpoint: http://localhost:9000
      # 访问密钥
      access-key: minioadmin
      # 秘密密钥
      secret-key: minioadmin
      # 默认存储桶
      default-bucket: nebula-files
      # 是否自动创建默认存储桶
      auto-create-default-bucket: true
      # 连接超时时间（毫秒）
      connect-timeout: 10000
      # 写超时时间（毫秒）
      write-timeout: 10000
      # 读超时时间（毫秒）
      read-timeout: 10000
      # 预签名 URL 默认过期时间（秒）
      default-expiry: 3600
      # 最大文件大小（字节，100MB）
      max-file-size: 104857600
```

##  基础功能

### 1. 文件上传

#### 1.1 使用输入流上传

```java
@Service
public class FileService {
    
    @Autowired
    private StorageService storageService;
    
    public String uploadFile(MultipartFile file) {
        try {
            // 构建元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            metadata.addUserMetadata("original-name", file.getOriginalFilename());
            metadata.addUserMetadata("upload-time", LocalDateTime.now().toString());
            
            // 上传文件
            StorageResult result = storageService.upload(
                "documents",
                "files/" + UUID.randomUUID() + "_" + file.getOriginalFilename(),
                file.getInputStream(),
                metadata
            );
            
            if (result.isSuccess()) {
                log.info("文件上传成功: bucket={}, key={}, etag={}", 
                        result.getBucket(), result.getKey(), result.getEtag());
                return result.getKey();
            } else {
                throw new RuntimeException("文件上传失败: " + result.getErrorMessage());
            }
            
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }
    }
}
```

#### 1.2 使用字节数组上传

```java
public void uploadFromBytes(String fileName, byte[] content, String contentType) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(contentType);
    metadata.setContentLength((long) content.length);
    
    StorageResult result = storageService.upload(
        "documents",
        "files/" + fileName,
        content,
        metadata
    );
    
    if (!result.isSuccess()) {
        throw new RuntimeException("上传失败: " + result.getErrorMessage());
    }
}
```

### 2. 文件下载

```java
public void downloadFile(String bucket, String key, HttpServletResponse response) {
    try {
        // 下载文件
        StorageResult result = storageService.download(bucket, key);
        
        if (!result.isSuccess()) {
            throw new RuntimeException("下载失败: " + result.getErrorMessage());
        }
        
        // 设置响应头
        ObjectMetadata metadata = result.getMetadata();
        response.setContentType(metadata.getContentType());
        response.setContentLengthLong(metadata.getContentLength());
        response.setHeader("Content-Disposition", 
            "attachment; filename=\"" + extractFileName(key) + "\"");
        
        // 写入响应流
        try (InputStream inputStream = result.getInputStream();
             OutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
        }
        
    } catch (IOException e) {
        throw new RuntimeException("下载文件失败", e);
    }
}

private String extractFileName(String key) {
    int lastSlash = key.lastIndexOf('/');
    return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
}
```

### 3. 文件列表

```java
public List<FileInfo> listFiles(String bucket, String prefix) {
    // 列出所有匹配前缀的文件
    List<ObjectSummary> objects = storageService.listObjects(bucket, prefix);
    
    return objects.stream()
        .filter(obj -> !obj.isDirectory())  // 过滤掉目录
        .map(obj -> {
            FileInfo info = new FileInfo();
            info.setFileName(obj.getFileName());
            info.setFileSize(obj.getSize());
            info.setLastModified(obj.getLastModified());
            info.setEtag(obj.getEtag());
            info.setKey(obj.getKey());
            return info;
        })
        .collect(Collectors.toList());
}
```

### 4. 生成预签名 URL

```java
public String generateDownloadUrl(String bucket, String key, Duration expiration) {
    // 生成有效期为指定时间的预签名 URL
    String url = storageService.generatePresignedUrl(bucket, key, expiration);
    
    log.info("生成预签名 URL: bucket={}, key={}, expiration={}", 
            bucket, key, expiration);
    
    return url;
}

// 示例：生成 7 天有效期的下载链接
public String shareFile(String bucket, String key) {
    return generateDownloadUrl(bucket, key, Duration.ofDays(7));
}
```

### 5. 文件删除

```java
public void deleteFile(String bucket, String key) {
    StorageResult result = storageService.delete(bucket, key);
    
    if (result.isSuccess()) {
        log.info("文件删除成功: bucket={}, key={}", bucket, key);
    } else {
        throw new RuntimeException("删除失败: " + result.getErrorMessage());
    }
}
```

### 6. 文件复制

```java
public void copyFile(String sourceBucket, String sourceKey, 
                     String targetBucket, String targetKey) {
    StorageResult result = storageService.copy(
        sourceBucket, sourceKey,
        targetBucket, targetKey
    );
    
    if (result.isSuccess()) {
        log.info("文件复制成功: {}:{} -> {}:{}", 
                sourceBucket, sourceKey, targetBucket, targetKey);
    } else {
        throw new RuntimeException("复制失败: " + result.getErrorMessage());
    }
}
```

##  高级特性

### Bucket 管理

```java
@Service
public class BucketService {
    
    @Autowired
    private StorageService storageService;
    
    // 检查 Bucket 是否存在
    public boolean checkBucket(String bucket) {
        return storageService.bucketExists(bucket);
    }
    
    // 创建 Bucket
    public void createBucket(String bucket) {
        if (!storageService.bucketExists(bucket)) {
            storageService.createBucket(bucket);
            log.info("创建存储桶: {}", bucket);
        } else {
            log.info("存储桶已存在: {}", bucket);
        }
    }
    
    // 删除 Bucket（注意：只能删除空桶）
    public void deleteBucket(String bucket) {
        storageService.deleteBucket(bucket);
        log.info("删除存储桶: {}", bucket);
    }
}
```

### 获取对象元数据

```java
public ObjectMetadata getFileMetadata(String bucket, String key) {
    ObjectMetadata metadata = storageService.getObjectMetadata(bucket, key);
    
    log.info("文件元数据: contentType={}, size={}, etag={}, lastModified={}",
            metadata.getContentType(),
            metadata.getContentLength(),
            metadata.getEtag(),
            metadata.getLastModified());
    
    return metadata;
}
```

### 检查对象是否存在

```java
public boolean fileExists(String bucket, String key) {
    return storageService.objectExists(bucket, key);
}
```

### 分页查询对象

```java
public List<ObjectSummary> listFilesPaged(String bucket, String prefix, 
                                          int maxKeys, String marker) {
    // marker 是上一页最后一个对象的 key，用于分页
    return storageService.listObjects(bucket, prefix, maxKeys, marker);
}
```

##  配置参数详解

### 必需配置

| 参数 | 说明 | 默认值 | 示例 |
|------|------|--------|------|
| `nebula.storage.minio.enabled` | 是否启用 MinIO | `false` | `true` |
| `nebula.storage.minio.endpoint` | MinIO 服务器地址 | `http://localhost:9000` | `http://minio.example.com:9000` |
| `nebula.storage.minio.access-key` | 访问密钥 | `minioadmin` | `your-access-key` |
| `nebula.storage.minio.secret-key` | 秘密密钥 | `minioadmin` | `your-secret-key` |

### 可选配置

| 参数 | 说明 | 默认值 | 范围 |
|------|------|--------|------|
| `nebula.storage.minio.default-bucket` | 默认存储桶名称 | `default` | 任意合法桶名 |
| `nebula.storage.minio.secure` | 是否使用 HTTPS | `false` | `true/false` |
| `nebula.storage.minio.region` | 区域 | `null` | 任意区域名 |
| `nebula.storage.minio.auto-create-default-bucket` | 是否自动创建默认桶 | `true` | `true/false` |
| `nebula.storage.minio.connect-timeout` | 连接超时（毫秒） | `10000` | >= 1000 |
| `nebula.storage.minio.write-timeout` | 写超时（毫秒） | `10000` | >= 1000 |
| `nebula.storage.minio.read-timeout` | 读超时（毫秒） | `10000` | >= 1000 |
| `nebula.storage.minio.default-expiry` | 预签名 URL 默认过期时间（秒） | `3600` | >= 60 |
| `nebula.storage.minio.max-file-size` | 最大文件大小（字节） | `104857600` (100MB) | >= 1024 |

### 完整配置示例

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: http://minio.example.com:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      default-bucket: nebula-files
      secure: false
      region: us-east-1
      auto-create-default-bucket: true
      connect-timeout: 15000
      write-timeout: 30000
      read-timeout: 30000
      default-expiry: 7200
      max-file-size: 209715200  # 200MB
      allowed-content-types:
        - image/jpeg
        - image/png
        - image/gif
        - application/pdf
        - text/plain
        - application/octet-stream
```

##  Docker 环境部署

### 使用 Docker Compose 启动 MinIO

```yaml
version: '3.8'
services:
  minio:
    image: minio/minio:latest
    container_name: nebula-minio
    ports:
      - "9000:9000"      # API 端口
      - "9001:9001"      # Console 端口
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

volumes:
  minio_data:
```

启动服务：

```bash
docker-compose up -d
```

访问 MinIO Console：`http://localhost:9001`

##  故障排查

### 常见问题

#### 1. 连接失败

**问题**: 无法连接到 MinIO 服务器

**排查步骤**:
- 检查 MinIO 服务是否启动：`docker ps | grep minio`
- 检查端口是否可访问：`telnet localhost 9000`
- 检查配置的 endpoint 是否正确
- 查看防火墙设置

#### 2. 认证失败

**问题**: `Authentication error` 或 `Access denied`

**解决方案**:
- 确认 `access-key` 和 `secret-key` 配置正确
- 检查 MinIO 用户权限配置
- 确认环境变量是否正确加载

#### 3. Bucket 不存在

**问题**: `Bucket not found` 错误

**解决方案**:
- 确认 bucket 名称拼写正确
- 启用 `auto-create-default-bucket` 自动创建
- 手动在 MinIO Console 中创建 bucket

#### 4. 上传失败

**问题**: 文件上传失败或超时

**排查步骤**:
- 检查文件大小是否超过 `max-file-size` 限制
- 增加 `write-timeout` 配置
- 检查磁盘空间是否充足
- 查看 MinIO 服务器日志

#### 5. 下载失败

**问题**: 文件下载失败或中断

**解决方案**:
- 确认文件是否存在：使用 `objectExists()` 方法
- 增加 `read-timeout` 配置
- 检查网络连接稳定性

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.storage: DEBUG
    io.minio: DEBUG
    okhttp3: DEBUG
```

### 健康检查

```java
@RestController
@RequestMapping("/admin/storage")
public class StorageHealthController {
    
    @Autowired
    private StorageService storageService;
    
    @GetMapping("/health")
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 测试连接：检查默认 bucket 是否存在
            boolean connected = storageService.bucketExists("nebula-files");
            health.put("status", "UP");
            health.put("minio", "connected");
            health.put("defaultBucket", connected ? "exists" : "not exists");
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}
```

##  最佳实践

### 1. 文件命名规范

```java
public class FileKeyGenerator {
    
    // 推荐：使用日期分层 + UUID + 原始文件名
    public static String generateKey(String category, String originalFilename) {
        LocalDate date = LocalDate.now();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        return String.format("%s/%d/%02d/%02d/%s_%s",
                category,
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                uuid,
                originalFilename);
    }
    
    // 示例：documents/2024/01/15/a1b2c3d4_report.pdf
}
```

### 2. 文件类型验证

```java
public void validateFile(MultipartFile file, long maxSize, Set<String> allowedTypes) {
    // 检查文件大小
    if (file.getSize() > maxSize) {
        throw new IllegalArgumentException(
            String.format("文件大小超过限制: %d bytes", maxSize));
    }
    
    // 检查文件类型
    String contentType = file.getContentType();
    if (contentType == null || !allowedTypes.contains(contentType)) {
        throw new IllegalArgumentException(
            String.format("不支持的文件类型: %s", contentType));
    }
    
    // 检查文件名
    String filename = file.getOriginalFilename();
    if (filename == null || filename.isEmpty()) {
        throw new IllegalArgumentException("文件名不能为空");
    }
}
```

### 3. 使用元数据

```java
public void uploadWithMetadata(MultipartFile file, Map<String, String> customMetadata) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());
    
    // 添加自定义元数据
    metadata.addUserMetadata("uploaded-by", getCurrentUserId());
    metadata.addUserMetadata("upload-time", Instant.now().toString());
    metadata.addUserMetadata("original-name", file.getOriginalFilename());
    
    // 添加业务元数据
    if (customMetadata != null) {
        customMetadata.forEach(metadata::addUserMetadata);
    }
    
    // 上传
    storageService.upload("documents", generateKey(), file.getInputStream(), metadata);
}
```

### 4. 异常处理

```java
@Service
public class SafeStorageService {
    
    @Autowired
    private StorageService storageService;
    
    public String uploadSafely(MultipartFile file) {
        try {
            // 验证文件
            validateFile(file);
            
            // 生成 key
            String key = generateKey("uploads", file.getOriginalFilename());
            
            // 上传文件
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            
            StorageResult result = storageService.upload(
                "documents", key, file.getInputStream(), metadata);
            
            if (!result.isSuccess()) {
                throw new StorageException(result.getErrorCode(), result.getErrorMessage());
            }
            
            return key;
            
        } catch (StorageException e) {
            log.error("存储异常: errorCode={}, message={}", e.getErrorCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("上传失败", e);
            throw new StorageException("UPLOAD_FAILED", "文件上传失败: " + e.getMessage(), e);
        }
    }
}
```

### 5. 分批处理大量文件

```java
public void processBatchFiles(String bucket, String prefix) {
    String marker = null;
    int batchSize = 100;
    
    do {
        // 分批查询
        List<ObjectSummary> batch = storageService.listObjects(
            bucket, prefix, batchSize, marker);
        
        if (batch.isEmpty()) {
            break;
        }
        
        // 处理这批文件
        for (ObjectSummary summary : batch) {
            processFile(summary);
        }
        
        // 更新 marker 为最后一个对象的 key
        marker = batch.get(batch.size() - 1).getKey();
        
    } while (marker != null);
}
```

##  测试指南

详细的功能测试指南请参考：[Nebula Storage 功能测试指南](../../../nebula-example/docs/nebula-storage-test.md)

##  更多资源

- [MinIO 官方文档](https://min.io/docs/minio/linux/index.html)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- [Nebula 框架使用指南](../../docs/Nebula框架使用指南.md)
- [完整示例项目](../../../nebula-example)

##  贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个模块

##  许可证

本项目基于 Apache 2.0 许可证开源

