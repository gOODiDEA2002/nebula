package io.nebula.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 通知服务配置
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.notification")
public class NotificationProperties {
    
    private boolean enabled = true;
    private Sms sms = new Sms();
    
    @Data
    public static class Sms {
        private String accessKeyId;
        private String accessKeySecret;
        private String signName;
    }
}

