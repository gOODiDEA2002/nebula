package io.nebula.example.modules.messaging.event.delay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券过期事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponExpireEvent implements Serializable {
    private Long couponId;
    private Long userId;
    private LocalDateTime expireTime;
}

