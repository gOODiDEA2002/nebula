# nebula-messaging-rabbitmq 模块示例

## 模块简介

`nebula-messaging-rabbitmq` 是 Nebula 框架基于 RabbitMQ 的消息实现模块。它实现了 `nebula-messaging-core` 定义的标准接口，并提供了 RabbitMQ 特有的功能支持，如死信队列、延时消息（基于 TTL+DLX）等。

## 核心功能示例

### 1. 配置 RabbitMQ

在 `application.yml` 中配置 RabbitMQ 连接信息和行为参数。

**`application.yml`**:

```yaml
nebula:
  messaging:
    rabbitmq:
      enabled: true
      host: ${RABBITMQ_HOST:localhost}
      port: ${RABBITMQ_PORT:5672}
      username: ${RABBITMQ_USERNAME:guest}
      password: ${RABBITMQ_PASSWORD:guest}
      virtual-host: /
      
      # 生产者配置
      producer:
        publisher-confirms: true # 开启发送确认
        publisher-returns: true  # 开启消息回退
        confirm-timeout: 5000    # 确认超时时间(ms)
      
      # 消费者配置
      consumer:
        auto-ack: false          # 手动ACK (框架自动处理，但底层设置为false)
        prefetch-count: 10       # 预取数量
        retry-count: 3           # 消费失败重试次数
        retry-interval: 2000     # 重试间隔(ms)
      
      # 交换机配置
      exchange:
        default-type: topic      # 默认交换机类型
        durable: true            # 是否持久化
      
      # 延时消息配置 (核心特性)
      delay-message:
        enabled: true
        auto-create-resources: true # 自动创建延时交换机和队列
        max-delay-millis: 604800000 # 最大延时 7 天
        enable-dead-letter-queue: true # 启用死信队列
```

### 2. 启动应用

引入模块后，Spring Boot 会自动配置 `MessageProducer` 和消费者容器。

**`io.nebula.example.rabbitmq.RabbitMQApplication`**:

```java
package io.nebula.example.rabbitmq;

import io.nebula.messaging.core.annotation.EnableMessaging;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMessaging // 启用消息功能
public class RabbitMQApplication {
    public static void main(String[] args) {
        SpringApplication.run(RabbitMQApplication.class, args);
    }
}
```

### 3. 发送延时消息

`nebula-messaging-rabbitmq` 通过 `DelayMessageProducer` 实现了高效的延时消息。

**`io.nebula.example.rabbitmq.service.DelayTaskService`**:

```java
package io.nebula.example.rabbitmq.service;

import io.nebula.messaging.core.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DelayTaskService {

    private final MessageProducer<String> messageProducer;

    public void scheduleTask(String taskId) {
        log.info("调度任务: {}", taskId);

        // 发送延时消息，10秒后投递
        // RabbitMQ 实现会自动创建:
        // 1. 延时交换机: nebula.delay.exchange.{topic}
        // 2. 延时队列: nebula.delay.queue.{topic}.{delay}ms (设置了 TTL)
        // 3. 死信指向目标 topic
        messageProducer.sendDelayMessage("task.execute", taskId, Duration.ofSeconds(10));
    }
}
```

### 4. 处理消息与重试

消费者使用 `@MessageHandler`。如果抛出异常，框架支持自动重试（可配置次数）。

**`io.nebula.example.rabbitmq.listener.TaskListener`**:

```java
package io.nebula.example.rabbitmq.listener;

import io.nebula.messaging.core.annotation.MessageHandler;
import io.nebula.messaging.core.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskListener {

    /**
     * 监听 task.execute 主题
     * 当延时消息 TTL 到期后，会进入此方法
     */
    @MessageHandler(topic = "task.execute")
    public void executeTask(Message<String> message) {
        String taskId = message.getPayload();
        log.info("开始执行任务: {}, 消息ID: {}", taskId, message.getId());
        
        // 模拟业务处理
        if ("FAIL_TASK".equals(taskId)) {
            throw new RuntimeException("任务执行失败，将触发重试");
        }
        
        log.info("任务完成: {}", taskId);
    }
    
    /**
     * 监听死信队列 (如果配置了 DLQ，且重试耗尽)
     * 默认死信队列名为: nebula.dlx.queue
     */
    @MessageHandler(queue = "nebula.dlx.queue")
    public void handleDeadLetter(Message<Object> message) {
         log.warn("收到死信消息: topic={}, payload={}", message.getTopic(), message.getPayload());
         // 进行人工干预或报警
    }
}
```

## 进阶特性

### 1. 延时队列原理

本模块使用 **TTL (Time-To-Live)** + **DLX (Dead-Letter-Exchange)** 实现延时消息。
- 发送时，消息被路由到一个临时的延时队列，该队列设置了 `x-message-ttl`。
- 队列没有消费者。
- TTL 到期后，RabbitMQ 将消息转发到 `x-dead-letter-exchange`（即目标 topic）。
- 消费者监听目标 topic 对应的队列，从而接收到“延时”后的消息。

### 2. 可靠性保证

- **发送端**: 开启 `publisher-confirms`，确保消息到达 Broker。
- **消费端**: 默认使用手动 ACK (Manual ACK)。框架在方法执行成功后自动 ACK，抛出异常则 NACK 并重试。

## 总结

`nebula-messaging-rabbitmq` 是一个功能完备的生产级 RabbitMQ 客户端封装，特别是在延时消息的处理上提供了非常便捷的抽象，极大地简化了复杂的定时/延时任务开发。

