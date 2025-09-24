package io.nebula.web.mask.strategies;

import io.nebula.web.mask.DataMaskingStrategy;
import org.springframework.util.StringUtils;

/**
 * 身份证号脱敏策略
 * 110***********1234
 * 
 * @author nebula
 */
public class IdCardMaskingStrategy implements DataMaskingStrategy {
    
    @Override
    public String mask(String original) {
        if (!StringUtils.hasText(original)) {
            return original;
        }
        
        String idCard = original.trim();
        
        // 中国身份证号（18位或15位）
        if (idCard.matches("^\\d{15}$") || idCard.matches("^\\d{17}[\\dXx]$")) {
            if (idCard.length() == 18) {
                return idCard.substring(0, 3) + "***********" + idCard.substring(14);
            } else if (idCard.length() == 15) {
                return idCard.substring(0, 3) + "********" + idCard.substring(11);
            }
        }
        
        // 通用处理：前3位+中间用*替换+后4位
        if (idCard.length() >= 7) {
            int prefixLength = Math.min(3, idCard.length() / 4);
            int suffixLength = Math.min(4, idCard.length() / 4);
            StringBuilder sb = new StringBuilder();
            sb.append(idCard, 0, prefixLength);
            for (int i = prefixLength; i < idCard.length() - suffixLength; i++) {
                sb.append('*');
            }
            if (suffixLength > 0) {
                sb.append(idCard.substring(idCard.length() - suffixLength));
            }
            return sb.toString();
        }
        
        return "****";
    }
}
