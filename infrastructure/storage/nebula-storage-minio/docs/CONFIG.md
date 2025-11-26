# Nebula Storage MinIO 配置指南

> MinIO对象存储配置说明

## 概述

`nebula-storage-minio` 提供 MinIO 对象存储支持。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-storage-minio</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### MinIO配置

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: http://minio:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      # 默认bucket
      default-bucket: ticket-files
```

## 票务系统场景

### 电影海报存储

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: http://minio:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      buckets:
        - name: movie-posters
          policy: public-read
        - name: e-tickets
          policy: private
```

### 使用示例

```java
@Service
public class MoviePosterService {
    
    @Autowired
    private MinioClient minioClient;
    
    public String uploadPoster(MultipartFile file, Long movieId) {
        String objectName = "poster/" + movieId + "/" + file.getOriginalFilename();
        
        minioClient.putObject("movie-posters", objectName, 
            file.getInputStream(), file.getContentType());
        
        return minioClient.getPresignedUrl("movie-posters", objectName);
    }
}
```

---

**最后更新**: 2025-11-20

