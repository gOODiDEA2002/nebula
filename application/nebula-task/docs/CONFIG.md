# Nebula Task 配置指南

> 任务调度模块配置说明

## 概述

`nebula-task` 提供任务调度能力,集成 XXL-Job。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-task</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### XXL-Job配置

```yaml
nebula:
  task:
    xxl-job:
      enabled: true
      admin-addresses: http://xxl-job-admin:8080/xxl-job-admin
      app-name: ticket-service
      access-token: ${XXL_JOB_TOKEN}
      # 执行器配置
      executor:
        ip: 
        port: 9999
        log-path: /data/applogs/xxl-job
        log-retention-days: 30
```

## 票务系统场景

### 定时任务

```java
@Component
public class OrderTimeoutTask {
    
    @XxlJob("cancelExpiredOrders")
    public void cancelExpiredOrders() {
        log.info("开始取消超时订单");
        
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByStatusAndExpireTimeBefore(
            "PENDING", now
        );
        
        for (Order order : expiredOrders) {
            orderService.cancelOrder(order.getOrderNo());
        }
        
        log.info("取消超时订单完成: count={}", expiredOrders.size());
    }
}
```

---

**最后更新**: 2025-11-20

