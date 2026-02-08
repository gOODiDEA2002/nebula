package io.nebula.example.rpc.async.client.controller.vo;

import io.nebula.example.rpc.async.api.dto.ProcessRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量任务请求
 * 
 * @author Nebula Framework
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTaskRequest {
    
    /**
     * 任务请求列表
     */
    private List<ProcessRequest> requests;
}
