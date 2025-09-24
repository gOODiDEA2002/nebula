package io.nebula.integration.payment.autoconfigure;

import io.nebula.integration.payment.core.PaymentService;
import io.nebula.integration.payment.provider.mock.MockPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 支付模块自动配置
 *
 * @author nebula
 */
@AutoConfiguration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PaymentAutoConfiguration.class);

    /**
     * Mock 支付服务配置
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "nebula.payment.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(PaymentService.class)
    public PaymentService mockPaymentService() {
        logger.info("Configuring Mock Payment Service");
        return new MockPaymentService();
    }
}
