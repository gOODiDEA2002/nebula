package io.nebula.storage.minio;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.ObjectWriteResponse;
import io.nebula.storage.core.model.ObjectMetadata;
import io.nebula.storage.core.model.StorageResult;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文件上传功能测试
 */
@ExtendWith(MockitoExtension.class)
class FileUploadTest {
    
    @Mock
    private MinioClient minioClient;
    
    @Mock
    private ObjectWriteResponse objectWriteResponse;
    
    private MinIOStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new MinIOStorageService(minioClient);
    }
    
    @Test
    void testUploadInputStream() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        String content = "test content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength((long) content.getBytes().length);
        metadata.setContentType("text/plain");
        
        // 模拟桶存在
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(objectWriteResponse);
        when(objectWriteResponse.etag()).thenReturn("abc123");
        
        // 执行上传
        StorageResult result = storageService.upload(bucket, key, inputStream, metadata);
        
        // 验证结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBucket()).isEqualTo(bucket);
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getEtag()).isEqualTo("abc123");
        
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }
    
    @Test
    void testUploadByteArray() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        byte[] content = "test content".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(objectWriteResponse);
        when(objectWriteResponse.etag()).thenReturn("def456");
        
        StorageResult result = storageService.upload(bucket, key, content, metadata);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEtag()).isEqualTo("def456");
    }
    
    @Test
    void testUploadWithUserMetadata() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        byte[] content = "test".getBytes();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.getUserMetadata().put("author", "test-user");
        metadata.getUserMetadata().put("department", "engineering");
        
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(objectWriteResponse);
        
        StorageResult result = storageService.upload(bucket, key, content, metadata);
        
        assertThat(result.isSuccess()).isTrue();
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }
}

