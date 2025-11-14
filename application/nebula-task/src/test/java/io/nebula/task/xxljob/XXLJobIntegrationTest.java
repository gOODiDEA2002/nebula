package io.nebula.task.xxljob;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.task.execution.TaskEngine;
import io.nebula.task.xxljob.dto.*;
import io.nebula.task.xxljob.service.XxlJobTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * XXLJob 集成测试类
 * 测试 XXL-JOB 执行器的 HTTP 接口功能
 */
@ExtendWith(MockitoExtension.class)
class XXLJobIntegrationTest {

    @Mock
    private TaskEngine taskEngine;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private XxlJobTaskService xxlJobTaskService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(xxlJobTaskService, "objectMapper", objectMapper);
        
        // 设置默认的请求头和客户端IP
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("X-Real-IP")).thenReturn(null);
    }

    /**
     * 测试心跳检测
     */
    @Test
    void testBeat() {
        XxlJobResult result = xxlJobTaskService.beat(request, null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.isSuccess()).isTrue();
    }

    /**
     * 测试空闲心跳检测（无数据）
     */
    @Test
    void testIdleBeatWithNoData() {
        XxlJobResult result = xxlJobTaskService.idleBeat(request, null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.isSuccess()).isTrue();
    }

    /**
     * 测试空闲心跳检测（任务空闲）
     */
    @Test
    void testIdleBeatWhenTaskIdle() throws Exception {
        // 准备测试数据
        XxlJobExecuteRequest executeRequest = new XxlJobExecuteRequest();
        executeRequest.setExecutorHandler("test-task");
        String jsonData = objectMapper.writeValueAsString(executeRequest);
        
        // 模拟任务未运行
        when(taskEngine.isTaskRunning("test-task")).thenReturn(false);
        
        // 执行空闲心跳检测
        XxlJobResult result = xxlJobTaskService.idleBeat(request, jsonData);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.isSuccess()).isTrue();
        
        // 验证调用
        verify(taskEngine, times(1)).isTaskRunning("test-task");
    }

    /**
     * 测试空闲心跳检测（任务运行中）
     */
    @Test
    void testIdleBeatWhenTaskRunning() throws Exception {
        // 准备测试数据
        XxlJobExecuteRequest executeRequest = new XxlJobExecuteRequest();
        executeRequest.setExecutorHandler("test-task");
        String jsonData = objectMapper.writeValueAsString(executeRequest);
        
        // 模拟任务正在运行
        when(taskEngine.isTaskRunning("test-task")).thenReturn(true);
        
        // 执行空闲心跳检测
        XxlJobResult result = xxlJobTaskService.idleBeat(request, jsonData);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("任务正在运行中");
        
        // 验证调用
        verify(taskEngine, times(1)).isTaskRunning("test-task");
    }

    /**
     * 测试执行任务（无数据）
     */
    @Test
    void testRunWithNoData() {
        XxlJobResult result = xxlJobTaskService.run(request, null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("请求数据为空");
    }

    /**
     * 测试执行任务（正常执行）
     */
    @Test
    void testRunTask() throws Exception {
        // 准备测试数据
        XxlJobExecuteRequest executeRequest = new XxlJobExecuteRequest();
        executeRequest.setExecutorHandler("test-task");
        executeRequest.setExecutorParams("{\"param1\":\"value1\",\"param2\":123}");
        executeRequest.setLogId(1001);
        executeRequest.setLogDateTime(System.currentTimeMillis());
        String jsonData = objectMapper.writeValueAsString(executeRequest);
        
        // 执行任务
        XxlJobResult result = xxlJobTaskService.run(request, jsonData);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("任务已提交执行");
        
        // 验证任务引擎被调用
        verify(taskEngine, times(1)).executeAsync(
                eq("test-task"),
                any(Map.class),
                eq(1001),
                anyLong()
        );
    }

    /**
     * 测试执行任务（异常处理）
     */
    @Test
    void testRunTaskWithException() throws Exception {
        // 准备测试数据
        XxlJobExecuteRequest executeRequest = new XxlJobExecuteRequest();
        executeRequest.setExecutorHandler("test-task");
        executeRequest.setExecutorParams("{}");
        executeRequest.setLogId(1001);
        executeRequest.setLogDateTime(System.currentTimeMillis());
        String jsonData = objectMapper.writeValueAsString(executeRequest);
        
        // 模拟任务引擎抛出异常
        doThrow(new RuntimeException("执行失败")).when(taskEngine).executeAsync(
                anyString(),
                any(Map.class),
                anyInt(),
                anyLong()
        );
        
        // 执行任务
        XxlJobResult result = xxlJobTaskService.run(request, jsonData);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).contains("任务执行失败");
    }

    /**
     * 测试获取任务日志（无数据）
     */
    @Test
    void testLogWithNoData() {
        XxlJobLogResult result = xxlJobTaskService.log(request, null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("请求数据为空");
    }

    /**
     * 测试获取任务日志（正常获取）
     */
    @Test
    void testGetTaskLog() throws Exception {
        // 准备测试数据
        XxlJobLogRequest logRequest = new XxlJobLogRequest();
        logRequest.setLogId(1001);
        String jsonData = objectMapper.writeValueAsString(logRequest);
        
        // 模拟日志内容
        when(taskEngine.getTaskLog(1001)).thenReturn("任务执行日志内容");
        when(taskEngine.getTaskLogLines(1001)).thenReturn(10);
        
        // 获取日志
        XxlJobLogResult result = xxlJobTaskService.log(request, jsonData);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent().getLogContent()).isEqualTo("任务执行日志内容");
        assertThat(result.getContent().getToLineNum()).isEqualTo(10);
        
        // 验证调用
        verify(taskEngine, times(1)).getTaskLog(1001);
        verify(taskEngine, times(1)).getTaskLogLines(1001);
    }

    /**
     * 测试获取任务日志（日志不存在）
     */
    @Test
    void testGetTaskLogNotFound() throws Exception {
        // 准备测试数据
        XxlJobLogRequest logRequest = new XxlJobLogRequest();
        logRequest.setLogId(1001);
        String jsonData = objectMapper.writeValueAsString(logRequest);
        
        // 模拟日志不存在
        when(taskEngine.getTaskLog(1001)).thenReturn(null);
        
        // 获取日志
        XxlJobLogResult result = xxlJobTaskService.log(request, jsonData);
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("日志不存在");
        
        // 验证调用
        verify(taskEngine, times(1)).getTaskLog(1001);
    }

    /**
     * 测试终止任务
     */
    @Test
    void testKillTask() {
        XxlJobResult result = xxlJobTaskService.kill(request, null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("终止请求已接收");
    }

    /**
     * 测试获取客户端IP（直接获取）
     */
    @Test
    void testGetClientIpDirect() {
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        
        XxlJobResult result = xxlJobTaskService.beat(request, null);
        
        assertThat(result).isNotNull();
        verify(request, atLeastOnce()).getRemoteAddr();
    }

    /**
     * 测试获取客户端IP（通过X-Forwarded-For）
     */
    @Test
    void testGetClientIpFromForwardedFor() {
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        XxlJobResult result = xxlJobTaskService.beat(request, null);
        
        assertThat(result).isNotNull();
    }

    /**
     * 测试获取客户端IP（通过X-Real-IP）
     */
    @Test
    void testGetClientIpFromRealIp() {
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("X-Real-IP")).thenReturn("172.16.0.1");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        XxlJobResult result = xxlJobTaskService.beat(request, null);
        
        assertThat(result).isNotNull();
    }
}

