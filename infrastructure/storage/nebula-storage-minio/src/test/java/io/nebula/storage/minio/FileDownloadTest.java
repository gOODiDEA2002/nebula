package io.nebula.storage.minio;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import io.nebula.storage.core.exception.StorageException;
import io.nebula.storage.core.model.StorageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文件下载功能测试
 */
@ExtendWith(MockitoExtension.class)
class FileDownloadTest {
    
    @Mock
    private MinioClient minioClient;
    
    @Mock
    private StatObjectResponse statObjectResponse;
    
    @Mock
    private GetObjectResponse getObjectResponse;
    
    private MinIOStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new MinIOStorageService(minioClient);
    }
    
    @Test
    void testDownload() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        String content = "test content";
        
        // 模拟获取对象
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);
        
        // 模拟获取元数据
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);
        when(statObjectResponse.size()).thenReturn((long) content.getBytes().length);
        when(statObjectResponse.contentType()).thenReturn("text/plain");
        when(statObjectResponse.lastModified()).thenReturn(ZonedDateTime.now());
        
        // 执行下载
        StorageResult result = storageService.download(bucket, key);
        
        // 验证结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBucket()).isEqualTo(bucket);
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getInputStream()).isNotNull();
        assertThat(result.getMetadata()).isNotNull();
        
        verify(minioClient).getObject(argThat(args -> 
            args.bucket().equals(bucket) && args.object().equals(key)
        ));
    }
    
    @Test
    void testDownloadObjectNotFound() throws Exception {
        String bucket = "test-bucket";
        String key = "non-existent-file.txt";
        
        // 模拟对象不存在
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        
        ErrorResponseException exception = new ErrorResponseException(
                errorResponse, null, "servletPath"
        );
        
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(exception);
        
        // 验证抛出异常
        assertThatThrownBy(() -> storageService.download(bucket, key))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("not found");
    }
    
    @Test
    void testDownloadBucketNotFound() throws Exception {
        String bucket = "non-existent-bucket";
        String key = "test-file.txt";
        
        // 模拟桶不存在
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchBucket");
        
        ErrorResponseException exception = new ErrorResponseException(
                errorResponse, null, "servletPath"
        );
        
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(exception);
        
        // 验证抛出异常
        assertThatThrownBy(() -> storageService.download(bucket, key))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("not found");
    }
}

