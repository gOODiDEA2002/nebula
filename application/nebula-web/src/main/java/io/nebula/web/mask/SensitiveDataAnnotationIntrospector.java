package io.nebula.web.mask;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.core.Version;

/**
 * 敏感数据注解内省器
 * 用于 Jackson 自动检测 @SensitiveData 注解并应用相应的序列化器
 * 
 * @author nebula
 */
public class SensitiveDataAnnotationIntrospector extends AnnotationIntrospector {
    
    private final DataMaskingStrategyManager strategyManager;
    
    public SensitiveDataAnnotationIntrospector(DataMaskingStrategyManager strategyManager) {
        this.strategyManager = strategyManager;
    }
    
    @Override
    public Object findSerializer(Annotated annotated) {
        SensitiveData annotation = annotated.getAnnotation(SensitiveData.class);
        if (annotation != null) {
            return new SensitiveDataSerializer(
                strategyManager,
                annotation.type(),
                annotation.strategy(),
                annotation.enabled()
            );
        }
        return null;
    }
    
    @Override
    public Version version() {
        return Version.unknownVersion();
    }
}
