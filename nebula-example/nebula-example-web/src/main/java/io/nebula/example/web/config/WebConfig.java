package io.nebula.example.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 添加视图控制器
     * 简单的 URL 到视图的映射，无需创建控制器方法
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 错误页面映射
        registry.addViewController("/error/404").setViewName("error/404");
        registry.addViewController("/error/500").setViewName("error/500");
    }

    /**
     * 静态资源处理
     * Spring Boot 默认已配置，此处可添加额外的资源路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 默认静态资源路径已由 Spring Boot 配置
        // 如需添加额外路径，可在此配置
        // registry.addResourceHandler("/assets/**")
        //         .addResourceLocations("classpath:/assets/");
    }
}
