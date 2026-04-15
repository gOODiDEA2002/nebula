package io.nebula.examples.web.controller;

import io.nebula.core.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello, Nebula Web");
    }
}
