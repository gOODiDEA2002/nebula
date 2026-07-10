# Nebula Storage Core - 测试指南

> 对象存储核心抽象模块的测试策略与实践

## 目录

- [测试策略](#测试策略)
- [单元测试](#单元测试)
- [集成测试](#集成测试)
- [Mock测试](#mock测试)
- [性能测试](#性能测试)
- [测试工具](#测试工具)

---

## 测试策略

### 测试层次

由于 `nebula-storage-core` 是抽象模块，测试主要分为两个层面：

1. **接口契约测试**：确保接口定义正确
2. **实现测试**：在具体实现模块中测试（如 `nebula-storage-minio`）

### 测试覆盖目标

- 接口定义完整性：100%
- 模型类正确性：100%
- 异常处理：100%

---

## 单元测试

### 1. 模型类测试

测试 `StorageResult`、`ObjectMetadata`、`ObjectSummary` 等模型类：

```java
/**
 * StorageResult 测试
 */
class StorageResultTest {
    
    @Test
    void testBuilderPattern() {
        // 测试Builder模式
        StorageResult result = StorageResult.builder()
                .success(true)
                .key("test.txt")
                .etag("abc123")
                .url("http://example.com/test.txt")
                .build();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo("test.txt");
        assertThat(result.getEtag()).isEqualTo("abc123");
        assertThat(result.getUrl()).isEqualTo("http://example.com/test.txt");
    }
    
    @Test
    void testSuccessResult() {
        StorageResult result = StorageResult.success("uploaded-key", "etag-123");
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo("uploaded-key");
        assertThat(result.getEtag()).isEqualTo("etag-123");
    }
    
    @Test
    void testFailureResult() {
        StorageResult result = StorageResult.failure("Upload failed");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Upload failed");
    }
}
```

### 2. 元数据测试

```java
/**
 * ObjectMetadata 测试
 */
class ObjectMetadataTest {
    
    @Test
    void testMetadataFields() {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");
        metadata.setContentLength(1024L);
        metadata.setContentEncoding("utf-8");
        
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("author", "test-user");
        metadata.setUserMetadata(userMetadata);
        
        assertThat(metadata.getContentType()).isEqualTo("image/jpeg");
        assertThat(metadata.getContentLength()).isEqualTo(1024L);
        assertThat(metadata.getUserMetadata()).containsEntry("author", "test-user");
    }
    
    @Test
    void testUserMetadataIsolation() {
        ObjectMetadata metadata = new ObjectMetadata();
        
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("key1", "value1");
        metadata.setUserMetadata(userMetadata);
        
        // 修改原Map不应影响metadata
        userMetadata.put("key2", "value2");
        
        assertThat(metadata.getUserMetadata()).hasSize(1);
    }
}
```

### 3. 对象摘要测试

```java
/**
 * ObjectSummary 测试
 */
class ObjectSummaryTest {
    
    @Test
    void testObjectSummaryBuilder() {
        LocalDateTime now = LocalDateTime.now();
        
        ObjectSummary summary = ObjectSummary.builder()
                .bucket("test-bucket")
                .key("test.txt")
                .size(1024L)
                .etag("abc123")
                .lastModified(now)
                .storageClass("STANDARD")
                .build();
        
        assertThat(summary.getBucket()).isEqualTo("test-bucket");
        assertThat(summary.getKey()).isEqualTo("test.txt");
        assertThat(summary.getSize()).isEqualTo(1024L);
        assertThat(summary.getLastModified()).isEqualTo(now);
    }
}
```

---

## 集成测试

### 1. 接口契约测试

定义抽象测试基类，让实现模块继承：

```java
/**
 * StorageService 接口契约测试基类
 */
public abstract class StorageServiceContractTest {
    
    /**
     * 子类需要提供StorageService实现
     */
    protected abstract StorageService getStorageService();
    
    /**
     * 子类需要提供测试Bucket名称
     */
    protected abstract String getTestBucket();
    
    @Test
    void testUploadAndDownload() throws IOException {
        StorageService storageService = getStorageService();
        String bucket = getTestBucket();
        
        // 准备测试数据
        String key = "test/upload-" + UUID.randomUUID() + ".txt";
        String content = "Hello, Nebula Storage!";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        // 上传文件
        StorageResult uploadResult = storageService.upload(
                bucket, 
                key, 
                new ByteArrayInputStream(data), 
                metadata
        );
        
        assertThat(uploadResult.isSuccess()).isTrue();
        assertThat(uploadResult.getKey()).isEqualTo(key);
        
        // 下载文件
        StorageResult downloadResult = storageService.download(bucket, key);
        
        assertThat(downloadResult.isSuccess()).isTrue();
        assertThat(downloadResult.getInputStream()).isNotNull();
        
        // 验证内容
        String downloaded = new String(
                downloadResult.getInputStream().readAllBytes(), 
                StandardCharsets.UTF_8
        );
        assertThat(downloaded).isEqualTo(content);
        
        // 清理
        storageService.delete(bucket, key);
    }
    
    @Test
    void testDelete() throws IOException {
        StorageService storageService = getStorageService();
        String bucket = getTestBucket();
        
        // 上传文件
        String key = "test/delete-" + UUID.randomUUID() + ".txt";
        byte[] data = "test".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        storageService.upload(bucket, key, new ByteArrayInputStream(data), metadata);
        
        // 验证存在
        assertThat(storageService.exists(bucket, key)).isTrue();
        
        // 删除文件
        StorageResult deleteResult = storageService.delete(bucket, key);
        
        assertThat(deleteResult.isSuccess()).isTrue();
        assertThat(storageService.exists(bucket, key)).isFalse();
    }
    
    @Test
    void testCopy() throws IOException {
        StorageService storageService = getStorageService();
        String bucket = getTestBucket();
        
        // 上传原文件
        String sourceKey = "test/source-" + UUID.randomUUID() + ".txt";
        String targetKey = "test/target-" + UUID.randomUUID() + ".txt";
        byte[] data = "copy test".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        storageService.upload(bucket, sourceKey, new ByteArrayInputStream(data), metadata);
        
        // 复制文件
        StorageResult copyResult = storageService.copy(bucket, sourceKey, bucket, targetKey);
        
        assertThat(copyResult.isSuccess()).isTrue();
        assertThat(storageService.exists(bucket, targetKey)).isTrue();
        
        // 清理
        storageService.delete(bucket, sourceKey);
        storageService.delete(bucket, targetKey);
    }
    
    @Test
    void testListObjects() throws IOException {
        StorageService storageService = getStorageService();
        String bucket = getTestBucket();
        
        String prefix = "test/list-" + UUID.randomUUID() + "/";
        
        // 上传多个文件
        for (int i = 0; i < 3; i++) {
            String key = prefix + "file" + i + ".txt";
            byte[] data = ("content" + i).getBytes();
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("text/plain");
            metadata.setContentLength((long) data.length);
            
            storageService.upload(bucket, key, new ByteArrayInputStream(data), metadata);
        }
        
        // 列出文件
        List<ObjectSummary> objects = storageService.listObjects(bucket, prefix);
        
        assertThat(objects).hasSize(3);
        
        // 清理
        for (ObjectSummary obj : objects) {
            storageService.delete(bucket, obj.getKey());
        }
    }
    
    @Test
    void testGeneratePresignedUrl() {
        StorageService storageService = getStorageService();
        String bucket = getTestBucket();
        String key = "test/url-test.txt";
        
        String url = storageService.generatePresignedUrl(bucket, key, Duration.ofHours(1));
        
        assertThat(url).isNotNull();
        assertThat(url).contains(key);
    }
    
    @Test
    void testGetObjectMetadata() throws IOException {
        StorageService storageService = getStorageService();
        String bucket = getTestBucket();
        
        String key = "test/metadata-" + UUID.randomUUID() + ".txt";
        byte[] data = "metadata test".getBytes();
        
        ObjectMetadata uploadMetadata = new ObjectMetadata();
        uploadMetadata.setContentType("text/plain");
        uploadMetadata.setContentLength((long) data.length);
        
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("author", "test");
        uploadMetadata.setUserMetadata(userMetadata);
        
        storageService.upload(bucket, key, new ByteArrayInputStream(data), uploadMetadata);
        
        // 获取元数据
        ObjectMetadata retrievedMetadata = storageService.getObjectMetadata(bucket, key);
        
        assertThat(retrievedMetadata).isNotNull();
        assertThat(retrievedMetadata.getContentType()).isEqualTo("text/plain");
        assertThat(retrievedMetadata.getContentLength()).isEqualTo(data.length);
        assertThat(retrievedMetadata.getUserMetadata()).containsEntry("author", "test");
        
        // 清理
        storageService.delete(bucket, key);
    }
}
```

### 2. 使用示例（MinIO实现）

```java
/**
 * MinIO存储服务集成测试
 */
@SpringBootTest
@Testcontainers
class MinioStorageServiceTest extends StorageServiceContractTest {
    
    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest")
            .withUserName("minioadmin")
            .withPassword("minioadmin");
    
    @Autowired
    private StorageService storageService;
    
    private static final String TEST_BUCKET = "test-bucket";
    
    @Override
    protected StorageService getStorageService() {
        return storageService;
    }
    
    @Override
    protected String getTestBucket() {
        return TEST_BUCKET;
    }
    
    @BeforeAll
    static void setUp(@Autowired StorageService storageService) {
        // 创建测试Bucket
        storageService.createBucket(TEST_BUCKET);
    }
    
    @AfterAll
    static void tearDown(@Autowired StorageService storageService) {
        // 清理测试Bucket
        storageService.deleteBucket(TEST_BUCKET);
    }
}
```

---

## Mock测试

### 1. Mock StorageService

```java
/**
 * 文件服务Mock测试
 */
@ExtendWith(MockitoExtension.class)
class FileServiceMockTest {
    
    @Mock
    private StorageService storageService;
    
    @InjectMocks
    private FileService fileService;
    
    @Test
    void testUploadFile() throws IOException {
        // 准备Mock数据
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        
        // Mock StorageService
        StorageResult mockResult = StorageResult.builder()
                .success(true)
                .key("uploads/test.txt")
                .build();
        
        when(storageService.upload(
                anyString(), 
                anyString(), 
                any(InputStream.class), 
                any(ObjectMetadata.class)
        )).thenReturn(mockResult);
        
        // 执行测试
        String key = fileService.uploadFile(file, "test-bucket");
        
        assertThat(key).isNotNull();
        
        // 验证调用
        verify(storageService).upload(
                eq("test-bucket"), 
                anyString(), 
                any(InputStream.class), 
                any(ObjectMetadata.class)
        );
    }
    
    @Test
    void testUploadFileFailed() throws IOException {
        // Mock失败场景
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        
        StorageResult mockResult = StorageResult.builder()
                .success(false)
                .message("Upload failed")
                .build();
        
        when(storageService.upload(
                anyString(), 
                anyString(), 
                any(InputStream.class), 
                any(ObjectMetadata.class)
        )).thenReturn(mockResult);
        
        // 验证异常
        assertThatThrownBy(() -> fileService.uploadFile(file, "test-bucket"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("上传失败");
    }
}
```

---

## 性能测试

### 1. 并发上传测试

```java
/**
 * 并发上传性能测试
 */
class StoragePerformanceTest {
    
    @Test
    void testConcurrentUpload() throws InterruptedException {
        int threadCount = 10;
        int filesPerThread = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * filesPerThread);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < filesPerThread; j++) {
                    try {
                        uploadTestFile();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        
        long totalTime = endTime - startTime;
        int totalFiles = threadCount * filesPerThread;
        double avgTime = (double) totalTime / totalFiles;
        
        log.info("并发上传测试完成：");
        log.info("- 总文件数：{}", totalFiles);
        log.info("- 总耗时：{}ms", totalTime);
        log.info("- 平均耗时：{}ms/文件", avgTime);
        log.info("- QPS：{}", totalFiles * 1000.0 / totalTime);
        
        executor.shutdown();
    }
    
    private void uploadTestFile() {
        // 实现上传逻辑
    }
}
```

---

## 测试工具

### 1. 测试数据生成器

```java
/**
 * 测试数据生成器
 */
public class TestDataGenerator {
    
    /**
     * 生成随机文本文件
     */
    public static InputStream generateTextFile(int size) {
        String content = RandomStringUtils.randomAlphanumeric(size);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成随机图片文件
     */
    public static InputStream generateImageFile(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // 填充随机颜色
        Graphics2D g = image.createGraphics();
        g.setColor(new Color((int)(Math.random() * 0x1000000)));
        g.fillRect(0, 0, width, height);
        g.dispose();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }
    
    /**
     * 生成对象元数据
     */
    public static ObjectMetadata generateMetadata(String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("test-id", UUID.randomUUID().toString());
        userMetadata.put("timestamp", LocalDateTime.now().toString());
        metadata.setUserMetadata(userMetadata);
        
        return metadata;
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

