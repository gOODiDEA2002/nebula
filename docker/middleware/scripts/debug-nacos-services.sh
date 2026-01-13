#!/bin/bash

echo "=========================================="
echo " Nacos 服务注册调试工具"
echo "=========================================="
echo ""

# Nacos 配置
NACOS_SERVER="http://localhost:8848"
NAMESPACE_ID="83f84105-ad41-400e-8666-ef46aceb9e14"
NAMESPACE_NAME="nebula-dev"
USERNAME="nacos"
PASSWORD="nacos"

echo "[1] 查询所有命名空间"
curl -s "${NACOS_SERVER}/nacos/v1/console/namespaces?username=${USERNAME}&password=${PASSWORD}" | jq '.data[] | {namespace, namespaceShowName}'
echo ""

echo "[2] 查询 ${NAMESPACE_NAME} 命名空间的服务列表"
SERVICES=$(curl -s "${NACOS_SERVER}/nacos/v1/ns/catalog/services?namespaceId=${NAMESPACE_ID}&username=${USERNAME}&password=${PASSWORD}" | jq -r '.serviceList[]?.name')

if [ -z "$SERVICES" ]; then
    echo "⚠️  未找到任何服务"
else
    echo "✓ 找到以下服务："
    echo "$SERVICES"
fi
echo ""

echo "[3] 查询 public 命名空间的服务列表（排查是否误注册到 public）"
PUBLIC_SERVICES=$(curl -s "${NACOS_SERVER}/nacos/v1/ns/catalog/services?namespaceId=&username=${USERNAME}&password=${PASSWORD}" | jq -r '.serviceList[]?.name')

if [ -z "$PUBLIC_SERVICES" ]; then
    echo "✓ public 命名空间没有服务"
else
    echo "⚠️  public 命名空间的服务："
    echo "$PUBLIC_SERVICES"
fi
echo ""

echo "[4] 检查本地服务进程"
echo "HTTP 端口："
lsof -ti:8081,8082 2>/dev/null && echo "✓ 服务运行中" || echo "⚠️  服务未运行"

echo "gRPC 端口："
lsof -ti:9081,9082 2>/dev/null && echo "✓ 服务运行中" || echo "⚠️  服务未运行"
echo ""

echo "[5] 检查服务实例详情（nebula-dev 命名空间）"
echo "nebula-example-service:"
curl -s "${NACOS_SERVER}/nacos/v1/ns/instance/list?serviceName=nebula-example-service&namespaceId=${NAMESPACE_ID}&username=${USERNAME}&password=${PASSWORD}" | jq '.hosts[]? | {ip, port, healthy}'

echo ""
echo "nebula-example-order-service:"
curl -s "${NACOS_SERVER}/nacos/v1/ns/instance/list?serviceName=nebula-example-order-service&namespaceId=${NAMESPACE_ID}&username=${USERNAME}&password=${PASSWORD}" | jq '.hosts[]? | {ip, port, healthy}'

echo ""
echo "[6] 检查服务实例详情（public 命名空间）"
echo "nebula-example-service:"
curl -s "${NACOS_SERVER}/nacos/v1/ns/instance/list?serviceName=nebula-example-service&username=${USERNAME}&password=${PASSWORD}" | jq '.hosts[]? | {ip, port, healthy}'

echo ""
echo "nebula-example-order-service:"
curl -s "${NACOS_SERVER}/nacos/v1/ns/instance/list?serviceName=nebula-example-order-service&username=${USERNAME}&password=${PASSWORD}" | jq '.hosts[]? | {ip, port, healthy}'

echo ""
echo "=========================================="
echo " 调试完成"
echo "=========================================="

