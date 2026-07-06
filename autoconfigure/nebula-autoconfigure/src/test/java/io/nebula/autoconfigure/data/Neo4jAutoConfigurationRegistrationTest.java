package io.nebula.autoconfigure.data;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Neo4j 自动配置已注册进 AutoConfiguration.imports。
 * 修复前两个类带 @AutoConfiguration 却未注册进任何 imports，配 nebula.data.neo4j.enabled=true 也不生效(死功能)。
 */
class Neo4jAutoConfigurationRegistrationTest {

    @Test
    void neo4jAutoConfigurationsAreRegisteredAsCandidates() {
        List<String> candidates = new ArrayList<>();
        ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())
                .forEach(candidates::add);

        assertThat(candidates).contains(
                "io.nebula.autoconfigure.data.Neo4jAutoConfiguration",
                "io.nebula.autoconfigure.data.Neo4jHealthAutoConfiguration");
    }
}
