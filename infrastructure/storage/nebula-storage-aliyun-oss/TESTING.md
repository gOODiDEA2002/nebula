# Nebula Storage Aliyun OSS - 测试指南

> 阿里云OSS存储模块的测试策略与实践

## 目录

- [测试策略](#测试策略)
- [单元测试](#单元测试)
- [集成测试](#集成测试)
- [Mock测试](#mock测试)
- [E2E测试](#e2e测试)
- [性能测试](#性能测试)

---

## 测试策略

### 测试层次

1. **单元测试**：测试服务类的业务逻辑
2. **Mock测试**：使用Mock OSS客户端进行测试
3. **集成测试**：使用真实OSS环境测试（测试环境）
4. **E2E测试**：完整的端到端测试

### 测试环境

- **开发环境**：使用Mock服务
- **测试环境**：使用专用测试Bucket
- **生产环境**：严禁测试

---

## 单元测试

### 1. 配置验证测试

```java
/**
 * 阿里云OSS配置测试
 */
@SpringBootTest
class AliyunOssConfigTest {
    
    @Autowired(required = false)
    private StorageService storageService;
    
    @Autowired
    private AliyunOssProperties properties;
    
    @Test
    void testConfigurationLoaded() {
        assertThat(properties).isNotNull();
        assertThat(properties.getEndpoint()).isNotBlank();
        assertThat(properties.getAccessKeyId()).isNotBlank();
        assertThat(properties.getAccessKeySecret()).isNotBlank();
    }
    
    @Test
    void testServiceBeanCreated() {
        assertThat(storageService).isNotNull();
    }
    
    @Test
    void testDefaultBucketConfigured() {
        assertThat(properties.getDefaultBucket()).isNotBlank();
    }
}
```

### 2. 文件Key生成测试

```java
/**
 * 文件Key生成测试
 */
class FileKeyGeneratorTest {
    
    private FileKeyGenerator keyGenerator = new FileKeyGenerator();
    
    @Test
    void testGenerateKey() {
        String originalFilename = "test.jpg";
        String key = keyGenerator.generateKey("avatars", originalFilename);
        
        // 验证格式：avatars/2024/01/15/uuid.jpg
        assertThat(key).matches("^avatars/\\d{4}/\\d{2}/\\d{2}/[a-f0-9-]+\\.jpg$");
    }
    
    @Test
    void testGenerateKeyWithoutExtension() {
        String originalFilename = "test";
        String key = keyGenerator.generateKey("docs", originalFilename);
        
        // 无扩展名时不应该有点
        assertThat(key).doesNotContain(".");
    }
    
    @Test
    void testKeyUniqueness() {
        Set<String> keys = new HashSet<>();
        
        // 生成1000个key，验证唯一性
        for (int i = 0; i < 1000; i++) {
            String key = keyGenerator.generateKey("test", "file.txt");
            keys.add(key);
        }
        
        assertThat(keys).hasSize(1000);
    }
}
```

---

## 集成测试

### 1. 基础操作测试

```java
/**
 * 阿里云OSS集成测试
 */
@SpringBootTest
@TestPropertySource(properties = {
        "nebula.storage.aliyun-oss.enabled=true",
        "nebula.storage.aliyun-oss.endpoint=${test.oss.endpoint}",
        "nebula.storage.aliyun-oss.access-key-id=${test.oss.access-key-id}",
        "nebula.storage.aliyun-oss.access-key-secret=${test.oss.access-key-secret}",
        "nebula.storage.aliyun-oss.default-bucket=${test.oss.bucket}"
})
class AliyunOssStorageServiceTest extends StorageServiceContractTest {
    
    @Autowired
    private StorageService storageService;
    
    @Value("${test.oss.bucket}")
    private String testBucket;
    
    @Override
    protected StorageService getStorageService() {
        return storageService;
    }
    
    @Override
    protected String getTestBucket() {
        return testBucket;
    }
    
    /**
     * 清理测试数据
     */
    @AfterEach
    void cleanup() {
        String prefix = "test/";
        List<ObjectSummary> objects = storageService.listObjects(testBucket, prefix);
        
        for (ObjectSummary obj : objects) {
            storageService.delete(testBucket, obj.getKey());
        }
    }
}
```

### 2. 上传下载测试

```java
/**
 * 文件上传下载测试
 */
@SpringBootTest
class FileUploadDownloadTest {
    
    @Autowired
    private StorageService storageService;
    
    @Value("${test.oss.bucket}")
    private String testBucket;
    
    @Test
    void testUploadAndDownloadTextFile() throws IOException {
        // 准备测试数据
        String key = "test/upload-" + UUID.randomUUID() + ".txt";
        String content = "Hello, Aliyun OSS!";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        // 上传文件
        StorageResult uploadResult = storageService.upload(
                testBucket,
                key,
                new ByteArrayInputStream(data),
                metadata
        );
        
        assertThat(uploadResult.isSuccess()).isTrue();
        assertThat(uploadResult.getKey()).isEqualTo(key);
        assertThat(uploadResult.getEtag()).isNotBlank();
        
        // 下载文件
        StorageResult downloadResult = storageService.download(testBucket, key);
        
        assertThat(downloadResult.isSuccess()).isTrue();
        assertThat(downloadResult.getInputStream()).isNotNull();
        
        // 验证内容
        String downloaded = new String(
                downloadResult.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        assertThat(downloaded).isEqualTo(content);
        
        // 清理
        storageService.delete(testBucket, key);
    }
    
    @Test
    void testUploadImageFile() throws IOException {
        // 创建测试图片
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageData = baos.toByteArray();
        
        String key = "test/image-" + UUID.randomUUID() + ".jpg";
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");
        metadata.setContentLength((long) imageData.length);
        
        // 上传图片
        StorageResult result = storageService.upload(
                testBucket,
                key,
                new ByteArrayInputStream(imageData),
                metadata
        );
        
        assertThat(result.isSuccess()).isTrue();
        
        // 验证文件存在
        assertThat(storageService.exists(testBucket, key)).isTrue();
        
        // 清理
        storageService.delete(testBucket, key);
    }
}
```

### 3. 元数据测试

```java
/**
 * 对象元数据测试
 */
@SpringBootTest
class ObjectMetadataTest {
    
    @Autowired
    private StorageService storageService;
    
    @Value("${test.oss.bucket}")
    private String testBucket;
    
    @Test
    void testUserMetadata() throws IOException {
        String key = "test/metadata-" + UUID.randomUUID() + ".txt";
        byte[] data = "test".getBytes();
        
        ObjectMetadata uploadMetadata = new ObjectMetadata();
        uploadMetadata.setContentType("text/plain");
        uploadMetadata.setContentLength((long) data.length);
        
        // 设置用户自定义元数据
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("author", "test-user");
        userMetadata.put("department", "engineering");
        userMetadata.put("version", "1.0");
        uploadMetadata.setUserMetadata(userMetadata);
        
        // 上传文件
        storageService.upload(testBucket, key, new ByteArrayInputStream(data), uploadMetadata);
        
        // 获取元数据
        ObjectMetadata retrievedMetadata = storageService.getObjectMetadata(testBucket, key);
        
        assertThat(retrievedMetadata).isNotNull();
        assertThat(retrievedMetadata.getContentType()).isEqualTo("text/plain");
        assertThat(retrievedMetadata.getContentLength()).isEqualTo(data.length);
        assertThat(retrievedMetadata.getUserMetadata())
                .containsEntry("author", "test-user")
                .containsEntry("department", "engineering")
                .containsEntry("version", "1.0");
        
        // 清理
        storageService.delete(testBucket, key);
    }
}
```

### 4. 预签名URL测试

```java
/**
 * 预签名URL测试
 */
@SpringBootTest
class PresignedUrlTest {
    
    @Autowired
    private StorageService storageService;
    
    @Value("${test.oss.bucket}")
    private String testBucket;
    
    @Test
    void testGeneratePresignedUrl() throws IOException {
        // 上传文件
        String key = "test/url-" + UUID.randomUUID() + ".txt";
        byte[] data = "test content".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        storageService.upload(testBucket, key, new ByteArrayInputStream(data), metadata);
        
        // 生成预签名URL
        String url = storageService.generatePresignedUrl(testBucket, key, Duration.ofHours(1));
        
        assertThat(url).isNotBlank();
        assertThat(url).contains(key);
        assertThat(url).contains("Expires=");
        assertThat(url).contains("Signature=");
        
        // 使用RestTemplate验证URL可访问
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("test content");
        
        // 清理
        storageService.delete(testBucket, key);
    }
    
    @Test
    void testExpiredUrl() throws IOException, InterruptedException {
        // 上传文件
        String key = "test/expire-" + UUID.randomUUID() + ".txt";
        byte[] data = "test".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        storageService.upload(testBucket, key, new ByteArrayInputStream(data), metadata);
        
        // 生成1秒过期的URL
        String url = storageService.generatePresignedUrl(testBucket, key, Duration.ofSeconds(1));
        
        // 等待URL过期
        Thread.sleep(2000);
        
        // 验证URL已过期
        RestTemplate restTemplate = new RestTemplate();
        assertThatThrownBy(() -> restTemplate.getForEntity(url, String.class))
                .isInstanceOf(HttpClientErrorException.class);
        
        // 清理
        storageService.delete(testBucket, key);
    }
}
```

---

## Mock测试

### 1. Mock OSS客户端

```java
/**
 * Mock OSS测试
 */
@ExtendWith(MockitoExtension.class)
class AliyunOssMockTest {
    
    @Mock
    private OSS ossClient;
    
    @InjectMocks
    private AliyunOssStorageService storageService;
    
    @Test
    void testMockUpload() throws IOException {
        // Mock上传结果
        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setETag("mock-etag-123");
        
        when(ossClient.putObject(
                anyString(),
                anyString(),
                any(InputStream.class),
                any(com.aliyun.oss.model.ObjectMetadata.class)
        )).thenReturn(putObjectResult);
        
        // 执行上传
        String key = "test/mock-upload.txt";
        byte[] data = "test".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        StorageResult result = storageService.upload(
                "test-bucket",
                key,
                new ByteArrayInputStream(data),
                metadata
        );
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEtag()).isEqualTo("mock-etag-123");
        
        // 验证调用
        verify(ossClient).putObject(
                eq("test-bucket"),
                eq(key),
                any(InputStream.class),
                any(com.aliyun.oss.model.ObjectMetadata.class)
        );
    }
}
```

---

## E2E测试

### 1. 完整文件生命周期测试

```java
/**
 * 文件完整生命周期E2E测试
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileLifecycleE2ETest {
    
    @Autowired
    private StorageService storageService;
    
    @Value("${test.oss.bucket}")
    private String testBucket;
    
    private static String uploadedKey;
    
    @Test
    @Order(1)
    void step1_uploadFile() throws IOException {
        uploadedKey = "test/e2e-" + UUID.randomUUID() + ".txt";
        byte[] data = "E2E Test Content".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength((long) data.length);
        
        StorageResult result = storageService.upload(
                testBucket,
                uploadedKey,
                new ByteArrayInputStream(data),
                metadata
        );
        
        assertThat(result.isSuccess()).isTrue();
    }
    
    @Test
    @Order(2)
    void step2_verifyFileExists() {
        assertThat(storageService.exists(testBucket, uploadedKey)).isTrue();
    }
    
    @Test
    @Order(3)
    void step3_downloadFile() throws IOException {
        StorageResult result = storageService.download(testBucket, uploadedKey);
        
        assertThat(result.isSuccess()).isTrue();
        
        String content = new String(
                result.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        assertThat(content).isEqualTo("E2E Test Content");
    }
    
    @Test
    @Order(4)
    void step4_copyFile() {
        String targetKey = uploadedKey.replace("e2e-", "e2e-copy-");
        
        StorageResult result = storageService.copy(
                testBucket,
                uploadedKey,
                testBucket,
                targetKey
        );
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(storageService.exists(testBucket, targetKey)).isTrue();
        
        // 清理复制的文件
        storageService.delete(testBucket, targetKey);
    }
    
    @Test
    @Order(5)
    void step5_deleteFile() {
        StorageResult result = storageService.delete(testBucket, uploadedKey);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(storageService.exists(testBucket, uploadedKey)).isFalse();
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
@SpringBootTest
class ConcurrentUploadPerformanceTest {
    
    @Autowired
    private StorageService storageService;
    
    @Value("${test.oss.bucket}")
    private String testBucket;
    
    @Test
    void testConcurrentUpload() throws InterruptedException {
        int threadCount = 10;
        int filesPerThread = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * filesPerThread);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < filesPerThread; j++) {
                    try {
                        String key = "test/perf-" + UUID.randomUUID() + ".txt";
                        byte[] data = "performance test".getBytes();
                        
                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentType("text/plain");
                        metadata.setContentLength((long) data.length);
                        
                        StorageResult result = storageService.upload(
                                testBucket,
                                key,
                                new ByteArrayInputStream(data),
                                metadata
                        );
                        
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        
        int totalFiles = threadCount * filesPerThread;
        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / totalFiles;
        double qps = totalFiles * 1000.0 / totalTime;
        
        System.out.println("========== 并发上传性能测试结果 ==========");
        System.out.println("总文件数: " + totalFiles);
        System.out.println("成功数: " + successCount.get());
        System.out.println("失败数: " + failureCount.get());
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均耗时: " + String.format("%.2f", avgTime) + "ms/文件");
        System.out.println("QPS: " + String.format("%.2f", qps));
        
        assertThat(successCount.get()).isGreaterThan(totalFiles * 0.95); // 至少95%成功
        
        executor.shutdown();
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

