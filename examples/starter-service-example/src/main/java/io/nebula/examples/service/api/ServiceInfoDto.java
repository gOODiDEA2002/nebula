package io.nebula.examples.service.api;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 服务信息DTO
 * 
 * @author Nebula Framework
 */
@Data
public class ServiceInfoDto {
    
    /** 服务名称 */
    private String serviceName;
    
    /** 服务版本 */
    private String version;
    
    /** 启动时间 */
    private LocalDateTime startTime;
    
    /** 服务描述 */
    private String description;
    
    public static ServiceInfoDto create(String serviceName, String version, String description) {
        ServiceInfoDto dto = new ServiceInfoDto();
        dto.setServiceName(serviceName);
        dto.setVersion(version);
        dto.setStartTime(LocalDateTime.now());
        dto.setDescription(description);
        return dto;
    }
}

