package io.nebula.example.modules.data.controller;

import io.nebula.core.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

/**
 * 数据库连接测试控制器
 */
@RestController
@RequestMapping("/db-test")
@RequiredArgsConstructor
@Slf4j
public class DatabaseTestController {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/connection")
    public Result<String> testConnection() {
        try {
            String result = jdbcTemplate.queryForObject("SELECT 'Database connected successfully!' as message", String.class);
            return Result.success(result, "数据库连接测试成功");
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            return Result.systemError("数据库连接测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/tables")
    public Result<Object> listTables() {
        try {
            var tables = jdbcTemplate.queryForList("SHOW TABLES");
            return Result.success(tables, "获取表列表成功");
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            return Result.systemError("获取表列表失败: " + e.getMessage());
        }
    }

}
