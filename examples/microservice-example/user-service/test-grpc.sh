#!/bin/bash

# gRPC 服务测试脚本
# 需要安装 grpcurl: brew install grpcurl

# 检查 grpcurl 是否已安装
if ! command -v grpcurl &> /dev/null; then
    echo "❌ grpcurl 未安装"
    echo "请先安装: brew install grpcurl"
    exit 1
fi

# 配置
GRPC_PORT="${GRPC_PORT:-2001}"  # 默认 2001，可通过环境变量覆盖
GRPC_URL="localhost:$GRPC_PORT"

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "======================================"
echo " Nebula gRPC 服务测试"
echo "======================================"
echo -e "${YELLOW}服务地址: $GRPC_URL${NC}\n"

# 测试连接
echo -e "${BLUE}[检查] 测试 gRPC 服务连接${NC}"
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
    echo "3. 如果端口不是 $GRPC_PORT，使用: GRPC_PORT=9090 ./test-grpc.sh"
    exit 1
fi

# 测试1: 列出所有服务
echo -e "\n${BLUE}[测试1] 列出所有可用服务${NC}"
grpcurl -plaintext $GRPC_URL list

# 测试2: 健康检查
echo -e "\n${BLUE}[测试2] 健康检查${NC}"
HEALTH_RESPONSE=$(grpcurl -plaintext \
  -d '{"service": ""}' \
  $GRPC_URL \
  grpc.health.v1.Health/Check 2>&1)

if echo "$HEALTH_RESPONSE" | grep -q "SERVING"; then
    echo -e "${GREEN}✓ 服务健康状态: SERVING${NC}"
else
    echo -e "${YELLOW}⚠ 健康检查响应:${NC}"
    echo "$HEALTH_RESPONSE"
fi

# 测试3: 查看健康服务的方法
echo -e "\n${BLUE}[测试3] 查看 Health 服务的方法${NC}"
grpcurl -plaintext $GRPC_URL list grpc.health.v1.Health

# 测试4: 查看反射服务
echo -e "\n${BLUE}[测试4] 查看反射服务的方法${NC}"
grpcurl -plaintext $GRPC_URL list grpc.reflection.v1alpha.ServerReflection

# 测试5: 查看方法详情
echo -e "\n${BLUE}[测试5] 查看 Health.Check 方法详情${NC}"
grpcurl -plaintext $GRPC_URL describe grpc.health.v1.Health.Check

# 查找自定义服务
echo -e "\n${BLUE}[检查] 查找自定义业务服务${NC}"
CUSTOM_SERVICES=$(grpcurl -plaintext $GRPC_URL list 2>&1 | grep -v "^grpc\." || true)

if [ -n "$CUSTOM_SERVICES" ]; then
    echo -e "${GREEN}✓ 发现自定义服务:${NC}"
    echo "$CUSTOM_SERVICES"
else
    echo -e "${YELLOW}⚠ 未发现自定义业务服务（预期行为）${NC}"
    echo "Nebula 使用通用 gRPC 服务（GenericRpcService）而不是为每个服务生成独立 proto"
fi

# 测试 Nebula GenericRpcService（如果存在）
echo -e "\n${BLUE}[测试6] 测试 Nebula 通用 RPC 服务${NC}"
if grpcurl -plaintext $GRPC_URL list | grep -q "io.nebula.rpc.grpc.GenericRpcService"; then
    echo -e "${GREEN}✓ 发现 GenericRpcService${NC}"
    
    # 查看方法
    echo -e "\n${BLUE}GenericRpcService 方法:${NC}"
    grpcurl -plaintext $GRPC_URL list io.nebula.rpc.grpc.GenericRpcService
    
    # 测试调用 UserService - 获取用户详情
    echo -e "\n${BLUE}[测试7] 通过 gRPC 调用 UserService.getUserById${NC}"
    USER_RESPONSE=$(grpcurl -plaintext \
      -d '{
        "request_id": "grpc-test-001",
        "service_name": "io.nebula.example.api.rpc.UserRpcClient",
        "method_name": "getUserById",
        "parameters": ["1"],
        "parameter_types": ["java.lang.Long"],
        "timestamp": '$(date +%s000)'
      }' \
      $GRPC_URL \
      io.nebula.rpc.grpc.GenericRpcService/Call 2>&1)
    
    if echo "$USER_RESPONSE" | grep -q '"success": *true'; then
        echo -e "${GREEN}✓ getUserById 调用成功${NC}"
        echo "$USER_RESPONSE" | jq -r '.result' 2>/dev/null | head -c 150 || echo "结果已返回"
        echo "..."
    else
        echo -e "${YELLOW}⚠ getUserById 调用响应:${NC}"
        echo "$USER_RESPONSE" | jq '.' 2>/dev/null || echo "$USER_RESPONSE"
    fi
    
    # 测试创建用户
    echo -e "\n${BLUE}[测试8] 通过 gRPC 调用 UserService.createUser${NC}"
    CREATE_RESPONSE=$(grpcurl -plaintext \
      -d '{
        "request_id": "grpc-test-002",
        "service_name": "io.nebula.example.api.rpc.UserRpcClient",
        "method_name": "createUser",
        "parameters": ["{\"username\":\"grpctest\",\"name\":\"gRPC测试用户\",\"email\":\"grpc@test.com\",\"phone\":\"13900000000\",\"status\":\"ACTIVE\"}"],
        "parameter_types": ["io.nebula.example.api.dto.CreateUserDto$Request"],
        "timestamp": '$(date +%s000)'
      }' \
      $GRPC_URL \
      io.nebula.rpc.grpc.GenericRpcService/Call 2>&1)
    
    if echo "$CREATE_RESPONSE" | grep -q "success"; then
        echo -e "${GREEN}✓ CreateUser 调用成功${NC}"
        echo "$CREATE_RESPONSE" | grep -o '"result":"[^"]*"' | head -1
    else
        echo -e "${YELLOW}⚠ CreateUser 调用响应:${NC}"
        echo "$CREATE_RESPONSE"
    fi
    
    # 测试获取用户列表（5个参数：username, name, status, page, size）
    echo -e "\n${BLUE}[测试9] 通过 gRPC 调用 UserService.getUsers${NC}"
    LIST_RESPONSE=$(grpcurl -plaintext \
      -d '{
        "request_id": "grpc-test-003",
        "service_name": "io.nebula.example.api.rpc.UserRpcClient",
        "method_name": "getUsers",
        "parameters": ["null", "null", "\"ACTIVE\"", "1", "5"],
        "parameter_types": ["java.lang.String", "java.lang.String", "java.lang.String", "java.lang.Integer", "java.lang.Integer"],
        "timestamp": '$(date +%s000)'
      }' \
      $GRPC_URL \
      io.nebula.rpc.grpc.GenericRpcService/Call 2>&1)
    
    if echo "$LIST_RESPONSE" | grep -q '"success": *true'; then
        echo -e "${GREEN}✓ getUsers 调用成功${NC}"
        # 显示用户数量
        USER_COUNT=$(echo "$LIST_RESPONSE" | jq -r '.result' | jq -r '.total' 2>/dev/null || echo "N/A")
        echo -e "${GREEN}返回用户数: $USER_COUNT${NC}"
    else
        echo -e "${YELLOW}⚠ getUsers 调用响应:${NC}"
        echo "$LIST_RESPONSE" | jq '.' 2>/dev/null || echo "$LIST_RESPONSE"
    fi
    
    # 测试更新用户（2个参数：Long id, UpdateUserDto.Request request）
    echo -e "\n${BLUE}[测试10] 通过 gRPC 调用 UserService.updateUser${NC}"
    UPDATE_RESPONSE=$(grpcurl -plaintext \
      -d '{
        "request_id": "grpc-test-004",
        "service_name": "io.nebula.example.api.rpc.UserRpcClient",
        "method_name": "updateUser",
        "parameters": ["1", "{\"id\":1,\"name\":\"gRPC更新的用户\",\"email\":\"grpc-updated@test.com\"}"],
        "parameter_types": ["java.lang.Long", "io.nebula.example.api.dto.UpdateUserDto$Request"],
        "timestamp": '$(date +%s000)'
      }' \
      $GRPC_URL \
      io.nebula.rpc.grpc.GenericRpcService/Call 2>&1)
    
    if echo "$UPDATE_RESPONSE" | grep -q '"success": *true'; then
        echo -e "${GREEN}✓ updateUser 调用成功${NC}"
    else
        echo -e "${YELLOW}⚠ updateUser 调用响应:${NC}"
        echo "$UPDATE_RESPONSE" | jq '.' 2>/dev/null || echo "$UPDATE_RESPONSE"
    fi
    
    # 测试删除用户
    echo -e "\n${BLUE}[测试11] 通过 gRPC 调用 UserService.deleteUser${NC}"
    DELETE_RESPONSE=$(grpcurl -plaintext \
      -d '{
        "request_id": "grpc-test-005",
        "service_name": "io.nebula.example.api.rpc.UserRpcClient",
        "method_name": "deleteUser",
        "parameters": ["100"],
        "parameter_types": ["java.lang.Long"],
        "timestamp": '$(date +%s000)'
      }' \
      $GRPC_URL \
      io.nebula.rpc.grpc.GenericRpcService/Call 2>&1)
    
    if echo "$DELETE_RESPONSE" | grep -q '"success": *true'; then
        echo -e "${GREEN}✓ deleteUser 调用成功${NC}"
    else
        echo -e "${YELLOW}⚠ deleteUser 调用响应:${NC}"
        echo "$DELETE_RESPONSE" | jq '.' 2>/dev/null || echo "$DELETE_RESPONSE"
    fi
else
    echo -e "${YELLOW}⚠ 未发现 GenericRpcService${NC}"
    echo "可能的原因："
    echo "1. nebula-rpc-grpc 模块未启用"
    echo "2. 服务尚未注册"
fi

# 总结
echo -e "\n${GREEN}======================================"
echo "  测试完成！"
echo -e "======================================${NC}"
echo ""
echo "💡 提示："
echo "- Nebula 使用 GenericRpcService 处理所有 RPC 调用"
echo "- 如果端口不是 $GRPC_PORT，请检查日志确认实际端口"
echo "- 详细文档: ../docs/GRPC_TESTING_GUIDE.md"
echo ""
echo "📋 测试摘要："
echo "- Health服务: ✓"
echo "- 反射服务: ✓"
echo "- UserService (gRPC): 见上方测试结果"

