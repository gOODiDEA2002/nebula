# nebula-storage-minio 模块单元测试清单

## 模块说明

MinIO对象存储实现模块，提供统一的文件存储和管理功能。

## 核心功能

1. 文件上传（单文件、多文件）
2. 文件下载
3. 文件删除
4. 桶（Bucket）管理
5. 文件元数据管理
6. 预签名URL生成

## 测试类清单

### 1. FileUploadTest

**测试类路径**: `io.nebula.storage.minio.MinioStorageService`  
**测试目的**: 验证文件上传功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testUploadFile() | upload(InputStream, String) | 测试单文件上传 | MinioClient |
| testUploadWithMetadata() | upload(InputStream, String, Map) | 测试带元数据的上传 | MinioClient |
| testUploadMultipart() | uploadMultipart(MultipartFile) | 测试分片上传 | MinioClient |
| testUploadLargeFile() | upload() | 测试大文件上传 | MinioClient |

**测试数据准备**:
- Mock MinioClient
- 准备测试文件InputStream

**验证要点**:
- 文件正确上传
- 元数据正确设置
- 返回的文件路径正确
- 大文件上传正确处理

**Mock示例**:
```java
@Mock
private MinioClient minioClient;

@InjectMocks
private MinioStorageService storageService;

@Test
void testUploadFile() throws Exception {
    String bucketName = "test-bucket";
    String fileName = "test.txt";
    byte[] content = "test content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(content);
    
    ObjectWriteResponse mockResponse = mock(ObjectWriteResponse.class);
    when(mockResponse.object()).thenReturn(fileName);
    
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenReturn(mockResponse);
    
    String path = storageService.upload(inputStream, fileName);
    
    assertThat(path).contains(fileName);
    verify(minioClient).putObject(any(PutObjectArgs.class));
}
```

---

### 2. FileDownloadTest

**测试类路径**: `io.nebula.storage.minio.MinioStorageService`  
**测试目的**: 验证文件下载功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testDownloadFile() | download(String) | 测试文件下载 | MinioClient |
| testDownloadToStream() | download(String, OutputStream) | 测试下载到流 | MinioClient |
| testDownloadNotFound() | download() | 测试下载不存在的文件 | MinioClient |

**测试数据准备**:
- Mock MinioClient
- 准备模拟的文件流

**验证要点**:
- 文件正确下载
- 文件内容完整
- 不存在的文件正确处理

**Mock示例**:
```java
@Test
void testDownloadFile() throws Exception {
    String filePath = "test/test.txt";
    byte[] expectedContent = "test content".getBytes();
    InputStream mockStream = new ByteArrayInputStream(expectedContent);
    
    GetObjectResponse mockResponse = mock(GetObjectResponse.class);
    
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenReturn(mockStream);
    
    InputStream result = storageService.download(filePath);
    
    byte[] actualContent = result.readAllBytes();
    assertThat(actualContent).isEqualTo(expectedContent);
    verify(minioClient).getObject(any(GetObjectArgs.class));
}
```

---

### 3. FileDeleteTest

**测试类路径**: `io.nebula.storage.minio.MinioStorageService`  
**测试目的**: 验证文件删除功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testDeleteFile() | delete(String) | 测试删除单个文件 | MinioClient |
| testDeleteMultipleFiles() | deleteMultiple(List&lt;String&gt;) | 测试批量删除文件 | MinioClient |
| testDeleteNonExistentFile() | delete() | 测试删除不存在的文件 | MinioClient |

**测试数据准备**:
- Mock MinioClient
- 准备测试文件路径

**验证要点**:
- 文件正确删除
- 批量删除正确执行
- 不存在的文件不抛异常

**Mock示例**:
```java
@Test
void testDeleteFile() throws Exception {
    String filePath = "test/test.txt";
    
    doNothing().when(minioClient)
        .removeObject(any(RemoveObjectArgs.class));
    
    storageService.delete(filePath);
    
    verify(minioClient).removeObject(any(RemoveObjectArgs.class));
}
```

---

### 4. BucketManagementTest

**测试类路径**: `io.nebula.storage.minio.MinioStorageService`  
**测试目的**: 验证桶管理功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCreateBucket() | createBucket(String) | 测试创建桶 | MinioClient |
| testDeleteBucket() | deleteBucket(String) | 测试删除桶 | MinioClient |
| testBucketExists() | bucketExists(String) | 测试检查桶是否存在 | MinioClient |
| testListBuckets() | listBuckets() | 测试列出所有桶 | MinioClient |

**测试数据准备**:
- Mock MinioClient
- 准备桶名称

**验证要点**:
- 桶正确创建
- 桶正确删除
- 存在性检查正确
- 桶列表正确返回

**Mock示例**:
```java
@Test
void testCreateBucket() throws Exception {
    String bucketName = "test-bucket";
    
    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenReturn(false);
    
    doNothing().when(minioClient)
        .makeBucket(any(MakeBucketArgs.class));
    
    storageService.createBucket(bucketName);
    
    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
}
```

---

### 5. PresignedUrlTest

**测试类路径**: `io.nebula.storage.minio.MinioStorageService`  
**测试目的**: 验证预签名URL生成功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGetPresignedUrl() | getPresignedUrl(String) | 测试生成下载URL | MinioClient |
| testGetPresignedUrlWithExpiry() | getPresignedUrl(String, int) | 测试带过期时间的URL | MinioClient |
| testGetPresignedUploadUrl() | getPresignedUploadUrl(String) | 测试生成上传URL | MinioClient |

**测试数据准备**:
- Mock MinioClient
- 准备文件路径和过期时间

**验证要点**:
- URL正确生成
- 过期时间正确设置
- URL格式正确

**Mock示例**:
```java
@Test
void testGetPresignedUrl() throws Exception {
    String filePath = "test/test.txt";
    String expectedUrl = "http://minio:9000/test-bucket/test/test.txt?signature=xxx";
    
    when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenReturn(expectedUrl);
    
    String url = storageService.getPresignedUrl(filePath);
    
    assertThat(url).isEqualTo(expectedUrl);
    verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
}
```

---

### 6. FileMetadataTest

**测试类路径**: `io.nebula.storage.minio.MinioStorageService`  
**测试目的**: 验证文件元数据管理功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGetFileInfo() | getFileInfo(String) | 测试获取文件信息 | MinioClient |
| testGetFileMetadata() | getMetadata(String) | 测试获取文件元数据 | MinioClient |
| testListFiles() | listFiles(String) | 测试列出文件 | MinioClient |

**测试数据准备**:
- Mock MinioClient
- 准备StatObjectResponse

**验证要点**:
- 文件信息正确返回
- 元数据正确解析
- 文件列表正确返回

**Mock示例**:
```java
@Test
void testGetFileInfo() throws Exception {
    String filePath = "test/test.txt";
    
    StatObjectResponse mockResponse = mock(StatObjectResponse.class);
    when(mockResponse.size()).thenReturn(1024L);
    when(mockResponse.contentType()).thenReturn("text/plain");
    
    when(minioClient.statObject(any(StatObjectArgs.class)))
        .thenReturn(mockResponse);
    
    FileInfo fileInfo = storageService.getFileInfo(filePath);
    
    assertThat(fileInfo.getSize()).isEqualTo(1024L);
    assertThat(fileInfo.getContentType()).isEqualTo("text/plain");
}
```

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| MinioClient | 所有MinIO操作 | Mock putObject(), getObject(), removeObject() |
| ObjectWriteResponse | 上传响应 | Mock object() |
| StatObjectResponse | 文件信息 | Mock size(), contentType() |

### 不需要真实MinIO
**所有测试都应该Mock MinioClient，不需要启动真实的MinIO服务器**。

---

## 测试依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/infrastructure/storage/nebula-storage-minio
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- 文件上传下载删除测试通过
- 桶管理和预签名URL测试通过
- 元数据管理测试通过

