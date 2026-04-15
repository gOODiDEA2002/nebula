package io.nebula.example.modules.storage.service;

import io.nebula.example.modules.storage.entity.dto.*;

/**
 * 存储演示服务接口
 */
public interface StorageDemoService {
    
    /**
     * 上传文件
     * 
     * @param request 上传请求
     * @return 上传结果
     */
    UploadFileDto.Response uploadFile(UploadFileDto.Request request);
    
    /**
     * 下载文件（获取元数据）
     * 
     * @param request 下载请求
     * @return 下载结果
     */
    DownloadFileDto.Response downloadFile(DownloadFileDto.Request request);
    
    /**
     * 列出文件
     * 
     * @param request 列表请求
     * @return 文件列表
     */
    ListFilesDto.Response listFiles(ListFilesDto.Request request);
    
    /**
     * 删除文件
     * 
     * @param request 删除请求
     * @return 删除结果
     */
    DeleteFileDto.Response deleteFile(DeleteFileDto.Request request);
    
    /**
     * 生成预签名URL
     * 
     * @param request 生成URL请求
     * @return 预签名URL
     */
    GenerateUrlDto.Response generatePresignedUrl(GenerateUrlDto.Request request);
}

