package io.nebula.example.modules.payment.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.payment.entity.dto.CreatePaymentDto;
import io.nebula.example.modules.payment.entity.dto.QueryPaymentDto;
import io.nebula.example.modules.payment.entity.dto.RefundPaymentDto;
import io.nebula.example.modules.payment.service.PaymentDemoService;
import io.nebula.integration.payment.core.model.PaymentQueryResponse;
import io.nebula.integration.payment.core.model.PaymentResponse;
import io.nebula.integration.payment.core.model.RefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentDemoService paymentDemoService;

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    public Result<PaymentResponse> createPayment(@RequestBody CreatePaymentDto dto) {
        return Result.success(paymentDemoService.createPayment(dto));
    }

    /**
     * 查询支付状态
     */
    @PostMapping("/query")
    public Result<PaymentQueryResponse> queryPayment(@RequestBody QueryPaymentDto dto) {
        return Result.success(paymentDemoService.queryPayment(dto));
    }

    /**
     * 申请退款
     */
    @PostMapping("/refund")
    public Result<RefundResponse> refund(@RequestBody RefundPaymentDto dto) {
        return Result.success(paymentDemoService.refund(dto));
    }

    /**
     * 检查支付服务可用性
     */
    @GetMapping("/status")
    public Result<Object> checkStatus() {
        return Result.success(paymentDemoService.checkServiceStatus());
    }
}
