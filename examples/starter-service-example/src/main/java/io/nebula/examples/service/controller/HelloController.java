package io.nebula.examples.service.controller;

import io.nebula.core.common.result.Result;
import io.nebula.examples.service.api.ServiceInfoDto;
import io.nebula.examples.service.rpc.HelloRpcClientImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向 HTTP 调用方的示例接口，内部复用同一份 RPC 服务实现。
 */
@RestController
@RequestMapping("/rpc/hello")
public class HelloController {

    private final HelloRpcClientImpl helloService;

    public HelloController(HelloRpcClientImpl helloService) {
        this.helloService = helloService;
    }

    @GetMapping
    public Result<String> hello() {
        return Result.success(helloService.hello());
    }

    @GetMapping("/greet")
    public Result<String> greet(@RequestParam String name) {
        return Result.success(helloService.greet(name));
    }

    @GetMapping("/info")
    public Result<ServiceInfoDto> info() {
        return Result.success(helloService.getServiceInfo());
    }
}
