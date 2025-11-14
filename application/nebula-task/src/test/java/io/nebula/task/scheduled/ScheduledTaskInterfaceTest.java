package io.nebula.task.scheduled;

import com.xxl.job.core.biz.model.ReturnT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ScheduledTaskInterface 测试类
 * 测试定时任务调度器接口的功能
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTaskInterfaceTest {

    @InjectMocks
    private TimedTaskJobHandler timedTaskJobHandler;

    private List<EveryMinuteExecute> everyMinuteExecutes;
    private List<EveryFiveMinuteExecute> everyFiveMinuteExecutes;
    private List<EveryHourExecute> everyHourExecutes;
    private List<EveryDayExecute> everyDayExecutes;

    @BeforeEach
    void setUp() {
        everyMinuteExecutes = new ArrayList<>();
        everyFiveMinuteExecutes = new ArrayList<>();
        everyHourExecutes = new ArrayList<>();
        everyDayExecutes = new ArrayList<>();
        
        // 使用反射注入模拟的任务列表
        ReflectionTestUtils.setField(timedTaskJobHandler, "everyMinuteExecutes", everyMinuteExecutes);
        ReflectionTestUtils.setField(timedTaskJobHandler, "everyFiveMinuteExecutes", everyFiveMinuteExecutes);
        ReflectionTestUtils.setField(timedTaskJobHandler, "everyHourExecutes", everyHourExecutes);
        ReflectionTestUtils.setField(timedTaskJobHandler, "everyDayExecutes", everyDayExecutes);
    }

    /**
     * 测试每分钟执行任务（无任务）
     */
    @Test
    void testEveryMinuteExecuteWithNoTasks() {
        ReturnT<String> result = timedTaskJobHandler.everyMinuteExecuteJobHandler(null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每分钟执行任务（有任务）
     */
    @Test
    void testEveryMinuteExecuteWithTasks() {
        // 创建模拟任务
        EveryMinuteExecute task1 = mock(EveryMinuteExecute.class);
        EveryMinuteExecute task2 = mock(EveryMinuteExecute.class);
        everyMinuteExecutes.add(task1);
        everyMinuteExecutes.add(task2);
        
        // 执行任务处理器
        ReturnT<String> result = timedTaskJobHandler.everyMinuteExecuteJobHandler(null);
        
        // 验证任务被执行
        verify(task1, times(1)).execute();
        verify(task2, times(1)).execute();
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每分钟执行任务（异常处理）
     */
    @Test
    void testEveryMinuteExecuteWithException() {
        // 创建会抛出异常的模拟任务
        EveryMinuteExecute task1 = mock(EveryMinuteExecute.class);
        doThrow(new RuntimeException("任务执行失败")).when(task1).execute();
        everyMinuteExecutes.add(task1);
        
        // 执行任务处理器（异常应该被捕获，不影响整体执行）
        ReturnT<String> result = timedTaskJobHandler.everyMinuteExecuteJobHandler(null);
        
        // 验证任务被执行
        verify(task1, times(1)).execute();
        
        // 验证结果（即使任务失败，整体也应该返回成功）
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每5分钟执行任务（无任务）
     */
    @Test
    void testEveryFiveMinuteExecuteWithNoTasks() {
        ReturnT<String> result = timedTaskJobHandler.everyFiveMinuteExecuteJobHandler(null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每5分钟执行任务（有任务）
     */
    @Test
    void testEveryFiveMinuteExecuteWithTasks() {
        // 创建模拟任务
        EveryFiveMinuteExecute task1 = mock(EveryFiveMinuteExecute.class);
        EveryFiveMinuteExecute task2 = mock(EveryFiveMinuteExecute.class);
        everyFiveMinuteExecutes.add(task1);
        everyFiveMinuteExecutes.add(task2);
        
        // 执行任务处理器
        ReturnT<String> result = timedTaskJobHandler.everyFiveMinuteExecuteJobHandler(null);
        
        // 验证任务被执行
        verify(task1, times(1)).execute();
        verify(task2, times(1)).execute();
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每小时执行任务（无任务）
     */
    @Test
    void testEveryHourExecuteWithNoTasks() {
        ReturnT<String> result = timedTaskJobHandler.everyHourExecuteJobHandler(null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每小时执行任务（有任务）
     */
    @Test
    void testEveryHourExecuteWithTasks() {
        // 创建模拟任务
        EveryHourExecute task1 = mock(EveryHourExecute.class);
        EveryHourExecute task2 = mock(EveryHourExecute.class);
        everyHourExecutes.add(task1);
        everyHourExecutes.add(task2);
        
        // 执行任务处理器
        ReturnT<String> result = timedTaskJobHandler.everyHourExecuteJobHandler(null);
        
        // 验证任务被执行
        verify(task1, times(1)).execute();
        verify(task2, times(1)).execute();
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每天执行任务（无任务）
     */
    @Test
    void testEveryDayExecuteWithNoTasks() {
        ReturnT<String> result = timedTaskJobHandler.everyDayExecuteJobHandler(null);
        
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试每天执行任务（有任务）
     */
    @Test
    void testEveryDayExecuteWithTasks() {
        // 创建模拟任务
        EveryDayExecute task1 = mock(EveryDayExecute.class);
        EveryDayExecute task2 = mock(EveryDayExecute.class);
        everyDayExecutes.add(task1);
        everyDayExecutes.add(task2);
        
        // 执行任务处理器
        ReturnT<String> result = timedTaskJobHandler.everyDayExecuteJobHandler(null);
        
        // 验证任务被执行
        verify(task1, times(1)).execute();
        verify(task2, times(1)).execute();
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("SUCCESS");
    }

    /**
     * 测试多任务混合执行
     */
    @Test
    void testMultipleTaskTypesExecution() {
        // 创建各种类型的模拟任务
        EveryMinuteExecute minuteTask = mock(EveryMinuteExecute.class);
        EveryFiveMinuteExecute fiveMinuteTask = mock(EveryFiveMinuteExecute.class);
        EveryHourExecute hourTask = mock(EveryHourExecute.class);
        EveryDayExecute dayTask = mock(EveryDayExecute.class);
        
        everyMinuteExecutes.add(minuteTask);
        everyFiveMinuteExecutes.add(fiveMinuteTask);
        everyHourExecutes.add(hourTask);
        everyDayExecutes.add(dayTask);
        
        // 执行各种任务处理器
        timedTaskJobHandler.everyMinuteExecuteJobHandler(null);
        timedTaskJobHandler.everyFiveMinuteExecuteJobHandler(null);
        timedTaskJobHandler.everyHourExecuteJobHandler(null);
        timedTaskJobHandler.everyDayExecuteJobHandler(null);
        
        // 验证所有任务都被执行
        verify(minuteTask, times(1)).execute();
        verify(fiveMinuteTask, times(1)).execute();
        verify(hourTask, times(1)).execute();
        verify(dayTask, times(1)).execute();
    }
}

