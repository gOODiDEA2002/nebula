package io.nebula.examples.service.controller;

import io.nebula.core.common.result.Result;
import io.nebula.lock.LockManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 分布式锁最小用法示例。
 */
@RestController
@RequestMapping("/lock")
public class LockController {

    private final LockManager lockManager;

    public LockController(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    @GetMapping("/execute")
    public Result<Map<String, Object>> execute(@RequestParam String key) {
        Map<String, Object> result = lockManager.execute(key, () -> Map.of(
                "key", key,
                "executed", true));
        return Result.success(result);
    }
}
