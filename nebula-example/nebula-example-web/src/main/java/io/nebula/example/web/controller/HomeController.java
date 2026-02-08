package io.nebula.example.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 * 
 * 演示 Thymeleaf 模板渲染和 REST API
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@Controller
public class HomeController {

    @Value("${spring.application.name:nebula-example-web}")
    private String applicationName;

    @Value("${nebula.version:2.0.1-SNAPSHOT}")
    private String nebulaVersion;

    /**
     * 首页 - 返回 Thymeleaf 模板
     */
    @GetMapping("/")
    public String index(Model model) {
        log.debug("访问首页");
        
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("nebulaVersion", nebulaVersion);
        model.addAttribute("currentTime", LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        
        return "index";
    }

    /**
     * 关于页面
     */
    @GetMapping("/about")
    public String about(Model model) {
        log.debug("访问关于页面");
        
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("nebulaVersion", nebulaVersion);
        
        return "about";
    }

    /**
     * API: 获取系统信息
     */
    @GetMapping("/api/info")
    @ResponseBody
    public Map<String, Object> info() {
        log.debug("API: 获取系统信息");
        
        Map<String, Object> info = new HashMap<>();
        info.put("applicationName", applicationName);
        info.put("nebulaVersion", nebulaVersion);
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("currentTime", LocalDateTime.now().toString());
        
        return info;
    }

    /**
     * API: Hello 接口
     */
    @GetMapping("/api/hello")
    @ResponseBody
    public Map<String, String> hello() {
        log.debug("API: Hello");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Nebula Framework!");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return response;
    }
}
