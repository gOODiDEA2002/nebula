package io.nebula.web.mask;

/**
 * 数据脱敏策略接口
 * 
 * @author nebula
 */
public interface DataMaskingStrategy {
    
    /**
     * 执行数据脱敏
     * 
     * @param original 原始数据
     * @return 脱敏后的数据
     */
    String mask(String original);
}
