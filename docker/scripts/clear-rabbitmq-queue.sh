#!/bin/bash

# 清空 RabbitMQ 队列脚本
# 用于清理因错误而堆积的消息

RABBITMQ_HOST="localhost"
RABBITMQ_PORT="15672"
RABBITMQ_USER="guest"
RABBITMQ_PASSWORD="guest"
QUEUE_NAME="order-notification-queue"

echo "正在清空 RabbitMQ 队列: $QUEUE_NAME"

# 使用 RabbitMQ Management API 清空队列
curl -i -u ${RABBITMQ_USER}:${RABBITMQ_PASSWORD} \
  -X DELETE \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/%2F/${QUEUE_NAME}/contents

echo ""
echo "队列清空完成！"
echo ""
echo "如果需要清空其他队列，请修改 QUEUE_NAME 变量"
echo "例如: order-batch-queue, order-status-update-queue"
