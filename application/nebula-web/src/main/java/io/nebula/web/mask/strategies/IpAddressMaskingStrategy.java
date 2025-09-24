package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * IP地址脱敏策略
 * 192.168.*.*
 * 
 * @author nebula
 */
public class IpAddressMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        String ip = original.trim();
        
        // IPv4地址脱敏
        if (ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }
        
        // IPv6地址脱敏（简单处理）
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            if (parts.length >= 4) {
                return parts[0] + ":" + parts[1] + ":****";
            }
        }
        
        // 通用处理
        if (ip.length() > 6) {
            return ip.substring(0, ip.length() / 2) + "****";
        }
        
        return "****";
    }
}
