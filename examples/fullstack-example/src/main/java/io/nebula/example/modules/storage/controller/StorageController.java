package io.nebula.example.modules.storage.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.storage.entity.dto.*;
import io.nebula.example.modules.storage.service.StorageDemoService;
import io.nebula.storage.core.StorageService;
import io.nebula.storage.core.model.StorageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 对象存储演示控制器
 * 演示 Nebula 对象存储功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Validated
@Tag(name = "对象存储演示", description = "Nebula 对象存储功能演示API")
@ConditionalOnBean(StorageService.class)
public class StorageController {
    
    private final StorageDemoService storageDemoService;
    private final StorageService storageService;
    
    @Operation(summary = "上传文件", description = "上传文件到MinIO对象存储")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadFileDto.Response> uploadFile(
            @Valid @ModelAttribute UploadFileDto.Request request) {
        log.info("接收文件上传请求: bucket={}, category={}, filename={}", 
                request.getBucket(), request.getCategory(), 
                request.getFile().getOriginalFilename());
        
        UploadFileDto.Response response = storageDemoService.uploadFile(request);
        return Result.success(response, "文件上传成功");
    }
    
    @Operation(summary = "下载文件", description = "从MinIO对象存储下载文件")
    @GetMapping("/download")
    public void downloadFile(
            @Valid @ModelAttribute DownloadFileDto.Request request,
            HttpServletResponse response) {
        log.info("接收文件下载请求: bucket={}, key={}", request.getBucket(), request.getKey());
        
        try {
            // 获取文件元数据
            DownloadFileDto.Response metadata = storageDemoService.downloadFile(request);
            
            // 下载文件
            StorageResult result = storageService.download(request.getBucket(), request.getKey());
            
            if (!result.isSuccess()) {
                throw new RuntimeException("下载失败: " + result.getErrorMessage());
            }
            
            // 设置响应头
            response.setContentType(metadata.getContentType());
            response.setContentLengthLong(metadata.getFileSize());
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + metadata.getFileName() + "\"");
            
            // 写入响应流
            try (InputStream inputStream = result.getInputStream();
                 OutputStream outputStream = response.getOutputStream()) {
                IOUtils.copy(inputStream, outputStream);
                outputStream.flush();
            }
            
            log.info("文件下载成功: bucket={}, key={}, size={}", 
                    request.getBucket(), request.getKey(), metadata.getFileSize());
            
        } catch (IOException e) {
            log.error("下载文件失败: bucket={}, key={}", request.getBucket(), request.getKey(), e);
            throw new RuntimeException("下载文件失败", e);
        }
    }
    
    @Operation(summary = "列出文件", description = "列出存储桶中的文件")
    @PostMapping("/list")
    public Result<ListFilesDto.Response> listFiles(
            @Valid @RequestBody ListFilesDto.Request request) {
        log.info("接收文件列表请求: bucket={}, prefix={}, maxKeys={}", 
                request.getBucket(), request.getPrefix(), request.getMaxKeys());
        
        ListFilesDto.Response response = storageDemoService.listFiles(request);
        return Result.success(response, "查询文件列表成功");
    }
    
    @Operation(summary = "删除文件", description = "从对象存储中删除文件")
    @DeleteMapping("/delete")
    public Result<DeleteFileDto.Response> deleteFile(
            @Valid @RequestBody DeleteFileDto.Request request) {
        log.info("接收文件删除请求: bucket={}, keys={}", 
                request.getBucket(), request.getKeys());
        
        DeleteFileDto.Response response = storageDemoService.deleteFile(request);
        return Result.success(response, "文件删除操作完成");
    }
    
    @Operation(summary = "生成预签名URL", description = "生成文件的临时访问URL")
    @PostMapping("/presigned-url")
    public Result<GenerateUrlDto.Response> generatePresignedUrl(
            @Valid @RequestBody GenerateUrlDto.Request request) {
        log.info("接收生成预签名URL请求: bucket={}, key={}, expiry={}s", 
                request.getBucket(), request.getKey(), request.getExpirySeconds());
        
        GenerateUrlDto.Response response = storageDemoService.generatePresignedUrl(request);
        return Result.success(response, "生成预签名URL成功");
    }
    
    @Operation(summary = "检查Bucket是否存在", description = "检查指定的存储桶是否存在")
    @GetMapping("/bucket/exists")
    public Result<Boolean> bucketExists(@RequestParam String bucket) {
        log.info("检查Bucket是否存在: bucket={}", bucket);
        boolean exists = storageService.bucketExists(bucket);
        return Result.success(exists, "查询成功");
    }
    
    @Operation(summary = "创建Bucket", description = "创建新的存储桶")
    @PostMapping("/bucket/create")
    public Result<Void> createBucket(@RequestParam String bucket) {
        log.info("创建Bucket: bucket={}", bucket);
        storageService.createBucket(bucket);
        return Result.success(null, "创建存储桶成功");
    }
}

