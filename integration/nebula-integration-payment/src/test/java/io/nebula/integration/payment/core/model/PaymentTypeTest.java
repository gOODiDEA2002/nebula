package io.nebula.integration.payment.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTypeTest {

    @Test
    void resolvesCodeCaseInsensitively() {
        assertThat(PaymentType.fromCode("WEB")).isEqualTo(PaymentType.WEB);
        assertThat(PaymentType.fromCode("qr_code")).isEqualTo(PaymentType.QR_CODE);
    }
}
