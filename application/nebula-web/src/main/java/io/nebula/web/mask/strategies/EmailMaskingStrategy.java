package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * 邮箱脱敏策略
 * test***@example.com
 * 
 * @author nebula
 */
public class EmailMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        String email = original.trim();
        int atIndex = email.indexOf('@');
        
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            // 不是有效的邮箱格式，简单脱敏
            return maskSimple(email);
        }
        
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        // 用户名脱敏
        String maskedUsername;
        if (username.length() <= 1) {
            maskedUsername = "*";
        } else if (username.length() <= 3) {
            maskedUsername = username.charAt(0) + "*";
        } else {
            int visibleCount = Math.max(1, username.length() / 4);
            maskedUsername = username.substring(0, visibleCount) + "***";
        }
        
        return maskedUsername + domain;
    }
    
    private String maskSimple(String text) {
        if (text.length() <= 1) {
            return "*";
        } else if (text.length() <= 3) {
            return text.charAt(0) + "*";
        } else {
            return text.charAt(0) + "***" + text.charAt(text.length() - 1);
        }
    }
}
