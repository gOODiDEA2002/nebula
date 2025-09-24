package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * 密码脱敏策略
 * 完全隐藏：******
 * 
 * @author nebula
 */
public class PasswordMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        // 密码完全脱敏，只显示固定长度的*
        return "******";
    }
}
