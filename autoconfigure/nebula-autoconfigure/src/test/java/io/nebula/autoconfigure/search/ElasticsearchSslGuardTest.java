package io.nebula.autoconfigure.search;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 验证 ES trust-all 在生产环境被拦截:
 * 1) prod profile + sslVerificationEnabled=false -> createSSLContext 返回默认 SSLContext(不是 trust-all)
 * 2) dev profile -> 返回 trust-all SSLContext
 * 3) 无 activeProfile -> 视为非生产, 允许 trust-all
 */
class ElasticsearchSslGuardTest {

    @Test
    void prodProfile_trustAllIgnored() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var props = buildProps(false);

        var config = new ElasticsearchAutoConfiguration(props, env);
        javax.net.ssl.SSLContext ctx = invokeCreateSSLContext(config);

        assertThat(ctx).isEqualTo(javax.net.ssl.SSLContext.getDefault());
    }

    @Test
    void productionProfile_trustAllIgnored() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        var props = buildProps(false);

        var config = new ElasticsearchAutoConfiguration(props, env);
        javax.net.ssl.SSLContext ctx = invokeCreateSSLContext(config);

        assertThat(ctx).isEqualTo(javax.net.ssl.SSLContext.getDefault());
    }

    @Test
    void devProfile_trustAllAllowed() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        var props = buildProps(false);

        var config = new ElasticsearchAutoConfiguration(props, env);
        javax.net.ssl.SSLContext ctx = invokeCreateSSLContext(config);

        assertThat(ctx).isNotEqualTo(javax.net.ssl.SSLContext.getDefault());
    }

    @Test
    void noProfile_trustAllAllowed() throws Exception {
        MockEnvironment env = new MockEnvironment();
        var props = buildProps(false);

        var config = new ElasticsearchAutoConfiguration(props, env);
        javax.net.ssl.SSLContext ctx = invokeCreateSSLContext(config);

        assertThat(ctx).isNotEqualTo(javax.net.ssl.SSLContext.getDefault());
    }

    @Test
    void sslVerificationEnabled_normalBehavior() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var props = buildProps(true);

        var config = new ElasticsearchAutoConfiguration(props, env);
        assertThatCode(() -> invokeCreateSSLContext(config)).doesNotThrowAnyException();
    }

    private io.nebula.search.elasticsearch.config.ElasticsearchProperties buildProps(boolean sslVerificationEnabled) {
        var props = new io.nebula.search.elasticsearch.config.ElasticsearchProperties();
        props.setSslVerificationEnabled(sslVerificationEnabled);
        return props;
    }

    private javax.net.ssl.SSLContext invokeCreateSSLContext(ElasticsearchAutoConfiguration config) throws Exception {
        Method method = ElasticsearchAutoConfiguration.class.getDeclaredMethod("createSSLContext");
        method.setAccessible(true);
        return (javax.net.ssl.SSLContext) method.invoke(config);
    }
}
