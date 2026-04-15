package io.nebula.example.modules.data.service;

import io.nebula.example.modules.data.entity.dto.CreateReadWriteProductDto;
import io.nebula.example.modules.data.entity.dto.GetReadWriteProductDto;
import io.nebula.example.modules.data.entity.dto.UpdateReadWriteProductDto;

/**
 * 读写分离演示服务接口
 * 演示 Nebula 数据访问层读写分离功能，严格遵循 DTO 规范
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface ReadWriteDemoService {

    /**
     * 创建产品（读写分离演示）
     * 使用写数据源进行产品创建操作
     *
     * @param request 创建产品请求
     * @return 创建产品响应
     */
    CreateReadWriteProductDto.Response createProduct(CreateReadWriteProductDto.Request request);

    /**
     * 获取产品详情（读写分离演示）
     * 使用读数据源进行产品查询操作
     *
     * @param request 获取产品请求
     * @return 获取产品响应
     */
    GetReadWriteProductDto.Response getProductById(GetReadWriteProductDto.Request request);

    /**
     * 更新产品（读写分离演示）
     * 使用写数据源进行产品更新操作
     *
     * @param request 更新产品请求
     * @return 更新产品响应
     */
    UpdateReadWriteProductDto.Response updateProduct(UpdateReadWriteProductDto.Request request);
}