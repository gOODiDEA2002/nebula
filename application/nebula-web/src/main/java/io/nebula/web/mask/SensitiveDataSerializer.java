package io.nebula.web.mask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 敏感数据序列化器
 * 用于在 JSON 序列化时自动脱敏
 * 
 * @author nebula
 */
public class SensitiveDataSerializer extends JsonSerializer<String> {
    
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
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (!enabled || strategyManager == null) {
            gen.writeString(value);
            return;
        }
        
        String maskedValue = strategyManager.mask(value, maskType, customStrategy);
        gen.writeString(maskedValue);
    }
}
