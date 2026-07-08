package io.nebula.web.mask;

import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;

/**
 * 敏感数据注解内省器（Jackson 3）
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
    public Object findSerializer(MapperConfig<?> config, Annotated annotated) {
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
