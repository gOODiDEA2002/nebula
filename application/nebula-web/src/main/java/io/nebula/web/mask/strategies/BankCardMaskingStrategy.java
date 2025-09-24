package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * 银行卡号脱敏策略
 * 6222****1234
 * 
 * @author nebula
 */
public class BankCardMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        String bankCard = original.trim().replaceAll("\\s+", ""); // 移除空格
        
        // 银行卡号通常是13-19位数字
        if (bankCard.matches("^\\d{13,19}$")) {
            if (bankCard.length() >= 8) {
                return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length() - 4);
            } else if (bankCard.length() >= 6) {
                return bankCard.substring(0, 2) + "****" + bankCard.substring(bankCard.length() - 2);
            }
        }
        
        // 通用处理：前4位+中间用*替换+后4位
        if (bankCard.length() >= 8) {
            return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length() - 4);
        } else if (bankCard.length() >= 4) {
            return bankCard.substring(0, 2) + "**" + bankCard.substring(bankCard.length() - 2);
        }
        
        return "****";
    }
}
