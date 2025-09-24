package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * 地址脱敏策略
 * 北京市***区
 * 
 * @author nebula
 */
public class AddressMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        String address = original.trim();
        
        // 简单的地址脱敏：保留前缀，隐藏中间详细信息
        if (address.length() <= 6) {
            return address.substring(0, Math.min(2, address.length())) + "***";
        } else if (address.length() <= 12) {
            return address.substring(0, 3) + "***" + address.substring(address.length() - 2);
        } else {
            // 对于较长的地址，保留前面省市信息，隐藏详细地址
            int keepStart = Math.min(6, address.length() / 3);
            int keepEnd = Math.min(3, address.length() / 6);
            return address.substring(0, keepStart) + "***" + 
                   address.substring(address.length() - keepEnd);
        }
    }
}
