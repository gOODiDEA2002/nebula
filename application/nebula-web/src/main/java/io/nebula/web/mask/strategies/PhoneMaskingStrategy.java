package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * 手机号脱敏策略
 * 138****8888
 * 
 * @author nebula
 */
public class PhoneMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        String phone = original.trim();
        
        // 中国大陆手机号（11位）
        if (phone.matches("^1[3-9]\\d{9}$")) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        
        // 通用处理：中间用*替换
        if (phone.length() >= 4) {
            int visibleStart = Math.min(3, phone.length() / 3);
            int visibleEnd = Math.min(3, phone.length() / 3);
            StringBuilder sb = new StringBuilder();
            sb.append(phone, 0, visibleStart);
            for (int i = visibleStart; i < phone.length() - visibleEnd; i++) {
                sb.append('*');
            }
            if (visibleEnd > 0) {
                sb.append(phone.substring(phone.length() - visibleEnd));
            }
            return sb.toString();
        }
        
        return "****";
    }
}
