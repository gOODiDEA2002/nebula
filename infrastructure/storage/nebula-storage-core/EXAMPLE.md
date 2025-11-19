# nebula-storage-core 模块示例

## 模块简介

`nebula-storage-core` 模块定义了 Nebula 框架的对象存储核心抽象。它提供了一套统一的 API，用于文件的上传、下载、删除、复制和元数据管理，屏蔽了底层存储服务（如 MinIO, Aliyun OSS, AWS S3）的差异。

核心组件包括：
- **StorageService**: 统一的存储操作接口。
- **StorageResult**: 操作结果模型。
- **ObjectMetadata**: 对象元数据模型。
- **ObjectSummary**: 对象摘要信息。

## 核心功能示例

### 1. 上传文件

通过 `StorageService` 接口上传文件。

**`io.nebula.example.storage.service.FileService`**:

```java
package io.nebula.example.storage.service;

import io.nebula.storage.core.StorageService;
import io.nebula.storage.core.model.ObjectMetadata;
import io.nebula.storage.core.model.StorageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;

    /**
     * 上传文件到默认 bucket
     */
    public String uploadFile(MultipartFile file, String bucketName) throws IOException {
        String key = "uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        // 添加自定义元数据
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("author", "nebula-user");
        metadata.setUserMetadata(userMetadata);

        StorageResult result = storageService.upload(bucketName, key, file.getInputStream(), metadata);
        
        if (result.isSuccess()) {
            log.info("文件上传成功: {}", result.getKey());
            return result.getKey();
        } else {
            throw new RuntimeException("上传失败: " + result.getMessage());
        }
    }
}
```

### 2. 下载与预览

获取文件的流或预签名 URL。

**`io.nebula.example.storage.controller.FileController`**:

```java
package io.nebula.example.storage.controller;

import io.nebula.storage.core.StorageService;
import io.nebula.storage.core.model.StorageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;
    private final String defaultBucket = "my-bucket";

    @GetMapping("/download/{key}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String key) {
        StorageResult result = storageService.download(defaultBucket, key);
        
        if (!result.isSuccess()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(result.getInputStream()));
    }

    @GetMapping("/url/{key}")
    public String getPreviewUrl(@PathVariable String key) {
        // 生成一个 1 小时有效期的临时访问链接
        return storageService.generatePresignedUrl(defaultBucket, key, Duration.ofHours(1));
    }
}
```

### 3. 文件管理

列出、删除和复制文件。

```java
// 列出 'uploads/' 前缀的所有文件
List<ObjectSummary> files = storageService.listObjects(bucketName, "uploads/");

// 删除文件
storageService.delete(bucketName, "uploads/old-file.txt");

// 复制文件
storageService.copy(bucketName, "source.txt", bucketName, "backup/source.txt");
```

## 总结

`nebula-storage-core` 定义了对象存储的标准行为。在实际应用中，需要引入具体的实现模块（如 `nebula-storage-minio` 或 `nebula-storage-aliyun-oss`）来提供存储能力。

