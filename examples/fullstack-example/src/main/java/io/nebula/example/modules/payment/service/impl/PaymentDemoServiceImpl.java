package io.nebula.example.modules.payment.service.impl;

import io.nebula.example.modules.payment.entity.dto.CreatePaymentDto;
import io.nebula.example.modules.payment.entity.dto.QueryPaymentDto;
import io.nebula.example.modules.payment.entity.dto.RefundPaymentDto;
import io.nebula.example.modules.payment.service.PaymentDemoService;
import io.nebula.integration.payment.core.PaymentService;
import io.nebula.integration.payment.core.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付演示服务实现
 * 依赖 PaymentService（由自动配置注入 MockPaymentService）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDemoServiceImpl implements PaymentDemoService {

    private final PaymentService paymentService;

    @Override
    public PaymentResponse createPayment(CreatePaymentDto dto) {
        log.info("[支付演示] 创建支付订单: outTradeNo={}, amount={}, subject={}",
                dto.getOutTradeNo(), dto.getAmount(), dto.getSubject());

        PaymentRequest request = PaymentRequest.builder()
                .outTradeNo(dto.getOutTradeNo())
                .amount(dto.getAmount())
                .subject(dto.getSubject())
                .paymentType(dto.getPaymentType() != null
                        ? PaymentType.fromCode(dto.getPaymentType())
                        : PaymentType.WEB)
                .build();

        PaymentResponse response = paymentService.createPayment(request);
        log.info("[支付演示] 支付订单创建结果: success={}, tradeNo={}",
                response.isSuccess(), response.getTradeNo());
        return response;
    }

    @Override
    public PaymentQueryResponse queryPayment(QueryPaymentDto dto) {
        log.info("[支付演示] 查询支付状态: outTradeNo={}", dto.getOutTradeNo());

        PaymentQuery query = PaymentQuery.builder()
                .outTradeNo(dto.getOutTradeNo())
                .tradeNo(dto.getTradeNo())
                .build();

        return paymentService.queryPayment(query);
    }

    @Override
    public RefundResponse refund(RefundPaymentDto dto) {
        log.info("[支付演示] 申请退款: outTradeNo={}, refundAmount={}, reason={}",
                dto.getOutTradeNo(), dto.getRefundAmount(), dto.getReason());

        RefundRequest request = RefundRequest.builder()
                .outTradeNo(dto.getOutTradeNo())
                .outRefundNo(dto.getOutRefundNo())
                .refundAmount(dto.getRefundAmount())
                .reason(dto.getReason())
                .build();

        RefundResponse response = paymentService.refund(request);
        log.info("[支付演示] 退款结果: success={}, refundNo={}",
                response.isSuccess(), response.getRefundNo());
        return response;
    }

    @Override
    public Map<String, Object> checkServiceStatus() {
        return Map.of(
                "available", paymentService.isAvailable(),
                "provider", paymentService.getProvider().getName(),
                "providerCode", paymentService.getProvider().getCode()
        );
    }
}
