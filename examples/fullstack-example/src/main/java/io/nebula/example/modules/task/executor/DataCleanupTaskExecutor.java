package io.nebula.example.modules.task.executor;

import io.nebula.task.core.TaskContext;
import io.nebula.task.core.TaskExecutor;
import io.nebula.task.core.TaskResult;
import io.nebula.task.core.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据清理任务执行器
 * 演示通过实现 TaskExecutor 接口注册任务
 */
@Slf4j
@Component
public class DataCleanupTaskExecutor implements TaskExecutor {

    @Override
    public String getExecutorName() {
        return "dataCleanupTask";
    }

    @Override
    public String getDescription() {
        return "清理过期数据（演示用）";
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.SCHEDULED || taskType == TaskType.MANUAL;
    }

    @Override
    public TaskResult execute(TaskContext context) {
        log.info("[数据清理] 任务开始, taskId={}", context.getTaskId());

        int retentionDays = context.getIntParameter("retentionDays", 30);
        String targetTable = context.getStringParameter("targetTable", "all");

        int cleanedCount = ThreadLocalRandom.current().nextInt(50, 500);

        log.info("[数据清理] 清理 {} 天前的 {} 表数据, 清理 {} 条记录",
                retentionDays, targetTable, cleanedCount);

        return TaskResult.success(
                context.getTaskId(),
                context.getTaskName(),
                String.format("清理完成: %d 条记录, 保留 %d 天, 表=%s", cleanedCount, retentionDays, targetTable),
                Map.of(
                        "cleanedCount", cleanedCount,
                        "retentionDays", retentionDays,
                        "targetTable", targetTable,
                        "cleanedAt", LocalDateTime.now().toString()
                )
        );
    }
}
