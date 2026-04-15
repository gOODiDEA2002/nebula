package io.nebula.example.modules.messaging.event.delay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单超时事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutEvent implements Serializable {
    private Long orderId;
    private LocalDateTime createTime;
}

