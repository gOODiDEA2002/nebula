#!/bin/bash

# RPC 端点测试脚本
# 演示如何通过统一的 /rpc 端点调用服务

RPC_URL="http://localhost:1001/rpc"
SERVICE_NAME="io.nebula.example.api.rpc.UserRpcService"

echo "======================================"
echo " Nebula RPC 端点测试"
echo "======================================"

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试1: 获取用户详情
echo -e "\n${BLUE}[测试1] 获取用户详情${NC}"
curl -s -X POST $RPC_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"test-$(date +%s)-1\",
    \"serviceName\": \"$SERVICE_NAME\",
    \"methodName\": \"getUserById\",
    \"parameters\": [1],
    \"parameterTypes\": [\"java.lang.Long\"]
  }" | jq

# 测试2: 创建用户
echo -e "\n${BLUE}[测试2] 创建用户${NC}"
curl -s -X POST $RPC_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"test-$(date +%s)-2\",
    \"serviceName\": \"$SERVICE_NAME\",
    \"methodName\": \"createUser\",
    \"parameters\": [{
      \"username\": \"rpctest\",
      \"name\": \"RPC测试用户\",
      \"email\": \"rpctest@example.com\",
      \"phone\": \"13900000000\",
      \"status\": \"ACTIVE\"
    }],
    \"parameterTypes\": [\"io.nebula.example.api.dto.CreateUserDto\$Request\"]
  }" | jq

# 测试3: 获取用户列表
echo -e "\n${BLUE}[测试3] 获取用户列表（状态=ACTIVE，前5条）${NC}"
curl -s -X POST $RPC_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"test-$(date +%s)-3\",
    \"serviceName\": \"$SERVICE_NAME\",
    \"methodName\": \"getUsers\",
    \"parameters\": [null, null, \"ACTIVE\", 1, 5],
    \"parameterTypes\": [
      \"java.lang.String\",
      \"java.lang.String\",
      \"java.lang.String\",
      \"java.lang.Integer\",
      \"java.lang.Integer\"
    ]
  }" | jq '.result.users | length' | xargs -I {} echo "返回用户数: {}"

# 测试4: 更新用户
echo -e "\n${BLUE}[测试4] 更新用户${NC}"
curl -s -X POST $RPC_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"test-$(date +%s)-4\",
    \"serviceName\": \"$SERVICE_NAME\",
    \"methodName\": \"updateUser\",
    \"parameters\": [
      1,
      {
        \"name\": \"RPC更新的用户\",
        \"email\": \"updated-rpc@example.com\"
      }
    ],
    \"parameterTypes\": [
      \"java.lang.Long\",
      \"io.nebula.example.api.dto.UpdateUserDto\$Request\"
    ]
  }" | jq

# 测试5: 删除用户
echo -e "\n${BLUE}[测试5] 删除用户${NC}"
curl -s -X POST $RPC_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"test-$(date +%s)-5\",
    \"serviceName\": \"$SERVICE_NAME\",
    \"methodName\": \"deleteUser\",
    \"parameters\": [100],
    \"parameterTypes\": [\"java.lang.Long\"]
  }" | jq

echo -e "\n${GREEN}======================================"
echo "  测试完成！"
echo "======================================${NC}"

