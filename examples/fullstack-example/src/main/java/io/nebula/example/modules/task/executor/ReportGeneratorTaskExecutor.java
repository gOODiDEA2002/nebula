package io.nebula.example.modules.task.executor;

import io.nebula.task.core.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 报表生成任务执行器
 * 演示通过 @TaskHandler 注解注册任务
 */
@Slf4j
@TaskHandler(value = "reportGeneratorTask", description = "生成日报/周报/月报（演示用）")
public class ReportGeneratorTaskExecutor implements TaskExecutor {

    @Override
    public String getExecutorName() {
        return "reportGeneratorTask";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        String reportType = context.getStringParameter("reportType", "daily");
        log.info("[报表生成] 任务开始, reportType={}, taskId={}", reportType, context.getTaskId());

        int orderCount = ThreadLocalRandom.current().nextInt(100, 1000);
        double totalRevenue = ThreadLocalRandom.current().nextDouble(10000, 100000);
        int userCount = ThreadLocalRandom.current().nextInt(10, 200);

        log.info("[报表生成] 报表类型={}, 订单数={}, 营收={}, 新用户={}",
                reportType, orderCount, String.format("%.2f", totalRevenue), userCount);

        return TaskResult.success(
                context.getTaskId(),
                context.getTaskName(),
                String.format("%s报表生成完成", reportType),
                Map.of(
                        "reportType", reportType,
                        "reportDate", LocalDate.now().toString(),
                        "orderCount", orderCount,
                        "totalRevenue", String.format("%.2f", totalRevenue),
                        "newUserCount", userCount,
                        "generatedAt", LocalDateTime.now().toString()
                )
        );
    }
}
