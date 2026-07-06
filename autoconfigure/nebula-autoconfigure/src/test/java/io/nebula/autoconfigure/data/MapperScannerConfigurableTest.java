package io.nebula.autoconfigure.data;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Nebula 的 Mapper 扫描器：
 * (1) 扫描包可配置(不再写死 io.nebula.**.mapper)；
 * (2) markerInterface 用 MyBatis-Plus 原生 BaseMapper，标准 MP Mapper 无需继承 nebula BaseMapper 即可被扫描。
 * <p>
 * 这是老项目(如 proud-day，33 个标准 MP Mapper)无痛接入 Nebula 持久化的关键。
 */
class MapperScannerConfigurableTest {

    @Test
    void scansStandardMybatisPlusMapperFromConfiguredPackage() {
        GenericApplicationContext context = new GenericApplicationContext();
        MapperScannerConfigurer configurer = DataPersistenceAutoConfiguration.nebulaMapperScannerConfigurer();
        configurer.setApplicationContext(context);
        // 指向测试 mapper 所在包(模拟应用把扫描包指向自己的包)
        configurer.setBasePackage("io.nebula.autoconfigure.data.mappers");

        configurer.postProcessBeanDefinitionRegistry(context);

        // 该 Mapper 继承的是 MyBatis-Plus 原生 BaseMapper(非 nebula 的)，仍被扫描注册
        assertThat(context.containsBeanDefinition("standardMybatisPlusMapper")).isTrue();
    }
}
