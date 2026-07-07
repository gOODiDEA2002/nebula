package io.nebula.task.scheduled;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Timed Task Job Handler
 * 
 * This class serves as the unified task handler for XXL-JOB.
 *
 * <p>失败上报语义: 任一子任务抛异常, 该轮调度返回 FAIL 并在消息中列出失败任务
 * (此前吞异常恒报 SUCCESS, 调度中心的失败告警/重试策略形同虚设)。
 * 单个子任务失败不中断同批其余任务的执行。</p>
 *
 * @author Nebula Framework
 */
@Slf4j
public class TimedTaskJobHandler {

    @Autowired(required = false)
    private List<EveryMinuteExecute> everyMinuteExecutes;

    @Autowired(required = false)
    private List<EveryFiveMinuteExecute> everyFiveMinuteExecutes;

    @Autowired(required = false)
    private List<EveryHourExecute> everyHourExecutes;

    @Autowired(required = false)
    private List<EveryDayExecute> everyDayExecutes;

    /**
     * Every minute task scheduler
     */
    @XxlJob("everyMinuteExecuteJobHandler")
    public ReturnT<String> everyMinuteExecuteJobHandler(String param) {
        return executeAll("every minute", everyMinuteExecutes, EveryMinuteExecute::execute);
    }

    /**
     * Every 5 minutes task scheduler
     */
    @XxlJob("everyFiveMinuteExecuteJobHandler")
    public ReturnT<String> everyFiveMinuteExecuteJobHandler(String param) {
        return executeAll("every 5 minutes", everyFiveMinuteExecutes, EveryFiveMinuteExecute::execute);
    }

    /**
     * Every hour task scheduler
     */
    @XxlJob("everyHourExecuteJobHandler")
    public ReturnT<String> everyHourExecuteJobHandler(String param) {
        return executeAll("every hour", everyHourExecutes, EveryHourExecute::execute);
    }

    /**
     * Every day task scheduler
     */
    @XxlJob("everyDayExecuteJobHandler")
    public ReturnT<String> everyDayExecuteJobHandler(String param) {
        return executeAll("every day", everyDayExecutes, EveryDayExecute::execute);
    }

    /**
     * 逐个执行子任务并汇总结果: 全部成功返回 SUCCESS;
     * 任一失败继续执行剩余任务, 最终返回 FAIL 并列出失败任务类名
     */
    private <T> ReturnT<String> executeAll(String jobName, List<T> tasks, TaskInvoker<T> invoker) {
        log.info("{} task started", jobName);
        if (tasks == null || tasks.isEmpty()) {
            log.info("No {} tasks registered", jobName);
            return new ReturnT<>(ReturnT.SUCCESS_CODE, "SUCCESS");
        }

        List<String> failedTasks = new ArrayList<>();
        for (T task : tasks) {
            try {
                invoker.invoke(task);
            } catch (Exception e) {
                failedTasks.add(task.getClass().getSimpleName());
                log.error("Error executing {} task: {}", jobName, task.getClass().getSimpleName(), e);
            }
        }

        if (!failedTasks.isEmpty()) {
            String message = String.format("%d/%d tasks failed: %s",
                    failedTasks.size(), tasks.size(), String.join(", ", failedTasks));
            log.warn("{} task completed with failures: {}", jobName, message);
            return new ReturnT<>(ReturnT.FAIL_CODE, message);
        }

        log.info("{} task completed", jobName);
        return new ReturnT<>(ReturnT.SUCCESS_CODE, "SUCCESS");
    }

    /**
     * 子任务调用函数式接口（各 Every*Execute 无公共父接口, 以方法引用适配）
     */
    @FunctionalInterface
    private interface TaskInvoker<T> {
        void invoke(T task) throws Exception;
    }
}
