package io.nebula.storage.minio;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.nebula.storage.core.exception.StorageException;
import io.nebula.storage.core.model.StorageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文件删除功能测试
 */
@ExtendWith(MockitoExtension.class)
class FileDeleteTest {
    
    @Mock
    private MinioClient minioClient;
    
    private MinIOStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new MinIOStorageService(minioClient);
    }
    
    @Test
    void testDelete() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        
        // 执行删除
        StorageResult result = storageService.delete(bucket, key);
        
        // 验证删除调用
        verify(minioClient).removeObject(argThat(args -> 
            args.bucket().equals(bucket) && args.object().equals(key)
        ));
        
        // 验证结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBucket()).isEqualTo(bucket);
        assertThat(result.getKey()).isEqualTo(key);
    }
    
    @Test
    void testDeleteMultiple() throws Exception {
        String bucket = "test-bucket";
        
        // 删除多个文件
        storageService.delete(bucket, "file1.txt");
        storageService.delete(bucket, "file2.txt");
        storageService.delete(bucket, "file3.txt");
        
        // 验证删除被调用3次
        verify(minioClient, times(3)).removeObject(any(RemoveObjectArgs.class));
    }
    
    @Test
    void testDeleteException() throws Exception {
        String bucket = "test-bucket";
        String key = "test-file.txt";
        
        // 模拟删除失败
        doThrow(new RuntimeException("Delete failed")).when(minioClient).removeObject(any(RemoveObjectArgs.class));
        
        // 验证抛出异常
        assertThatThrownBy(() -> storageService.delete(bucket, key))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("failed");
    }
}

