# nebula-storage-minio 模块示例

## 模块简介

`nebula-storage-minio` 是 Nebula 框架基于 MinIO (兼容 AWS S3 协议) 实现的对象存储模块。它非常适合私有化部署场景，提供了与云厂商一致的对象存储体验。

## 核心功能示例

### 1. 配置 MinIO

在 `application.yml` 中配置 MinIO 连接信息。

**`application.yml`**:

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      default-bucket: public-files
      
      # 高级配置
      secure: false              # 是否使用 HTTPS
      region: us-east-1          # 区域 (可选)
      connect-timeout: 10000     # 连接超时 (ms)
      write-timeout: 10000       # 写入超时 (ms)
      read-timeout: 10000        # 读取超时 (ms)
      
      auto-create-default-bucket: true # 启动时自动创建 bucket
```

### 2. 启动应用

引入模块后，`StorageService` 接口会自动注入 MinIO 实现。

**`io.nebula.example.minio.StorageApplication`**:

```java
package io.nebula.example.minio;

import io.nebula.storage.core.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@SpringBootApplication
public class StorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageApplication.class, args);
    }

    @Bean
    public CommandLineRunner testMinio(StorageService storageService) {
        return args -> {
            String bucket = "public-files";
            String key = "hello.txt";
            String content = "Hello MinIO from Nebula Framework!";

            // 1. 检查并创建 Bucket
            if (!storageService.bucketExists(bucket)) {
                storageService.createBucket(bucket);
                log.info("Bucket {} created", bucket);
            }

            // 2. 上传文本文件
            storageService.upload(bucket, key, 
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), null);
            log.info("File {} uploaded", key);

            // 3. 生成访问链接
            String url = storageService.generatePresignedUrl(bucket, key, java.time.Duration.ofMinutes(5));
            log.info("Pre-signed URL: {}", url);
        };
    }
}
```

## 进阶特性

### 1. 兼容性

由于 MinIO 兼容 AWS S3 协议，本模块实际上也可以用于连接 AWS S3 或其他兼容 S3 的存储服务（如腾讯云 COS、华为云 OBS 等，只要配置 endpoint 正确）。

### 2. 策略配置

虽然模块提供了 `auto-create-default-bucket`，但生产环境中通常建议提前在 MinIO Console 或通过 Terraform 配置好 Bucket 策略（Public/Private），以确保安全性。

## 总结

`nebula-storage-minio` 是构建私有云存储方案的首选，它简单、轻量且功能强大，完全符合 `nebula-storage-core` 的标准接口。

