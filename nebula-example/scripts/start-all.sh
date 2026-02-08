#!/bin/bash
# ==================================================
# Nebula 示例项目启动脚本
# 一键启动所有示例服务
# ==================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLE_DIR="$(dirname "$SCRIPT_DIR")"
LOG_DIR="/tmp/nebula-example"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 创建日志目录
mkdir -p "$LOG_DIR"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Nebula 示例项目启动脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查 Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: 未找到 Java，请先安装 JDK 21+${NC}"
    exit 1
fi

# 检查 Nacos
echo -e "${YELLOW}检查 Nacos 连接...${NC}"
if curl -s http://localhost:8848/nacos/v1/console/health/liveness > /dev/null 2>&1; then
    echo -e "${GREEN}  Nacos 可用${NC}"
else
    echo -e "${RED}  警告: Nacos 不可用 (localhost:8848)${NC}"
    echo -e "${RED}  某些服务可能无法正常工作${NC}"
fi

# 编译所有项目
echo ""
echo -e "${YELLOW}编译所有示例项目...${NC}"
cd "$EXAMPLE_DIR"
mvn clean package -DskipTests -q
echo -e "${GREEN}  编译完成${NC}"

# 启动函数
start_service() {
    local name=$1
    local jar_path=$2
    local port=$3
    
    echo ""
    echo -e "${YELLOW}启动 $name (端口: $port)...${NC}"
    
    if [ -f "$jar_path" ]; then
        java -jar "$jar_path" --enable-preview > "$LOG_DIR/$name.log" 2>&1 &
        echo "$!" > "$LOG_DIR/$name.pid"
        sleep 5
        
        # 检查是否启动成功
        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}  $name 启动成功 (PID: $(cat "$LOG_DIR/$name.pid"))${NC}"
        else
            echo -e "${YELLOW}  $name 正在启动中...${NC}"
        fi
    else
        echo -e "${RED}  错误: 未找到 $jar_path${NC}"
    fi
}

# 启动 Web 示例
start_service "nebula-example-web" \
    "$EXAMPLE_DIR/nebula-example-web/target/nebula-example-web-1.0.0-SNAPSHOT.jar" \
    8080

# 启动微服务示例 - User Service
start_service "user-service" \
    "$EXAMPLE_DIR/nebula-example-microservice/user-service/target/user-service-1.0.0-SNAPSHOT.jar" \
    8001

# 启动微服务示例 - Order Service
start_service "order-service" \
    "$EXAMPLE_DIR/nebula-example-microservice/order-service/target/order-service-1.0.0-SNAPSHOT.jar" \
    8002

# 启动网关示例
start_service "nebula-example-gateway" \
    "$EXAMPLE_DIR/nebula-example-gateway/target/nebula-example-gateway-1.0.0-SNAPSHOT.jar" \
    8000

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  所有服务启动完成${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "服务列表:"
echo "  - Web 应用:    http://localhost:8080"
echo "  - User 服务:   http://localhost:8001"
echo "  - Order 服务:  http://localhost:8002"
echo "  - API 网关:    http://localhost:8000"
echo ""
echo "日志目录: $LOG_DIR"
echo "停止服务: $SCRIPT_DIR/stop-all.sh"
