package io.nebula.example.modules.task.controller;

import io.nebula.core.common.result.Result;
import io.nebula.task.core.TaskResult;
import io.nebula.task.execution.TaskEngine;
import io.nebula.task.execution.TaskRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 任务调度演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    @Autowired(required = false)
    private TaskRegistry taskRegistry;

    @Autowired(required = false)
    private TaskEngine taskEngine;

    /**
     * 查看已注册的任务列表
     */
    @GetMapping("/executors")
    public Result<List<Map<String, String>>> listExecutors() {
        if (taskRegistry == null) {
            return Result.error("503", "任务模块未启用，请配置 nebula.task.enabled=true");
        }

        List<Map<String, String>> executors = taskRegistry.getAllExecutors().stream()
                .map(e -> Map.of(
                        "name", e.getExecutorName(),
                        "description", e.getDescription()
                ))
                .toList();

        return Result.success(executors);
    }

    /**
     * 手动触发任务（同步执行）
     */
    @PostMapping("/execute/{executorName}")
    public Result<TaskResult> executeTask(
            @PathVariable String executorName,
            @RequestBody(required = false) Map<String, Object> params) {

        if (taskEngine == null) {
            return Result.error("503", "任务模块未启用，请配置 nebula.task.enabled=true");
        }

        int logId = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        TaskResult result = taskEngine.executeSync(
                executorName,
                params != null ? params : new HashMap<>(),
                logId,
                System.currentTimeMillis()
        );
        return Result.success(result);
    }
}
