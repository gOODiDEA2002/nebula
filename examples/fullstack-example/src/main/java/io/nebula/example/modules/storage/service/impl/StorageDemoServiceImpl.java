package io.nebula.example.modules.storage.service.impl;

import io.nebula.example.modules.storage.entity.dto.*;
import io.nebula.example.modules.storage.entity.vo.FileInfoVo;
import io.nebula.example.modules.storage.service.StorageDemoService;
import io.nebula.storage.core.StorageService;
import io.nebula.storage.core.exception.StorageException;
import io.nebula.storage.core.model.ObjectMetadata;
import io.nebula.storage.core.model.ObjectSummary;
import io.nebula.storage.core.model.StorageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 存储演示服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(StorageService.class)
public class StorageDemoServiceImpl implements StorageDemoService {
    
    private final StorageService storageService;
    
    @Override
    public UploadFileDto.Response uploadFile(UploadFileDto.Request request) {
        try {
            MultipartFile file = request.getFile();
            
            // 生成文件键
            String key = generateFileKey(request.getCategory(), file.getOriginalFilename());
            
            // 构建元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            
            // 添加自定义元数据
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(metadata::addUserMetadata);
            }
            metadata.addUserMetadata("original-name", file.getOriginalFilename());
            metadata.addUserMetadata("upload-time", Instant.now().toString());
            
            // 上传文件
            StorageResult result = storageService.upload(
                request.getBucket(),
                key,
                file.getInputStream(),
                metadata
            );
            
            if (!result.isSuccess()) {
                throw new StorageException(result.getErrorCode(), result.getErrorMessage());
            }
            
            log.info("文件上传成功: bucket={}, key={}, size={}", 
                    request.getBucket(), key, file.getSize());
            
            // 构建响应
            UploadFileDto.Response response = new UploadFileDto.Response();
            response.setKey(key);
            response.setFileName(file.getOriginalFilename());
            response.setFileSize(file.getSize());
            response.setEtag(result.getEtag());
            response.setBucket(request.getBucket());
            response.setUploadTime(Instant.now().toString());
            
            return response;
            
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new StorageException("FILE_READ_ERROR", "读取文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public DownloadFileDto.Response downloadFile(DownloadFileDto.Request request) {
        // 获取对象元数据
        ObjectMetadata metadata = storageService.getObjectMetadata(
            request.getBucket(),
            request.getKey()
        );
        
        log.info("获取文件元数据: bucket={}, key={}, size={}", 
                request.getBucket(), request.getKey(), metadata.getContentLength());
        
        // 构建响应
        DownloadFileDto.Response response = new DownloadFileDto.Response();
        response.setFileName(extractFileName(request.getKey()));
        response.setFileSize(metadata.getContentLength());
        response.setContentType(metadata.getContentType());
        
        return response;
    }
    
    @Override
    public ListFilesDto.Response listFiles(ListFilesDto.Request request) {
        // 列出对象
        List<ObjectSummary> objects = storageService.listObjects(
            request.getBucket(),
            request.getPrefix(),
            request.getMaxKeys(),
            request.getMarker()
        );
        
        // 转换为VO
        List<FileInfoVo> files = objects.stream()
            .filter(obj -> !obj.isDirectory())  // 过滤目录
            .map(this::convertToFileInfoVo)
            .collect(Collectors.toList());
        
        log.info("列出文件: bucket={}, prefix={}, count={}", 
                request.getBucket(), request.getPrefix(), files.size());
        
        // 构建响应
        ListFilesDto.Response response = new ListFilesDto.Response();
        response.setFiles(files);
        response.setTotal(files.size());
        response.setHasMore(files.size() >= request.getMaxKeys());
        if (response.getHasMore() && !files.isEmpty()) {
            response.setNextMarker(files.get(files.size() - 1).getKey());
        }
        
        return response;
    }
    
    @Override
    public DeleteFileDto.Response deleteFile(DeleteFileDto.Request request) {
        List<String> failedKeys = new ArrayList<>();
        int deletedCount = 0;
        
        for (String key : request.getKeys()) {
            try {
                StorageResult result = storageService.delete(request.getBucket(), key);
                
                if (result.isSuccess()) {
                    deletedCount++;
                    log.info("文件删除成功: bucket={}, key={}", request.getBucket(), key);
                } else {
                    failedKeys.add(key);
                    log.warn("文件删除失败: bucket={}, key={}, error={}", 
                            request.getBucket(), key, result.getErrorMessage());
                }
                
            } catch (Exception e) {
                failedKeys.add(key);
                log.error("文件删除异常: bucket={}, key={}", request.getBucket(), key, e);
            }
        }
        
        // 构建响应
        DeleteFileDto.Response response = new DeleteFileDto.Response();
        response.setDeletedCount(deletedCount);
        response.setFailedKeys(failedKeys.isEmpty() ? null : failedKeys);
        
        return response;
    }
    
    @Override
    public GenerateUrlDto.Response generatePresignedUrl(GenerateUrlDto.Request request) {
        // 生成预签名URL
        Duration expiration = Duration.ofSeconds(request.getExpirySeconds());
        String url = storageService.generatePresignedUrl(
            request.getBucket(),
            request.getKey(),
            expiration
        );
        
        log.info("生成预签名URL: bucket={}, key={}, expiry={}s", 
                request.getBucket(), request.getKey(), request.getExpirySeconds());
        
        // 计算过期时间
        Instant expiryInstant = Instant.now().plus(expiration);
        
        // 构建响应
        GenerateUrlDto.Response response = new GenerateUrlDto.Response();
        response.setUrl(url);
        response.setExpirySeconds(request.getExpirySeconds());
        response.setExpiryTime(expiryInstant.toString());
        
        return response;
    }
    
    /**
     * 生成文件键
     */
    private String generateFileKey(String category, String originalFilename) {
        LocalDate date = LocalDate.now();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        return String.format("%s/%d/%02d/%02d/%s_%s",
                category,
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                uuid,
                originalFilename);
    }
    
    /**
     * 从键中提取文件名
     */
    private String extractFileName(String key) {
        if (key == null) {
            return null;
        }
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
    }
    
    /**
     * 转换为FileInfoVo
     */
    private FileInfoVo convertToFileInfoVo(ObjectSummary summary) {
        FileInfoVo vo = new FileInfoVo();
        vo.setKey(summary.getKey());
        vo.setFileName(summary.getFileName());
        vo.setFileSize(summary.getSize());
        vo.setFormattedSize(summary.getFormattedSize());
        vo.setContentType(summary.getContentType());
        vo.setEtag(summary.getEtag());
        vo.setLastModified(summary.getLastModified());
        vo.setBucket(summary.getBucket());
        vo.setExtension(summary.getFileExtension());
        return vo;
    }
}

