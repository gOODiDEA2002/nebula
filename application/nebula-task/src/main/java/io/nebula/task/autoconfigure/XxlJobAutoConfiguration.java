package io.nebula.task.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import io.nebula.task.scheduled.TimedTaskJobHandler;
import io.nebula.task.xxljob.service.XxlJobRegistryService;
import io.nebula.task.xxljob.service.XxlJobTaskService;
import io.nebula.task.xxljob.util.XxlJobHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * XXL-JOB 自动配置类
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({TaskProperties.class})
@ConditionalOnProperty(prefix = "nebula.task.xxl-job", name = "admin-addresses")
public class XxlJobAutoConfiguration {
    
    /**
     * XXL-JOB HTTP 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public XxlJobHttpClient xxlJobHttpClient(ObjectMapper objectMapper) {
        return new XxlJobHttpClient(objectMapper);
    }

    /**
     * XXL-JOB 执行器（EmbedServer）
     *
     * <p>负责监听执行器端口接收管理端调度请求，并扫描注册 @XxlJob 注解的 handler
     * （如 TimedTaskJobHandler 的四个定时入口）。缺少该 Bean 时执行器虽完成注册，
     * 但调度请求无监听方接收，任务永远不会被触发。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.task.xxl-job", name = "enabled", havingValue = "true", matchIfMissing = true)
    public XxlJobSpringExecutor xxlJobSpringExecutor(TaskProperties taskProperties) {
        TaskProperties.XxlJob xxlJob = taskProperties.getXxlJob();
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(xxlJob.getAdminAddresses());
        executor.setAppname(xxlJob.getExecutorName());
        if (StringUtils.hasText(xxlJob.getExecutorIp())) {
            executor.setIp(xxlJob.getExecutorIp());
        }
        executor.setPort(xxlJob.getExecutorPort());
        executor.setAccessToken(xxlJob.getAccessToken());
        executor.setLogPath(xxlJob.getLogPath());
        executor.setLogRetentionDays(xxlJob.getLogRetentionDays());
        log.info("初始化 XXL-JOB 执行器: appname={}, port={}", xxlJob.getExecutorName(), xxlJob.getExecutorPort());
        return executor;
    }
    
    /**
     * 统一定时任务入口（@XxlJob handler）
     *
     * <p>该类虽标注 @Component，但 io.nebula.task 包不在业务应用的组件扫描范围内，
     * 必须由自动配置显式注册，@XxlJob 注解方法才能被 XxlJobSpringExecutor 发现。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public TimedTaskJobHandler timedTaskJobHandler() {
        return new TimedTaskJobHandler();
    }

    /**
     * XXL-JOB 任务服务
     */
    @Bean
    @ConditionalOnMissingBean
    public XxlJobTaskService xxlJobTaskService() {
        return new XxlJobTaskService();
    }
    
    /**
     * XXL-JOB 注册服务（仅在无官方执行器时兜底）
     *
     * <p>XxlJobSpringExecutor 自带注册心跳且使用真实网卡 IP；
     * 本服务用 InetAddress.getLocalHost() 在部分环境会上报 127.0.0.1 等脏地址，
     * 导致管理端路由到错误地址。因此有官方执行器时不再注册。</p>
     */
    @Bean
    @ConditionalOnMissingBean({XxlJobRegistryService.class, XxlJobSpringExecutor.class})
    public XxlJobRegistryService xxlJobRegistryService() {
        return new XxlJobRegistryService();
    }
}
