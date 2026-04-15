package io.nebula.example.modules.discovery.entity.vo;

import lombok.Data;

import java.util.Map;

/**
 * 服务实例VO
 */
@Data
public class ServiceInstanceVo {
    
    /** 服务名称 */
    private String serviceName;
    
    /** 实例ID */
    private String instanceId;
    
    /** IP地址 */
    private String ip;
    
    /** 端口号 */
    private Integer port;
    
    /** 权重 */
    private Double weight;
    
    /** 是否健康 */
    private Boolean healthy;
    
    /** 是否启用 */
    private Boolean enabled;
    
    /** 集群名称 */
    private String clusterName;
    
    /** 分组名称 */
    private String groupName;
    
    /** 元数据 */
    private Map<String, String> metadata;
    
    /** 服务版本 */
    private String version;
    
    /** 服务协议 */
    private String protocol;
    
    /** 完整地址 */
    private String address;
    
    /** 是否可用 */
    private Boolean available;
}


