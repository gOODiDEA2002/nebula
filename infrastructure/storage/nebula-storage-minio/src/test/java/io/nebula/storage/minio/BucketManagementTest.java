package io.nebula.storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 存储桶管理功能测试
 */
@ExtendWith(MockitoExtension.class)
class BucketManagementTest {
    
    @Mock
    private MinioClient minioClient;
    
    private MinIOStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new MinIOStorageService(minioClient);
    }
    
    @Test
    void testBucketExists() throws Exception {
        String bucket = "test-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        
        boolean exists = storageService.bucketExists(bucket);
        
        assertThat(exists).isTrue();
        verify(minioClient).bucketExists(argThat(args -> args.bucket().equals(bucket)));
    }
    
    @Test
    void testBucketNotExists() throws Exception {
        String bucket = "non-existent-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        
        boolean exists = storageService.bucketExists(bucket);
        
        assertThat(exists).isFalse();
    }
    
    @Test
    void testCreateBucket() throws Exception {
        String bucket = "new-bucket";
        
        storageService.createBucket(bucket);
        
        verify(minioClient).makeBucket(argThat(args -> args.bucket().equals(bucket)));
    }
    
    @Test
    void testDeleteBucket() throws Exception {
        String bucket = "test-bucket";
        
        storageService.deleteBucket(bucket);
        
        verify(minioClient).removeBucket(argThat(args -> args.bucket().equals(bucket)));
    }
}

