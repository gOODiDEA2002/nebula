package io.nebula.storage.aliyun.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
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
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 阿里云OSS存储服务实现
 */
@Service
public class AliyunOSSStorageService implements StorageService {
    
    private static final Logger log = LoggerFactory.getLogger(AliyunOSSStorageService.class);
    
    private final OSS ossClient;
    
    @Autowired
    public AliyunOSSStorageService(OSS ossClient) {
        this.ossClient = ossClient;
    }
    
    @Override
    public StorageResult upload(String bucket, String key, InputStream inputStream, ObjectMetadata metadata) {
        try {
            // 确保bucket存在
            if (!bucketExists(bucket)) {
                createBucket(bucket);
            }
            
            // 构建OSS元数据
            com.aliyun.oss.model.ObjectMetadata ossMetadata = new com.aliyun.oss.model.ObjectMetadata();
            if (metadata.getContentType() != null) {
                ossMetadata.setContentType(metadata.getContentType());
            }
            if (metadata.getContentLength() != null) {
                ossMetadata.setContentLength(metadata.getContentLength());
            }
            if (metadata.getUserMetadata() != null) {
                ossMetadata.setUserMetadata(metadata.getUserMetadata());
            }
            
            // 执行上传
            PutObjectResult result = ossClient.putObject(bucket, key, inputStream, ossMetadata);
            
            log.info("阿里云OSS文件上传成功: bucket={}, key={}, etag={}", bucket, key, result.getETag());
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .etag(result.getETag())
                    .versionId(result.getVersionId())
                    .build();
                    
        } catch (Exception e) {
            log.error("阿里云OSS上传失败: bucket={}, key={}", bucket, key, e);
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
            OSSObject ossObject = ossClient.getObject(bucket, key);
            
            // 转换元数据
            ObjectMetadata metadata = convertMetadata(ossObject.getObjectMetadata());
            
            log.info("阿里云OSS文件下载成功: bucket={}, key={}", bucket, key);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .inputStream(ossObject.getObjectContent())
                    .metadata(metadata)
                    .build();
                    
        } catch (Exception e) {
            log.error("阿里云OSS下载失败: bucket={}, key={}", bucket, key, e);
            throw StorageException.downloadFailed(bucket, key, e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult delete(String bucket, String key) {
        try {
            ossClient.deleteObject(bucket, key);
            
            log.info("阿里云OSS文件删除成功: bucket={}, key={}", bucket, key);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .build();
                    
        } catch (Exception e) {
            log.error("阿里云OSS删除失败: bucket={}, key={}", bucket, key, e);
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
            
            CopyObjectRequest request = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
            CopyObjectResult result = ossClient.copyObject(request);
            
            log.info("阿里云OSS文件复制成功: {}:{} -> {}:{}", sourceBucket, sourceKey, targetBucket, targetKey);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(targetBucket)
                    .key(targetKey)
                    .etag(result.getETag())
                    .build();
                    
        } catch (Exception e) {
            log.error("阿里云OSS复制失败: {}:{} -> {}:{}", sourceBucket, sourceKey, targetBucket, targetKey, e);
            throw new StorageException("文件复制失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generatePresignedUrl(String bucket, String key, Duration expiration) {
        try {
            Date expirationDate = new Date(System.currentTimeMillis() + expiration.toMillis());
            return ossClient.generatePresignedUrl(bucket, key, expirationDate).toString();
        } catch (Exception e) {
            log.error("阿里云OSS生成预签名URL失败: bucket={}, key={}", bucket, key, e);
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
            ListObjectsRequest request = new ListObjectsRequest(bucket);
            request.setPrefix(prefix);
            request.setMaxKeys(maxKeys);
            if (marker != null) {
                request.setMarker(marker);
            }
            
            ObjectListing listing = ossClient.listObjects(request);
            
            List<ObjectSummary> objects = new ArrayList<>();
            for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                ObjectSummary objectSummary = ObjectSummary.builder()
                        .bucket(bucket)
                        .key(summary.getKey())
                        .size(summary.getSize())
                        .lastModified(LocalDateTime.ofInstant(summary.getLastModified().toInstant(), ZoneId.systemDefault()))
                        .etag(summary.getETag())
                        .storageClass(summary.getStorageClass())
                        .build();
                
                objects.add(objectSummary);
            }
            
            return objects;
            
        } catch (Exception e) {
            log.error("阿里云OSS列出对象失败: bucket={}, prefix={}", bucket, prefix, e);
            throw new StorageException("列出对象失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean bucketExists(String bucket) {
        try {
            return ossClient.doesBucketExist(bucket);
        } catch (Exception e) {
            log.error("阿里云OSS检查bucket是否存在失败: bucket={}", bucket, e);
            return false;
        }
    }
    
    @Override
    public void createBucket(String bucket) {
        try {
            ossClient.createBucket(bucket);
            log.info("阿里云OSS创建bucket成功: bucket={}", bucket);
        } catch (Exception e) {
            log.error("阿里云OSS创建bucket失败: bucket={}", bucket, e);
            throw new StorageException("创建bucket失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void deleteBucket(String bucket) {
        try {
            ossClient.deleteBucket(bucket);
            log.info("阿里云OSS删除bucket成功: bucket={}", bucket);
        } catch (Exception e) {
            log.error("阿里云OSS删除bucket失败: bucket={}", bucket, e);
            throw new StorageException("删除bucket失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ObjectMetadata getObjectMetadata(String bucket, String key) {
        try {
            com.aliyun.oss.model.ObjectMetadata ossMetadata = ossClient.getObjectMetadata(bucket, key);
            return convertMetadata(ossMetadata);
        } catch (Exception e) {
            log.error("阿里云OSS获取对象元数据失败: bucket={}, key={}", bucket, key, e);
            throw new StorageException("获取对象元数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean objectExists(String bucket, String key) {
        try {
            return ossClient.doesObjectExist(bucket, key);
        } catch (Exception e) {
            log.error("阿里云OSS检查对象是否存在失败: bucket={}, key={}", bucket, key, e);
            return false;
        }
    }
    
    /**
     * 转换OSS元数据为Nebula元数据
     */
    private ObjectMetadata convertMetadata(com.aliyun.oss.model.ObjectMetadata ossMetadata) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(ossMetadata.getContentType());
        metadata.setContentLength(ossMetadata.getContentLength());
        metadata.setEtag(ossMetadata.getETag());
        if (ossMetadata.getLastModified() != null) {
            metadata.setLastModified(LocalDateTime.ofInstant(
                    ossMetadata.getLastModified().toInstant(), ZoneId.systemDefault()));
        }
        metadata.setUserMetadata(ossMetadata.getUserMetadata());
        return metadata;
    }
}
