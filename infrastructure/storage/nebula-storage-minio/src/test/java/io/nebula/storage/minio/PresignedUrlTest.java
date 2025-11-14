package io.nebula.storage.minio;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 预签名URL功能测试
 */
@ExtendWith(MockitoExtension.class)
class PresignedUrlTest {
    
    @Mock
    private MinioClient minioClient;
    
    private MinIOStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new MinIOStorageService(minioClient);
    }
    
    @Test
    void testGeneratePresignedUrl() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        Duration expiration = Duration.ofHours(1);
        String expectedUrl = "https://minio.example.com/test-bucket/test-file.txt?X-Amz-Expires=3600";
        
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(expectedUrl);
        
        String url = storageService.generatePresignedUrl(bucket, key, expiration);
        
        assertThat(url).isEqualTo(expectedUrl);
        verify(minioClient).getPresignedObjectUrl(argThat(args -> 
            args.bucket().equals(bucket) && 
            args.object().equals(key) &&
            args.method() == Method.GET
        ));
    }
    
    @Test
    void testGeneratePresignedUrlWithDifferentExpiration() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        Duration expiration1h = Duration.ofHours(1);
        Duration expiration24h = Duration.ofHours(24);
        
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://example.com/url");
        
        // 生成1小时过期的URL
        storageService.generatePresignedUrl(bucket, key, expiration1h);
        
        // 生成24小时过期的URL
        storageService.generatePresignedUrl(bucket, key, expiration24h);
        
        // 验证被调用2次
        verify(minioClient, times(2)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }
}

