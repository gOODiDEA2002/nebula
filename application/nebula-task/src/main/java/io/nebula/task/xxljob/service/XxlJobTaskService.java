package io.nebula.task.xxljob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import io.nebula.task.core.TaskResult;
import io.nebula.task.execution.TaskEngine;
import io.nebula.task.xxljob.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * XXL-JOB 任务服务
 * 实现 XXL-JOB 执行器的 HTTP 接口
 */
@RestController
@Component
public class XxlJobTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(XxlJobTaskService.class);
    
    @Autowired
    private TaskEngine taskEngine;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 心跳检测
     */
    @RequestMapping("/beat")
    public XxlJobResult beat(HttpServletRequest request, 
                           @RequestBody(required = false) String data) {
        
        String accessToken = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
        String clientIp = getClientIp(request);
        
        logger.debug("收到心跳请求: clientIp={}, accessToken={}, data={}", clientIp, accessToken, data);
        
        return XxlJobResult.success();
    }
    
    /**
     * 空闲心跳检测
     */
    @RequestMapping("/idleBeat")
    public XxlJobResult idleBeat(HttpServletRequest request, 
                               @RequestBody(required = false) String data) {
        
        String accessToken = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
        String clientIp = getClientIp(request);
        
        logger.debug("收到空闲心跳请求: clientIp={}, accessToken={}, data={}", clientIp, accessToken, data);
        
        try {
            if (data != null && !data.trim().isEmpty()) {
                XxlJobExecuteRequest request1 = objectMapper.readValue(data, XxlJobExecuteRequest.class);
                String taskName = request1.getExecutorHandler();
                
                // 检查任务是否正在运行
                if (taskEngine.isTaskRunning(taskName)) {
                    return XxlJobResult.failure("任务正在运行中");
                }
            }
        } catch (Exception e) {
            logger.warn("解析空闲心跳请求失败: {}", e.getMessage());
        }
        
        return XxlJobResult.success();
    }
    
    /**
     * 执行任务
     */
    @RequestMapping("/run")
    public XxlJobResult run(HttpServletRequest request, 
                          @RequestBody(required = false) String data) {
        
        String accessToken = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
        String clientIp = getClientIp(request);
        
        logger.info("收到任务执行请求: clientIp={}, accessToken={}, data={}", clientIp, accessToken, data);
        
        try {
            if (data == null || data.trim().isEmpty()) {
                return XxlJobResult.failure("请求数据为空");
            }
            
            // 解析请求
            XxlJobExecuteRequest executeRequest = objectMapper.readValue(data, XxlJobExecuteRequest.class);
            String taskName = executeRequest.getExecutorHandler();
            Map<String, Object> parameters = executeRequest.parseParameters();
            int logId = executeRequest.getLogId();
            long logDateTime = executeRequest.getLogDateTime();
            
            logger.info("开始执行任务: taskName={}, logId={}, parameters={}", taskName, logId, parameters);
            
            // 异步执行任务
            taskEngine.executeAsync(taskName, parameters, logId, logDateTime);
            
            return XxlJobResult.success("任务已提交执行");
            
        } catch (Exception e) {
            logger.error("执行任务失败: {}", e.getMessage(), e);
            return XxlJobResult.failure("任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取任务日志
     */
    @RequestMapping("/log")
    public XxlJobLogResult log(HttpServletRequest request, 
                             @RequestBody(required = false) String data) {
        
        String accessToken = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
        String clientIp = getClientIp(request);
        
        logger.debug("收到日志查询请求: clientIp={}, accessToken={}, data={}", clientIp, accessToken, data);
        
        try {
            if (data == null || data.trim().isEmpty()) {
                return XxlJobLogResult.failure("请求数据为空");
            }
            
            // 解析请求
            XxlJobLogRequest logRequest = objectMapper.readValue(data, XxlJobLogRequest.class);
            int logId = logRequest.getLogId();
            
            // 获取日志内容
            String logContent = taskEngine.getTaskLog(logId);
            int logLines = taskEngine.getTaskLogLines(logId);
            
            if (logContent == null) {
                return XxlJobLogResult.failure("日志不存在");
            }
            
            return XxlJobLogResult.success(logContent, logLines);
            
        } catch (Exception e) {
            logger.error("获取任务日志失败: {}", e.getMessage(), e);
            return XxlJobLogResult.failure("获取日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 终止任务
     */
    @RequestMapping("/kill")
    public XxlJobResult kill(HttpServletRequest request, 
                           @RequestBody(required = false) String data) {
        
        String accessToken = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
        String clientIp = getClientIp(request);
        
        logger.info("收到任务终止请求: clientIp={}, accessToken={}, data={}", clientIp, accessToken, data);
        
        // XXL-JOB 的终止功能，暂时不实现
        return XxlJobResult.success("终止请求已接收");
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        return clientIp;
    }
}
