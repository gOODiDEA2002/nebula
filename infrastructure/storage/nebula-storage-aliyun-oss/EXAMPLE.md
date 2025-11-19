# nebula-storage-aliyun-oss 模块示例

## 模块简介

`nebula-storage-aliyun-oss` 是 Nebula 框架针对阿里云对象存储 (OSS) 的官方实现模块。它提供了稳定、高性能的云端存储接入能力。

## 核心功能示例

### 1. 配置 Aliyun OSS

在 `application.yml` 中配置 OSS 凭证。

**`application.yml`**:

```yaml
nebula:
  storage:
    aliyun:
      oss:
        enabled: true
        endpoint: oss-cn-hangzhou.aliyuncs.com
        access-key-id: ${ALIYUN_ACCESS_KEY_ID}
        access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
        default-bucket: my-app-assets
        
        # 连接配置
        connection-timeout: 50000
        max-connections: 1024
        max-error-retry: 3
        
        # 功能开关
        support-cname: false     # 是否支持 CNAME (自定义域名)
        auto-create-default-bucket: false # 建议生产环境关闭
```

### 2. 使用示例

使用方式与核心模块一致，只需注入 `StorageService`。

**`io.nebula.example.oss.ImageService`**:

```java
package io.nebula.example.oss;

import io.nebula.storage.core.StorageService;
import io.nebula.storage.core.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final StorageService storageService;
    private final String bucket = "my-app-assets";

    public String uploadImage(String imageName, InputStream stream, long size, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        
        // OSS 特有元数据可以通过 userMetadata 传递，或者框架内部自动处理标准头
        
        storageService.upload(bucket, "images/" + imageName, stream, metadata);
        
        // 如果配置了 CNAME，这里生成的 URL 可能是自定义域名的
        return storageService.generatePresignedUrl(bucket, "images/" + imageName, java.time.Duration.ofHours(24));
    }
}
```

## 进阶特性

### 1. 自定义域名 (CNAME)

如果在阿里云控制台绑定了自定义域名，可以开启 `support-cname` 并将 endpoint 配置为自定义域名。

```yaml
nebula:
  storage:
    aliyun:
      oss:
        endpoint: static.mydomain.com
        support-cname: true
```

### 2. STS 临时授权

本模块支持通过 `security-token` 配置项传入 STS Token，适用于在移动端或临时授权场景下使用。通常这需要动态配置或重新构建客户端，而在服务端场景中，通常使用长期 AccessKey。

## 总结

`nebula-storage-aliyun-oss` 提供了无缝接入阿里云 OSS 的能力，是公有云部署环境下的理想选择。

