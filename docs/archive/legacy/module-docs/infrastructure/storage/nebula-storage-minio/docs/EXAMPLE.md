# Nebula Storage MinIO - 使用示例

> MinIO对象存储完整使用指南，以票务系统为例

## 目录

- [快速开始](#快速开始)
- [文件上传](#文件上传)
- [文件下载](#文件下载)
- [文件删除](#文件删除)
- [文件列表](#文件列表)
- [预签名URL](#预签名url)
- [Bucket管理](#bucket管理)
- [文件元数据](#文件元数据)
- [分片上传](#分片上传)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-storage-minio</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
nebula:
  storage:
    minio:
      enabled: true
      endpoint: http://localhost:9000  # MinIO服务地址
      access-key: minioadmin           # 访问密钥
      secret-key: minioadmin           # 密钥
      default-bucket: ticket-files     # 默认bucket
      secure: false                    # 是否HTTPS
      region: us-east-1                # 区域
      auto-create-default-bucket: true # 自动创建默认bucket
```

---

## 文件上传

### 1. 基础文件上传

```java
/**
 * 基础文件上传服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {
    
    private final StorageService storageService;
    
    /**
     * 上传文件（字节数组）
     */
    public String uploadFile(String fileName, byte[] content) {
        String bucket = "ticket-files";
        String objectKey = "uploads/" + fileName;
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            storageService.upload(bucket, objectKey, inputStream, null);
            
            log.info("文件上传成功：bucket={}, key={}", bucket, objectKey);
            
            // 返回文件URL
            return getFileUrl(bucket, objectKey);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
    
    /**
     * 上传文件（InputStream）
     */
    public String uploadFile(String fileName, InputStream inputStream, String contentType) {
        String bucket = "ticket-files";
        String objectKey = "uploads/" + fileName;
        
        Map<String, String> metadata = new HashMap<>();
        if (contentType != null) {
            metadata.put("Content-Type", contentType);
        }
        
        try {
            storageService.upload(bucket, objectKey, inputStream, metadata);
            
            log.info("文件上传成功：bucket={}, key={}, contentType={}", 
                    bucket, objectKey, contentType);
            
            return getFileUrl(bucket, objectKey);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
    
    /**
     * 上传文件（MultipartFile - Spring MVC）
     */
    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        String fileName = generateUniqueFileName(originalFilename);
        
        try {
            return uploadFile(fileName, file.getInputStream(), file.getContentType());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
    
    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return UUID.randomUUID().toString() + extension;
    }
    
    private String getFileUrl(String bucket, String objectKey) {
        return String.format("http://localhost:9000/%s/%s", bucket, objectKey);
    }
}
```

### 2. 上传用户头像

```java
/**
 * 用户头像上传服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAvatarService {
    
    private final StorageService storageService;
    private final ImageCompressionService imageCompressionService;
    
    /**
     * 上传用户头像
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        // 1. 验证文件类型
        validateImageFile(file);
        
        // 2. 压缩图片
        byte[] compressedImage = imageCompressionService.compress(file, 200, 200);
        
        // 3. 生成对象键
        String objectKey = String.format("avatars/%d/%s.jpg", 
                userId, System.currentTimeMillis());
        
        // 4. 上传到MinIO
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedImage)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Type", "image/jpeg");
            metadata.put("user-id", String.valueOf(userId));
            
            storageService.upload("user-files", objectKey, inputStream, metadata);
            
            log.info("用户头像上传成功：userId={}, key={}", userId, objectKey);
            
            return objectKey;
        } catch (IOException e) {
            log.error("用户头像上传失败", e);
            throw new BusinessException("头像上传失败");
        }
    }
    
    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) {  // 5MB
            throw new BusinessException("图片大小不能超过5MB");
        }
    }
}
```

### 3. 上传电子票二维码

```java
/**
 * 电子票二维码上传服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketQRCodeService {
    
    private final StorageService storageService;
    private final QRCodeGenerator qrCodeGenerator;
    
    /**
     * 生成并上传电子票二维码
     */
    public String generateAndUploadQRCode(String ticketNo) {
        // 1. 生成二维码
        byte[] qrCodeImage = qrCodeGenerator.generate(ticketNo, 300, 300);
        
        // 2. 上传到MinIO
        String objectKey = String.format("tickets/qrcodes/%s/%s.png", 
                ticketNo.substring(0, 8), ticketNo);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(qrCodeImage)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Type", "image/png");
            metadata.put("ticket-no", ticketNo);
            metadata.put("generated-time", LocalDateTime.now().toString());
            
            storageService.upload("ticket-files", objectKey, inputStream, metadata);
            
            log.info("电子票二维码上传成功：ticketNo={}, key={}", ticketNo, objectKey);
            
            return objectKey;
        } catch (IOException e) {
            log.error("二维码上传失败", e);
            throw new BusinessException("二维码生成失败");
        }
    }
}
```

---

## 文件下载

### 1. 基础文件下载

```java
/**
 * 文件下载服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileDownloadService {
    
    private final StorageService storageService;
    
    /**
     * 下载文件到字节数组
     */
    public byte[] downloadFileToBytes(String bucket, String objectKey) {
        try (InputStream inputStream = storageService.download(bucket, objectKey)) {
            log.info("文件下载成功：bucket={}, key={}", bucket, objectKey);
            
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }
    
    /**
     * 下载文件到本地
     */
    public void downloadFileToLocal(String bucket, String objectKey, String localPath) {
        try (InputStream inputStream = storageService.download(bucket, objectKey);
             FileOutputStream outputStream = new FileOutputStream(localPath)) {
            
            inputStream.transferTo(outputStream);
            
            log.info("文件下载到本地成功：bucket={}, key={}, localPath={}", 
                    bucket, objectKey, localPath);
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }
    
    /**
     * 下载文件到HTTP响应（Spring MVC）
     */
    public void downloadFileToResponse(String bucket, String objectKey, 
                                       HttpServletResponse response) {
        try (InputStream inputStream = storageService.download(bucket, objectKey)) {
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", 
                    "attachment; filename=\"" + objectKey + "\"");
            
            // 写入响应
            inputStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
            
            log.info("文件下载到响应成功：bucket={}, key={}", bucket, objectKey);
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }
}
```

### 2. 下载电子票二维码

```java
/**
 * 电子票二维码下载
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketQRCodeController {
    
    private final StorageService storageService;
    
    /**
     * 下载电子票二维码
     */
    @GetMapping("/{ticketNo}/qrcode")
    public void downloadQRCode(@PathVariable String ticketNo, HttpServletResponse response) {
        String bucket = "ticket-files";
        String objectKey = String.format("tickets/qrcodes/%s/%s.png", 
                ticketNo.substring(0, 8), ticketNo);
        
        try (InputStream inputStream = storageService.download(bucket, objectKey)) {
            // 设置响应头
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", 
                    String.format("inline; filename=\"%s-qrcode.png\"", ticketNo));
            
            // 写入响应
            inputStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
            
            log.info("二维码下载成功：ticketNo={}", ticketNo);
        } catch (IOException e) {
            log.error("二维码下载失败", e);
            throw new BusinessException("二维码下载失败");
        }
    }
}
```

---

## 文件删除

### 1. 删除单个文件

```java
/**
 * 文件删除服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileDeletionService {
    
    private final StorageService storageService;
    
    /**
     * 删除文件
     */
    public void deleteFile(String bucket, String objectKey) {
        try {
            storageService.delete(bucket, objectKey);
            
            log.info("文件删除成功：bucket={}, key={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new BusinessException("文件删除失败");
        }
    }
    
    /**
     * 删除用户头像
     */
    public void deleteUserAvatar(Long userId, String objectKey) {
        deleteFile("user-files", objectKey);
        
        log.info("用户头像已删除：userId={}, key={}", userId, objectKey);
    }
}
```

### 2. 批量删除文件

```java
/**
 * 批量删除文件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchFileDeletionService {
    
    private final StorageService storageService;
    
    /**
     * 批量删除文件
     */
    public void deleteFiles(String bucket, List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            try {
                storageService.delete(bucket, objectKey);
                log.info("文件删除成功：bucket={}, key={}", bucket, objectKey);
            } catch (Exception e) {
                log.error("文件删除失败：bucket={}, key={}", bucket, objectKey, e);
            }
        }
        
        log.info("批量删除完成：bucket={}, 共{}个文件", bucket, objectKeys.size());
    }
    
    /**
     * 删除指定前缀的所有文件
     */
    public void deleteByPrefix(String bucket, String prefix) {
        // 1. 列出所有文件
        List<String> objectKeys = storageService.listObjects(bucket, prefix);
        
        // 2. 批量删除
        deleteFiles(bucket, objectKeys);
        
        log.info("按前缀删除完成：bucket={}, prefix={}, 共{}个文件", 
                bucket, prefix, objectKeys.size());
    }
}
```

---

## 文件列表

```java
/**
 * 文件列表服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileListService {
    
    private final StorageService storageService;
    
    /**
     * 列出所有文件
     */
    public List<String> listAllFiles(String bucket) {
        List<String> objectKeys = storageService.listObjects(bucket, "");
        
        log.info("列出所有文件：bucket={}, 共{}个文件", bucket, objectKeys.size());
        
        return objectKeys;
    }
    
    /**
     * 列出指定前缀的文件
     */
    public List<String> listFilesByPrefix(String bucket, String prefix) {
        List<String> objectKeys = storageService.listObjects(bucket, prefix);
        
        log.info("列出文件：bucket={}, prefix={}, 共{}个文件", 
                bucket, prefix, objectKeys.size());
        
        return objectKeys;
    }
    
    /**
     * 列出用户的所有头像
     */
    public List<String> listUserAvatars(Long userId) {
        String prefix = "avatars/" + userId + "/";
        
        return listFilesByPrefix("user-files", prefix);
    }
    
    /**
     * 列出电子票二维码
     */
    public List<String> listTicketQRCodes(String ticketPrefix) {
        String prefix = "tickets/qrcodes/" + ticketPrefix;
        
        return listFilesByPrefix("ticket-files", prefix);
    }
}
```

---

## 预签名URL

### 1. 生成临时访问链接

```java
/**
 * 预签名URL服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresignedUrlService {
    
    private final StorageService storageService;
    
    /**
     * 生成下载预签名URL
     */
    public String generateDownloadUrl(String bucket, String objectKey, Duration expiration) {
        String url = storageService.generatePresignedUrl(bucket, objectKey, expiration);
        
        log.info("生成下载URL：bucket={}, key={}, expiration={}秒", 
                bucket, objectKey, expiration.getSeconds());
        
        return url;
    }
    
    /**
     * 生成电子票二维码临时访问链接（5分钟有效）
     */
    public String generateTicketQRCodeUrl(String ticketNo) {
        String bucket = "ticket-files";
        String objectKey = String.format("tickets/qrcodes/%s/%s.png", 
                ticketNo.substring(0, 8), ticketNo);
        
        return generateDownloadUrl(bucket, objectKey, Duration.ofMinutes(5));
    }
    
    /**
     * 生成用户头像访问链接（1小时有效）
     */
    public String generateUserAvatarUrl(String objectKey) {
        return generateDownloadUrl("user-files", objectKey, Duration.ofHours(1));
    }
}
```

### 2. 预签名URL用于前端直传

```java
/**
 * 前端直传服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DirectUploadService {
    
    private final StorageService storageService;
    
    /**
     * 生成上传预签名URL（用于前端直传）
     */
    public UploadCredentials generateUploadCredentials(String fileName, String contentType) {
        String bucket = "ticket-files";
        String objectKey = "uploads/" + UUID.randomUUID() + "/" + fileName;
        
        // 生成预签名上传URL（10分钟有效）
        String uploadUrl = storageService.generatePresignedUploadUrl(
                bucket, objectKey, Duration.ofMinutes(10));
        
        UploadCredentials credentials = new UploadCredentials();
        credentials.setUploadUrl(uploadUrl);
        credentials.setObjectKey(objectKey);
        credentials.setExpiration(LocalDateTime.now().plusMinutes(10));
        
        log.info("生成上传凭证：objectKey={}", objectKey);
        
        return credentials;
    }
}

/**
 * 上传凭证
 */
@Data
public class UploadCredentials {
    private String uploadUrl;
    private String objectKey;
    private LocalDateTime expiration;
}
```

---

## Bucket管理

```java
/**
 * Bucket管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BucketManagementService {
    
    private final StorageService storageService;
    
    /**
     * 创建Bucket
     */
    public void createBucket(String bucket) {
        if (!storageService.bucketExists(bucket)) {
            storageService.createBucket(bucket);
            log.info("Bucket创建成功：{}", bucket);
        } else {
            log.info("Bucket已存在：{}", bucket);
        }
    }
    
    /**
     * 删除Bucket
     */
    public void deleteBucket(String bucket) {
        if (storageService.bucketExists(bucket)) {
            // 先删除Bucket中的所有文件
            List<String> objectKeys = storageService.listObjects(bucket, "");
            for (String objectKey : objectKeys) {
                storageService.delete(bucket, objectKey);
            }
            
            // 删除Bucket
            storageService.deleteBucket(bucket);
            
            log.info("Bucket删除成功：{}", bucket);
        }
    }
    
    /**
     * 检查Bucket是否存在
     */
    public boolean bucketExists(String bucket) {
        return storageService.bucketExists(bucket);
    }
    
    /**
     * 初始化票务系统所需的所有Bucket
     */
    public void initializeTicketingBuckets() {
        createBucket("ticket-files");     // 电子票文件
        createBucket("user-files");       // 用户文件（头像等）
        createBucket("showtime-files");   // 演出文件（海报、宣传图等）
        createBucket("temp-files");       // 临时文件
        
        log.info("票务系统Bucket初始化完成");
    }
}
```

---

## 文件元数据

```java
/**
 * 文件元数据服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileMetadataService {
    
    private final StorageService storageService;
    
    /**
     * 上传文件并设置元数据
     */
    public String uploadWithMetadata(String bucket, String objectKey, 
                                    InputStream inputStream, 
                                    Map<String, String> customMetadata) {
        Map<String, String> metadata = new HashMap<>();
        
        // 添加自定义元数据
        if (customMetadata != null) {
            metadata.putAll(customMetadata);
        }
        
        // 添加系统元数据
        metadata.put("upload-time", LocalDateTime.now().toString());
        metadata.put("upload-user", SecurityContext.getCurrentUserId().toString());
        
        storageService.upload(bucket, objectKey, inputStream, metadata);
        
        log.info("文件上传成功（带元数据）：bucket={}, key={}, metadata={}", 
                bucket, objectKey, metadata);
        
        return objectKey;
    }
    
    /**
     * 获取文件元数据
     */
    public Map<String, String> getFileMetadata(String bucket, String objectKey) {
        ObjectInfo info = storageService.getObjectInfo(bucket, objectKey);
        
        log.info("获取文件元数据：bucket={}, key={}", bucket, objectKey);
        
        return info.getMetadata();
    }
    
    /**
     * 获取文件信息
     */
    public FileInfo getFileInfo(String bucket, String objectKey) {
        ObjectInfo info = storageService.getObjectInfo(bucket, objectKey);
        
        FileInfo fileInfo = new FileInfo();
        fileInfo.setObjectKey(objectKey);
        fileInfo.setSize(info.getSize());
        fileInfo.setLastModified(info.getLastModified());
        fileInfo.setContentType(info.getContentType());
        fileInfo.setMetadata(info.getMetadata());
        
        return fileInfo;
    }
}

/**
 * 文件信息VO
 */
@Data
public class FileInfo {
    private String objectKey;
    private Long size;
    private LocalDateTime lastModified;
    private String contentType;
    private Map<String, String> metadata;
}
```

---

## 分片上传

```java
/**
 * 分片上传服务（用于大文件上传）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultipartUploadService {
    
    private final StorageService storageService;
    
    /**
     * 分片上传大文件
     */
    public String uploadLargeFile(String bucket, String objectKey, File file) {
        long fileSize = file.length();
        long partSize = 5 * 1024 * 1024; // 5MB per part
        int partCount = (int) Math.ceil((double) fileSize / partSize);
        
        log.info("开始分片上传：bucket={}, key={}, fileSize={}, partCount={}", 
                bucket, objectKey, fileSize, partCount);
        
        try (FileInputStream inputStream = new FileInputStream(file)) {
            // 1. 初始化分片上传
            String uploadId = storageService.initiateMultipartUpload(bucket, objectKey);
            
            // 2. 上传分片
            List<PartETag> partETags = new ArrayList<>();
            byte[] buffer = new byte[(int) partSize];
            
            for (int partNumber = 1; partNumber <= partCount; partNumber++) {
                int bytesRead = inputStream.read(buffer);
                
                ByteArrayInputStream partStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                
                PartETag partETag = storageService.uploadPart(
                        bucket, objectKey, uploadId, partNumber, partStream, bytesRead);
                
                partETags.add(partETag);
                
                log.info("上传分片：part={}/{}", partNumber, partCount);
            }
            
            // 3. 完成分片上传
            storageService.completeMultipartUpload(bucket, objectKey, uploadId, partETags);
            
            log.info("分片上传完成：bucket={}, key={}", bucket, objectKey);
            
            return objectKey;
        } catch (IOException e) {
            log.error("分片上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
}
```

---

## 票务系统完整示例

### 完整的文件存储服务

```java
/**
 * 票务系统文件存储服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingStorageService {
    
    private final StorageService storageService;
    private final QRCodeGenerator qrCodeGenerator;
    private final ImageCompressionService imageCompressionService;
    
    /**
     * 1. 上传演出海报
     */
    public String uploadShowtimePoster(Long showtimeId, MultipartFile file) {
        // 验证图片
        validateImageFile(file);
        
        // 压缩图片
        byte[] compressedImage = imageCompressionService.compress(file, 800, 600);
        
        // 上传
        String objectKey = String.format("showtimes/%d/posters/%s.jpg", 
                showtimeId, System.currentTimeMillis());
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedImage)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Type", "image/jpeg");
            metadata.put("showtime-id", String.valueOf(showtimeId));
            metadata.put("file-type", "poster");
            
            storageService.upload("showtime-files", objectKey, inputStream, metadata);
            
            log.info("演出海报上传成功：showtimeId={}, key={}", showtimeId, objectKey);
            
            return objectKey;
        } catch (IOException e) {
            log.error("演出海报上传失败", e);
            throw new BusinessException("海报上传失败");
        }
    }
    
    /**
     * 2. 生成并上传电子票
     */
    public TicketFile generateTicketFile(Ticket ticket) {
        // 2.1 生成二维码
        String qrCodeKey = generateAndUploadQRCode(ticket.getTicketNo());
        
        // 2.2 生成电子票PDF（包含二维码、订单信息等）
        String pdfKey = generateAndUploadTicketPDF(ticket, qrCodeKey);
        
        TicketFile ticketFile = new TicketFile();
        ticketFile.setTicketNo(ticket.getTicketNo());
        ticketFile.setQrCodeKey(qrCodeKey);
        ticketFile.setPdfKey(pdfKey);
        
        log.info("电子票文件生成完成：ticketNo={}", ticket.getTicketNo());
        
        return ticketFile;
    }
    
    /**
     * 生成并上传二维码
     */
    private String generateAndUploadQRCode(String ticketNo) {
        // 生成二维码
        byte[] qrCodeImage = qrCodeGenerator.generate(ticketNo, 300, 300);
        
        // 上传
        String objectKey = String.format("tickets/qrcodes/%s/%s.png", 
                ticketNo.substring(0, 8), ticketNo);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(qrCodeImage)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Type", "image/png");
            metadata.put("ticket-no", ticketNo);
            
            storageService.upload("ticket-files", objectKey, inputStream, metadata);
            
            return objectKey;
        } catch (IOException e) {
            throw new BusinessException("二维码生成失败");
        }
    }
    
    /**
     * 生成并上传电子票PDF
     */
    private String generateAndUploadTicketPDF(Ticket ticket, String qrCodeKey) {
        // 1. 下载二维码
        byte[] qrCode = downloadFile("ticket-files", qrCodeKey);
        
        // 2. 生成PDF
        byte[] pdfBytes = pdfGenerator.generateTicketPDF(ticket, qrCode);
        
        // 3. 上传PDF
        String objectKey = String.format("tickets/pdfs/%s/%s.pdf", 
                ticket.getTicketNo().substring(0, 8), ticket.getTicketNo());
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Type", "application/pdf");
            metadata.put("ticket-no", ticket.getTicketNo());
            
            storageService.upload("ticket-files", objectKey, inputStream, metadata);
            
            return objectKey;
        } catch (IOException e) {
            throw new BusinessException("电子票PDF生成失败");
        }
    }
    
    /**
     * 3. 上传用户头像
     */
    public String uploadUserAvatar(Long userId, MultipartFile file) {
        // 验证和压缩
        validateImageFile(file);
        byte[] compressedImage = imageCompressionService.compress(file, 200, 200);
        
        // 上传
        String objectKey = String.format("avatars/%d/%s.jpg", 
                userId, System.currentTimeMillis());
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedImage)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Type", "image/jpeg");
            metadata.put("user-id", String.valueOf(userId));
            
            storageService.upload("user-files", objectKey, inputStream, metadata);
            
            log.info("用户头像上传成功：userId={}, key={}", userId, objectKey);
            
            return objectKey;
        } catch (IOException e) {
            throw new BusinessException("头像上传失败");
        }
    }
    
    /**
     * 4. 获取电子票下载链接
     */
    public TicketDownloadUrls getTicketDownloadUrls(String ticketNo) {
        String qrCodeKey = String.format("tickets/qrcodes/%s/%s.png", 
                ticketNo.substring(0, 8), ticketNo);
        String pdfKey = String.format("tickets/pdfs/%s/%s.pdf", 
                ticketNo.substring(0, 8), ticketNo);
        
        // 生成5分钟有效的下载链接
        String qrCodeUrl = storageService.generatePresignedUrl(
                "ticket-files", qrCodeKey, Duration.ofMinutes(5));
        String pdfUrl = storageService.generatePresignedUrl(
                "ticket-files", pdfKey, Duration.ofMinutes(5));
        
        TicketDownloadUrls urls = new TicketDownloadUrls();
        urls.setQrCodeUrl(qrCodeUrl);
        urls.setPdfUrl(pdfUrl);
        urls.setExpiration(LocalDateTime.now().plusMinutes(5));
        
        return urls;
    }
    
    /**
     * 5. 清理过期临时文件（定时任务）
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void cleanupExpiredTempFiles() {
        log.info("开始清理过期临时文件");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        
        // 列出临时文件
        List<String> tempFiles = storageService.listObjects("temp-files", "");
        
        int deletedCount = 0;
        for (String objectKey : tempFiles) {
            ObjectInfo info = storageService.getObjectInfo("temp-files", objectKey);
            
            // 删除7天前的文件
            if (info.getLastModified().isBefore(cutoffTime)) {
                storageService.delete("temp-files", objectKey);
                deletedCount++;
            }
        }
        
        log.info("清理过期临时文件完成，共删除{}个文件", deletedCount);
    }
    
    // 辅助方法
    
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过5MB");
        }
    }
    
    private byte[] downloadFile(String bucket, String objectKey) {
        try (InputStream inputStream = storageService.download(bucket, objectKey)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException("文件下载失败");
        }
    }
}

/**
 * 电子票文件
 */
@Data
public class TicketFile {
    private String ticketNo;
    private String qrCodeKey;
    private String pdfKey;
}

/**
 * 电子票下载链接
 */
@Data
public class TicketDownloadUrls {
    private String qrCodeUrl;
    private String pdfUrl;
    private LocalDateTime expiration;
}
```

---

## 最佳实践

### 1. Bucket划分

- **按业务划分**：`user-files`、`ticket-files`、`showtime-files`
- **按权限划分**：`public-files`、`private-files`
- **按生命周期划分**：`temp-files`（临时）、`archive-files`（归档）

### 2. 文件命名规范

- **使用UUID**：避免文件名冲突
- **添加时间戳**：方便按时间排序
- **添加业务标识**：如用户ID、订单号
- **示例**：`avatars/123456/1638345600000-abc123.jpg`

### 3. 文件大小限制

- **头像**：最大5MB
- **海报**：最大10MB
- **文档**：最大50MB
- **使用分片上传**：超过100MB的文件

### 4. 文件压缩

- **图片压缩**：上传前压缩到合适的尺寸
- **质量控制**：JPEG质量设置为80-90
- **格式转换**：PNG转JPEG减小文件大小

### 5. 文件安全

- **预签名URL**：使用临时链接而非直接暴露文件URL
- **访问控制**：设置Bucket的访问策略
- **敏感文件加密**：对敏感文件进行加密存储

### 6. 性能优化

- **CDN加速**：静态文件使用CDN
- **缓存策略**：设置合理的缓存过期时间
- **分片上传**：大文件使用分片上传
- **并发控制**：限制并发上传数量

### 7. 监控和维护

- **存储空间监控**：监控各Bucket的存储使用情况
- **上传成功率监控**：监控文件上传成功率
- **定期清理**：清理过期的临时文件
- **备份策略**：重要文件定期备份

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
