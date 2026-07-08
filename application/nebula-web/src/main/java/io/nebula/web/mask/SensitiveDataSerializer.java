package io.nebula.web.mask;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * 敏感数据序列化器（Jackson 3）
 * 用于在 JSON 序列化时自动脱敏
 * 
 * @author nebula
 */
public class SensitiveDataSerializer extends ValueSerializer<String> {
    
    private final DataMaskingStrategyManager strategyManager;
    private final MaskType maskType;
    private final String customStrategy;
    private final boolean enabled;
    
    public SensitiveDataSerializer(DataMaskingStrategyManager strategyManager, 
                                  MaskType maskType, 
                                  String customStrategy, 
                                  boolean enabled) {
        this.strategyManager = strategyManager;
        this.maskType = maskType;
        this.customStrategy = customStrategy;
        this.enabled = enabled;
    }
    
    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
        if (!enabled || strategyManager == null) {
            gen.writeString(value);
            return;
        }
        
        String maskedValue = strategyManager.mask(value, maskType, customStrategy);
        gen.writeString(maskedValue);
    }
}
