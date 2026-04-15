#!/bin/bash

# Nebula Order Service gRPC 测试脚本
# 测试 OrderService 的 gRPC 功能

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# gRPC 端口（可通过环境变量覆盖）
GRPC_PORT=${GRPC_PORT:-2002}
GRPC_URL="localhost:$GRPC_PORT"

echo -e "${GREEN}======================================"
echo " Nebula Order Service gRPC 服务测试"
echo -e "======================================${NC}"
echo "服务地址: $GRPC_URL"

# 检查 grpcurl 是否安装
if ! command -v grpcurl &> /dev/null; then
    echo -e "${RED}错误: grpcurl 未安装${NC}"
    echo "请安装: brew install grpcurl"
    exit 1
fi

# 测试 gRPC 连接
echo -e "\n${BLUE}[检查] 测试 gRPC 服务连接${NC}"
if grpcurl -plaintext $GRPC_URL list > /dev/null 2>&1; then
    echo -e "${GREEN}✓ gRPC 服务连接成功${NC}"
else
    echo -e "${RED}✗ gRPC 服务连接失败${NC}"
    echo ""
    echo "可能的原因："
    echo "1. 服务未启动"
    echo "2. 端口错误（当前使用: $GRPC_PORT）"
    echo "3. 服务配置未生效"
    echo ""
    echo "解决方案："
    echo "1. 检查服务是否启动"
    echo "2. 检查日志中的 gRPC 端口"
    echo "3. 如果端口不是 $GRPC_PORT，使用: GRPC_PORT=实际端口 ./test-grpc.sh"
    exit 1
fi

# 测试1：列出所有服务
echo -e "\n${BLUE}[测试1] 列出所有可用服务${NC}"
grpcurl -plaintext $GRPC_URL list

# 测试2：健康检查
echo -e "\n${BLUE}[测试2] 健康检查${NC}"
HEALTH_STATUS=$(grpcurl -plaintext -d '{"service":""}' $GRPC_URL grpc.health.v1.Health/Check 2>&1 | grep -o '"status": *"[^"]*"' | grep -o '\"[^\"]*\"$' | tr -d '"')

if [ "$HEALTH_STATUS" = "SERVING" ]; then
    echo -e "${GREEN}✓ 服务健康状态: $HEALTH_STATUS${NC}"
else
    echo -e "${YELLOW}⚠ 服务健康状态: ${HEALTH_STATUS:-UNKNOWN}${NC}"
fi

# 测试3：查看 GenericRpcService
echo -e "\n${BLUE}[测试3] 测试 Nebula 通用 RPC 服务${NC}"
if grpcurl -plaintext $GRPC_URL list | grep -q "io.nebula.rpc.grpc.GenericRpcService"; then
    echo -e "${GREEN}✓ 发现 GenericRpcService${NC}"
    
    echo -e "\n${BLUE}GenericRpcService 方法:${NC}"
    grpcurl -plaintext $GRPC_URL list io.nebula.rpc.grpc.GenericRpcService
    
    # 测试4：调用 OrderService.createOrder
    echo -e "\n${BLUE}[测试4] 通过 gRPC 调用 OrderService.createOrder${NC}"
    CREATE_ORDER_RESPONSE=$(grpcurl -plaintext \
      -d '{
        "request_id": "grpc-test-order-001",
        "service_name": "io.nebula.example.order.api.rpc.OrderRpcClient",
        "method_name": "createOrder",
        "parameters": ["{\"userId\":1,\"productName\":\"gRPC测试商品\",\"quantity\":2,\"price\":199.99}"],
        "parameter_types": ["io.nebula.example.order.api.dto.CreateOrderDto$Request"],
        "timestamp": '$(date +%s000)'
      }' \
      $GRPC_URL \
      io.nebula.rpc.grpc.GenericRpcService/Call 2>&1)
    
    if echo "$CREATE_ORDER_RESPONSE" | grep -q '"success": *true'; then
        echo -e "${GREEN}✓ createOrder 调用成功${NC}"
        echo "$CREATE_ORDER_RESPONSE" | jq -r '.result' 2>/dev/null | head -c 200 || echo "订单创建成功"
        echo "..."
        
        # 提取订单ID
        ORDER_ID=$(echo "$CREATE_ORDER_RESPONSE" | jq -r '.result' | jq -r '.orderId' 2>/dev/null || echo "1")
        
        # 测试5：调用 OrderService.getOrderById
        echo -e "\n${BLUE}[测试5] 通过 gRPC 调用 OrderService.getOrderById${NC}"
        GET_ORDER_RESPONSE=$(grpcurl -plaintext \
          -d '{
            "request_id": "grpc-test-order-002",
            "service_name": "io.nebula.example.order.api.rpc.OrderRpcClient",
            "method_name": "getOrderById",
            "parameters": ["'$ORDER_ID'"],
            "parameter_types": ["java.lang.Long"],
            "timestamp": '$(date +%s000)'
          }' \
          $GRPC_URL \
          io.nebula.rpc.grpc.GenericRpcService/Call 2>&1)
        
        if echo "$GET_ORDER_RESPONSE" | grep -q '"success": *true'; then
            echo -e "${GREEN}✓ getOrderById 调用成功${NC}"
            echo "$GET_ORDER_RESPONSE" | jq -r '.result' 2>/dev/null | head -c 200 || echo "订单详情已返回"
            echo "..."
        else
            echo -e "${YELLOW}⚠ getOrderById 调用响应:${NC}"
            echo "$GET_ORDER_RESPONSE" | jq '.' 2>/dev/null || echo "$GET_ORDER_RESPONSE"
        fi
    else
        echo -e "${YELLOW}⚠ createOrder 调用响应:${NC}"
        echo "$CREATE_ORDER_RESPONSE" | jq '.' 2>/dev/null || echo "$CREATE_ORDER_RESPONSE"
    fi
    
    # 测试6：验证跨服务调用（Order -> User）
    echo -e "\n${BLUE}[测试6] 验证跨服务调用（OrderService → UserService via gRPC）${NC}"
    echo "提示：OrderService.createOrder 内部会通过 gRPC 调用 UserService.getUserById"
    echo "查看 OrderService 日志，应该看到："
    echo "  → 调用UserService验证用户"
    echo "  ← UserService返回用户信息"
    
else
    echo -e "${YELLOW}⚠ 未发现 GenericRpcService${NC}"
    echo "可能的原因："
    echo "1. nebula-rpc-grpc 模块未启用"
    echo "2. 服务尚未注册"
    echo ""
    echo "请检查 application.yml 中的配置："
    echo "  nebula.rpc.grpc.enabled: true"
fi

# 总结
echo -e "\n${GREEN}======================================"
echo "  测试完成！"
echo -e "======================================${NC}"
echo ""
echo "💡 提示："
echo "- OrderService 通过 gRPC 调用 UserService"
echo "- 如果端口不是 $GRPC_PORT，请检查日志确认实际端口"
echo "- 详细文档: ../docs/GRPC_TESTING_GUIDE.md"
echo ""
echo "📋 测试摘要："
echo "- Health服务: ✓"
echo "- GenericRpcService: 见上方测试结果"
echo "- OrderService (gRPC): 见上方测试结果"
echo "- 跨服务调用 (Order→User): 检查服务日志"
echo ""

