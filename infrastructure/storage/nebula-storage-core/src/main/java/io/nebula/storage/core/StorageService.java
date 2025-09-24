package io.nebula.storage.core;

import io.nebula.storage.core.model.ObjectMetadata;
import io.nebula.storage.core.model.ObjectSummary;
import io.nebula.storage.core.model.StorageResult;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * 统一存储服务接口
 * 提供对象存储的基础操作抽象
 */
public interface StorageService {
    
    /**
     * 上传文件 - 使用输入流
     * 
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param inputStream 输入流
     * @param metadata 对象元数据
     * @return 存储结果
     */
    StorageResult upload(String bucket, String key, InputStream inputStream, ObjectMetadata metadata);
    
    /**
     * 上传文件 - 使用字节数组
     * 
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param content 文件内容字节数组
     * @param metadata 对象元数据
     * @return 存储结果
     */
    StorageResult upload(String bucket, String key, byte[] content, ObjectMetadata metadata);
    
    /**
     * 下载文件
     * 
     * @param bucket 存储桶名称
     * @param key 对象键
     * @return 存储结果，包含输入流
     */
    StorageResult download(String bucket, String key);
    
    /**
     * 删除文件
     * 
     * @param bucket 存储桶名称
     * @param key 对象键
     * @return 存储结果
     */
    StorageResult delete(String bucket, String key);
    
    /**
     * 复制文件
     * 
     * @param sourceBucket 源存储桶
     * @param sourceKey 源对象键
     * @param targetBucket 目标存储桶
     * @param targetKey 目标对象键
     * @return 存储结果
     */
    StorageResult copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey);
    
    /**
     * 生成预签名URL
     * 
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param expiration 过期时间
     * @return 预签名URL
     */
    String generatePresignedUrl(String bucket, String key, Duration expiration);
    
    /**
     * 列出对象
     * 
     * @param bucket 存储桶名称
     * @param prefix 前缀过滤
     * @return 对象摘要列表
     */
    List<ObjectSummary> listObjects(String bucket, String prefix);
    
    /**
     * 列出对象（分页）
     * 
     * @param bucket 存储桶名称
     * @param prefix 前缀过滤
     * @param maxKeys 最大返回数量
     * @param marker 分页标记
     * @return 对象摘要列表
     */
    List<ObjectSummary> listObjects(String bucket, String prefix, int maxKeys, String marker);
    
    /**
     * 检查存储桶是否存在
     * 
     * @param bucket 存储桶名称
     * @return 是否存在
     */
    boolean bucketExists(String bucket);
    
    /**
     * 创建存储桶
     * 
     * @param bucket 存储桶名称
     */
    void createBucket(String bucket);
    
    /**
     * 删除存储桶
     * 
     * @param bucket 存储桶名称
     */
    void deleteBucket(String bucket);
    
    /**
     * 获取对象元数据
     * 
     * @param bucket 存储桶名称
     * @param key 对象键
     * @return 对象元数据
     */
    ObjectMetadata getObjectMetadata(String bucket, String key);
    
    /**
     * 检查对象是否存在
     * 
     * @param bucket 存储桶名称
     * @param key 对象键
     * @return 是否存在
     */
    boolean objectExists(String bucket, String key);
}
