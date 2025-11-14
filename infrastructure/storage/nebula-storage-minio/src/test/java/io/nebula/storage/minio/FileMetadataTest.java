package io.nebula.storage.minio;

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.nebula.storage.core.model.ObjectMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文件元数据功能测试
 */
@ExtendWith(MockitoExtension.class)
class FileMetadataTest {
    
    @Mock
    private MinioClient minioClient;
    
    @Mock
    private StatObjectResponse statObjectResponse;
    
    private MinIOStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new MinIOStorageService(minioClient);
    }
    
    @Test
    void testGetObjectMetadata() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        
        // 模拟StatObjectResponse
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);
        when(statObjectResponse.size()).thenReturn(1024L);
        when(statObjectResponse.contentType()).thenReturn("text/plain");
        when(statObjectResponse.etag()).thenReturn("abc123");
        when(statObjectResponse.lastModified()).thenReturn(ZonedDateTime.now());
        
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("x-amz-meta-author", "test-user");
        when(statObjectResponse.userMetadata()).thenReturn(userMetadata);
        
        // 获取元数据
        ObjectMetadata metadata = storageService.getObjectMetadata(bucket, key);
        
        // 验证元数据
        assertThat(metadata).isNotNull();
        assertThat(metadata.getContentLength()).isEqualTo(1024L);
        assertThat(metadata.getContentType()).isEqualTo("text/plain");
        assertThat(metadata.getEtag()).isEqualTo("abc123");
        assertThat(metadata.getUserMetadata()).containsEntry("author", "test-user");
        
        verify(minioClient).statObject(argThat(args -> 
            args.bucket().equals(bucket) && args.object().equals(key)
        ));
    }
    
    @Test
    void testGetObjectMetadataWithNullUserMetadata() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);
        when(statObjectResponse.size()).thenReturn(2048L);
        when(statObjectResponse.contentType()).thenReturn("application/octet-stream");
        when(statObjectResponse.lastModified()).thenReturn(ZonedDateTime.now());
        when(statObjectResponse.userMetadata()).thenReturn(new HashMap<>());
        
        ObjectMetadata metadata = storageService.getObjectMetadata(bucket, key);
        
        assertThat(metadata).isNotNull();
        assertThat(metadata.getContentLength()).isEqualTo(2048L);
        assertThat(metadata.getUserMetadata()).isNotNull(); // 应该返回空Map而不是null
    }
}

