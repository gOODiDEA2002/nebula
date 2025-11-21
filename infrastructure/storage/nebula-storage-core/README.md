# Nebula Storage Core

> 对象存储核心抽象模块

## 模块简介

`nebula-storage-core` 是 Nebula 框架的对象存储核心抽象模块，提供了统一的存储操作接口，屏蔽了底层存储服务（MinIO、阿里云OSS、AWS S3等）的差异，让开发者能够以统一的方式操作不同的对象存储服务。

### 核心特性

- **统一接口**：提供统一的存储操作API，无需关心底层实现
- **多存储支持**：支持多种对象存储服务（MinIO、OSS、S3等）
- **元数据管理**：完整的文件元数据管理
- **预签名URL**：生成临时访问URL
- **批量操作**：支持批量上传、下载、删除
- **分片上传**：大文件分片上传支持
- **权限控制**：灵活的存储权限控制

### 适用场景

- 文件存储服务
- 图片存储与CDN
- 视频存储与点播
- 文档管理系统
- 备份与归档
- 静态资源托管

---

## 快速开始

### 添加依赖

`nebula-storage-core` 是抽象模块，需要配合具体实现使用：

```xml
<!-- MinIO实现 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-storage-minio</artifactId>
    <version>${nebula.version}</version>
</dependency>

<!-- 或者 阿里云OSS实现 -->
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
    # 存储类型（minio、aliyun-oss、aws-s3）
    type: minio
    
    # 默认bucket
    default-bucket: nebula-storage
```

### 基础使用

```java
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final StorageService storageService;
    
    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String key = "files/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        StorageResult result = storageService.upload(
                "my-bucket", 
                key, 
                file.getInputStream(), 
                metadata
        );
        
        return result.isSuccess() ? key : null;
    }
    
    /**
     * 下载文件
     */
    public InputStream downloadFile(String key) {
        StorageResult result = storageService.download("my-bucket", key);
        return result.getInputStream();
    }
    
    /**
     * 生成临时访问URL
     */
    public String getPreviewUrl(String key) {
        return storageService.generatePresignedUrl(
                "my-bucket", 
                key, 
                Duration.ofHours(1)
        );
    }
}
```

---

## 核心组件

### 1. StorageService

统一的存储操作接口：

```java
public interface StorageService {
    
    /**
     * 上传文件
     */
    StorageResult upload(String bucket, String key, InputStream input, ObjectMetadata metadata);
    
    /**
     * 下载文件
     */
    StorageResult download(String bucket, String key);
    
    /**
     * 删除文件
     */
    StorageResult delete(String bucket, String key);
    
    /**
     * 批量删除文件
     */
    StorageResult deleteBatch(String bucket, List<String> keys);
    
    /**
     * 复制文件
     */
    StorageResult copy(String sourceBucket, String sourceKey, 
                      String targetBucket, String targetKey);
    
    /**
     * 列出对象
     */
    List<ObjectSummary> listObjects(String bucket, String prefix);
    
    /**
     * 获取对象元数据
     */
    ObjectMetadata getObjectMetadata(String bucket, String key);
    
    /**
     * 生成预签名URL
     */
    String generatePresignedUrl(String bucket, String key, Duration expiration);
    
    /**
     * 判断对象是否存在
     */
    boolean exists(String bucket, String key);
    
    /**
     * 创建Bucket
     */
    StorageResult createBucket(String bucket);
    
    /**
     * 删除Bucket
     */
    StorageResult deleteBucket(String bucket);
    
    /**
     * 列出所有Bucket
     */
    List<String> listBuckets();
}
```

### 2. StorageResult

操作结果封装：

```java
@Data
@Builder
public class StorageResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 对象Key
     */
    private String key;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 文件URL
     */
    private String url;
    
    /**
     * 输入流（用于下载）
     */
    private InputStream inputStream;
    
    /**
     * 元数据
     */
    private ObjectMetadata metadata;
}
```

### 3. ObjectMetadata

对象元数据：

```java
@Data
public class ObjectMetadata {
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 内容长度
     */
    private Long contentLength;
    
    /**
     * 内容编码
     */
    private String contentEncoding;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 用户自定义元数据
     */
    private Map<String, String> userMetadata;
}
```

### 4. ObjectSummary

对象摘要信息：

```java
@Data
@Builder
public class ObjectSummary {
    
    /**
     * Bucket名称
     */
    private String bucket;
    
    /**
     * 对象Key
     */
    private String key;
    
    /**
     * 文件大小
     */
    private Long size;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 存储类型
     */
    private String storageClass;
}
```

---

## 最佳实践

### 1. 文件命名规范

```java
/**
 * 文件Key命名规范
 */
public class StorageKeyGenerator {
    
    /**
     * 生成文件Key
     * 
     * 格式：{业务类型}/{日期}/{UUID}.{扩展名}
     * 示例：avatars/2024/01/15/550e8400-e29b-41d4-a716-446655440000.jpg
     */
    public static String generateKey(String businessType, String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();
        
        return String.format("%s/%s/%s.%s", businessType, datePath, uuid, extension);
    }
}
```

### 2. 文件类型验证

```java
/**
 * 文件类型验证
 */
@Service
public class FileValidator {
    
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    public void validateImageFile(MultipartFile file) {
        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("文件大小不能超过10MB");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ValidationException("不支持的文件类型");
        }
    }
}
```

### 3. 元数据管理

```java
/**
 * 元数据构建器
 */
public class MetadataBuilder {
    
    public static ObjectMetadata buildMetadata(MultipartFile file, String userId) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        // 自定义元数据
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("uploaded-by", userId);
        userMetadata.put("upload-time", LocalDateTime.now().toString());
        userMetadata.put("original-name", file.getOriginalFilename());
        
        metadata.setUserMetadata(userMetadata);
        
        return metadata;
    }
}
```

### 4. 异常处理

```java
/**
 * 存储服务包装器
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceWrapper {
    
    private final StorageService storageService;
    
    public String uploadWithRetry(String bucket, String key, 
                                  InputStream input, ObjectMetadata metadata) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                StorageResult result = storageService.upload(bucket, key, input, metadata);
                
                if (result.isSuccess()) {
                    return result.getKey();
                }
                
                log.warn("上传失败：{}", result.getMessage());
                attempt++;
                
            } catch (Exception e) {
                log.error("上传异常：attempt={}", attempt, e);
                attempt++;
                
                if (attempt >= maxRetries) {
                    throw new SystemException("文件上传失败");
                }
            }
        }
        
        throw new SystemException("文件上传失败");
    }
}
```

---

## 实现模块

### 可用实现

| 模块 | 存储服务 | 说明 |
|------|---------|------|
| `nebula-storage-minio` | MinIO | 开源对象存储，私有云首选 |
| `nebula-storage-aliyun-oss` | 阿里云OSS | 阿里云对象存储服务 |
| `nebula-storage-aws-s3` | AWS S3 | 亚马逊对象存储服务 |

### 如何选择

- **私有云部署**：选择 MinIO
- **阿里云环境**：选择阿里云OSS
- **AWS环境**：选择AWS S3
- **混合云**：可同时使用多个实现

---

## 扩展指南

### 实现自定义存储

1. 实现 `StorageService` 接口
2. 添加 `@Service` 注解
3. 实现所有接口方法

```java
@Service
@ConditionalOnProperty(name = "nebula.storage.type", havingValue = "custom")
public class CustomStorageServiceImpl implements StorageService {
    
    @Override
    public StorageResult upload(String bucket, String key, 
                               InputStream input, ObjectMetadata metadata) {
        // 实现上传逻辑
        return StorageResult.builder()
                .success(true)
                .key(key)
                .build();
    }
    
    // 实现其他接口方法...
}
```

---

## 相关文档

- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

## 技术栈

- **Spring Boot** 3.x
- **Java** 21+

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

