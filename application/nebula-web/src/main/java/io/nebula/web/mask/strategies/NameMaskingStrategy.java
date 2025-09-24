package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * 姓名脱敏策略
 * 张* 或 欧阳*
 * 
 * @author nebula
 */
public class NameMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        String name = original.trim();
        
        if (name.length() <= 1) {
            return "*";
        } else if (name.length() == 2) {
            return name.charAt(0) + "*";
        } else {
            // 保留第一个字符，其余用*替换
            return name.charAt(0) + "*".repeat(name.length() - 1);
        }
    }
}
