package io.nebula.example.modules.payment.service;

import io.nebula.example.modules.payment.entity.dto.CreatePaymentDto;
import io.nebula.example.modules.payment.entity.dto.QueryPaymentDto;
import io.nebula.example.modules.payment.entity.dto.RefundPaymentDto;
import io.nebula.integration.payment.core.model.PaymentQueryResponse;
import io.nebula.integration.payment.core.model.PaymentResponse;
import io.nebula.integration.payment.core.model.RefundResponse;

import java.util.Map;

/**
 * 支付演示服务接口
 */
public interface PaymentDemoService {

    PaymentResponse createPayment(CreatePaymentDto dto);

    PaymentQueryResponse queryPayment(QueryPaymentDto dto);

    RefundResponse refund(RefundPaymentDto dto);

    Map<String, Object> checkServiceStatus();
}
