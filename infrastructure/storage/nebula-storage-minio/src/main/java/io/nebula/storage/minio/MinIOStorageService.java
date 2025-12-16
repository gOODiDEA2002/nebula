package io.nebula.storage.minio;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import io.nebula.storage.core.StorageService;
import io.nebula.storage.core.exception.StorageException;
import io.nebula.storage.core.model.ObjectMetadata;
import io.nebula.storage.core.model.ObjectSummary;
import io.nebula.storage.core.model.StorageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MinIO存储服务实现
 */
@Service
public class MinIOStorageService implements StorageService {
    
    private static final Logger log = LoggerFactory.getLogger(MinIOStorageService.class);
    
    private final MinioClient minioClient;
    
    /**
     * 用于生成文件访问URL的基础地址
     */
    private String accessBaseUrl;
    
    @Autowired
    public MinIOStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }
    
    /**
     * 构造函数（带访问基础URL）
     *
     * @param minioClient MinIO客户端
     * @param accessBaseUrl 用于生成文件访问URL的基础地址（domain或endpoint）
     */
    public MinIOStorageService(MinioClient minioClient, String accessBaseUrl) {
        this.minioClient = minioClient;
        this.accessBaseUrl = accessBaseUrl;
    }
    
    /**
     * 生成文件访问URL
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @return 文件访问URL，如果未配置accessBaseUrl则返回null
     */
    public String generateAccessUrl(String bucket, String key) {
        if (accessBaseUrl == null || accessBaseUrl.isBlank()) {
            return null;
        }
        String baseUrl = accessBaseUrl.endsWith("/") ? accessBaseUrl.substring(0, accessBaseUrl.length() - 1) : accessBaseUrl;
        return baseUrl + "/" + bucket + "/" + key;
    }
    
    @Override
    public StorageResult upload(String bucket, String key, InputStream inputStream, ObjectMetadata metadata) {
        try {
            // 确保bucket存在
            if (!bucketExists(bucket)) {
                createBucket(bucket);
            }
            
            // 构建上传参数
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(inputStream, metadata.getContentLength(), -1);
            
            // 设置内容类型
            if (metadata.getContentType() != null) {
                builder.contentType(metadata.getContentType());
            }
            
            // 设置用户元数据
            if (metadata.getUserMetadata() != null && !metadata.getUserMetadata().isEmpty()) {
                Map<String, String> headers = new HashMap<>();
                for (Map.Entry<String, String> entry : metadata.getUserMetadata().entrySet()) {
                    headers.put("x-amz-meta-" + entry.getKey(), entry.getValue());
                }
                builder.headers(headers);
            }
            
            // 执行上传
            ObjectWriteResponse response = minioClient.putObject(builder.build());
            
            // 生成访问URL
            String accessUrl = generateAccessUrl(bucket, key);
            
            log.info("文件上传成功: bucket={}, key={}, etag={}, url={}", bucket, key, response.etag(), accessUrl);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .etag(response.etag())
                    .versionId(response.versionId())
                    .url(accessUrl)
                    .build();
                    
        } catch (MinioException e) {
            log.error("MinIO上传失败: bucket={}, key={}", bucket, key, e);
            throw StorageException.uploadFailed(bucket, key, e.getMessage(), e);
        } catch (Exception e) {
            log.error("上传文件失败: bucket={}, key={}", bucket, key, e);
            throw StorageException.uploadFailed(bucket, key, e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult upload(String bucket, String key, byte[] content, ObjectMetadata metadata) {
        metadata.setContentLength((long) content.length);
        return upload(bucket, key, new ByteArrayInputStream(content), metadata);
    }
    
    @Override
    public StorageResult download(String bucket, String key) {
        try {
            // 获取对象
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            
            InputStream inputStream = minioClient.getObject(args);
            
            // 获取对象元数据
            ObjectMetadata metadata = getObjectMetadata(bucket, key);
            
            log.info("文件下载成功: bucket={}, key={}", bucket, key);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .inputStream(inputStream)
                    .metadata(metadata)
                    .build();
                    
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw StorageException.objectNotFound(bucket, key);
            } else if ("NoSuchBucket".equals(e.errorResponse().code())) {
                throw StorageException.bucketNotFound(bucket);
            } else {
                log.error("MinIO下载失败: bucket={}, key={}", bucket, key, e);
                throw StorageException.downloadFailed(bucket, key, e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("下载文件失败: bucket={}, key={}", bucket, key, e);
            throw StorageException.downloadFailed(bucket, key, e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult delete(String bucket, String key) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            
            minioClient.removeObject(args);
            
            log.info("文件删除成功: bucket={}, key={}", bucket, key);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .build();
                    
        } catch (Exception e) {
            log.error("删除文件失败: bucket={}, key={}", bucket, key, e);
            throw StorageException.deleteFailed(bucket, key, e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            // 确保目标bucket存在
            if (!bucketExists(targetBucket)) {
                createBucket(targetBucket);
            }
            
            CopySource source = CopySource.builder()
                    .bucket(sourceBucket)
                    .object(sourceKey)
                    .build();
            
            CopyObjectArgs args = CopyObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(targetKey)
                    .source(source)
                    .build();
            
            ObjectWriteResponse response = minioClient.copyObject(args);
            
            log.info("文件复制成功: {}:{} -> {}:{}", sourceBucket, sourceKey, targetBucket, targetKey);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(targetBucket)
                    .key(targetKey)
                    .etag(response.etag())
                    .build();
                    
        } catch (Exception e) {
            log.error("复制文件失败: {}:{} -> {}:{}", sourceBucket, sourceKey, targetBucket, targetKey, e);
            throw new StorageException("文件复制失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generatePresignedUrl(String bucket, String key, Duration expiration) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) expiration.getSeconds())
                    .build();
            
            return minioClient.getPresignedObjectUrl(args);
            
        } catch (Exception e) {
            log.error("生成预签名URL失败: bucket={}, key={}", bucket, key, e);
            throw new StorageException("生成预签名URL失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ObjectSummary> listObjects(String bucket, String prefix) {
        return listObjects(bucket, prefix, 1000, null);
    }
    
    @Override
    public List<ObjectSummary> listObjects(String bucket, String prefix, int maxKeys, String marker) {
        try {
            ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                    .bucket(bucket)
                    .maxKeys(maxKeys);
            
            if (prefix != null) {
                builder.prefix(prefix);
            }
            
            if (marker != null) {
                builder.startAfter(marker);
            }
            
            Iterable<Result<Item>> results = minioClient.listObjects(builder.build());
            
            List<ObjectSummary> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                
                ObjectSummary summary = ObjectSummary.builder()
                        .bucket(bucket)
                        .key(item.objectName())
                        .size(item.size())
                        .lastModified(LocalDateTime.ofInstant(item.lastModified().toInstant(), ZoneId.systemDefault()))
                        .etag(item.etag())
                        .storageClass(item.storageClass())
                        .directory(item.isDir())
                        .build();
                
                objects.add(summary);
            }
            
            return objects;
            
        } catch (Exception e) {
            log.error("列出对象失败: bucket={}, prefix={}", bucket, prefix, e);
            throw new StorageException("列出对象失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean bucketExists(String bucket) {
        try {
            BucketExistsArgs args = BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build();
            
            return minioClient.bucketExists(args);
            
        } catch (Exception e) {
            log.error("检查bucket是否存在失败: bucket={}", bucket, e);
            return false;
        }
    }
    
    @Override
    public void createBucket(String bucket) {
        try {
            MakeBucketArgs args = MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build();
            
            minioClient.makeBucket(args);
            
            log.info("创建bucket成功: bucket={}", bucket);
            
        } catch (Exception e) {
            log.error("创建bucket失败: bucket={}", bucket, e);
            throw new StorageException("创建bucket失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void deleteBucket(String bucket) {
        try {
            RemoveBucketArgs args = RemoveBucketArgs.builder()
                    .bucket(bucket)
                    .build();
            
            minioClient.removeBucket(args);
            
            log.info("删除bucket成功: bucket={}", bucket);
            
        } catch (Exception e) {
            log.error("删除bucket失败: bucket={}", bucket, e);
            throw new StorageException("删除bucket失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ObjectMetadata getObjectMetadata(String bucket, String key) {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            
            StatObjectResponse response = minioClient.statObject(args);
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(response.contentType());
            metadata.setContentLength(response.size());
            metadata.setEtag(response.etag());
            metadata.setLastModified(LocalDateTime.ofInstant(response.lastModified().toInstant(), ZoneId.systemDefault()));
            
            // 提取用户元数据
            Map<String, String> userMetadata = new HashMap<>();
            for (Map.Entry<String, String> entry : response.userMetadata().entrySet()) {
                String key1 = entry.getKey();
                if (key1.startsWith("x-amz-meta-")) {
                    String userKey = key1.substring("x-amz-meta-".length());
                    String value = entry.getValue();
                    userMetadata.put(userKey, value);
                }
            }
            metadata.setUserMetadata(userMetadata);
            
            return metadata;
            
        } catch (Exception e) {
            log.error("获取对象元数据失败: bucket={}, key={}", bucket, key, e);
            throw new StorageException("获取对象元数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean objectExists(String bucket, String key) {
        try {
            getObjectMetadata(bucket, key);
            return true;
        } catch (StorageException e) {
            if (StorageException.OBJECT_NOT_FOUND.equals(e.getErrorCode())) {
                return false;
            }
            throw e;
        }
    }
}
