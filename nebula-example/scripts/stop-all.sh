#!/bin/bash
# ==================================================
# Nebula 示例项目停止脚本
# 一键停止所有示例服务
# ==================================================

LOG_DIR="/tmp/nebula-example"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Nebula 示例项目停止脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 停止函数
stop_service() {
    local name=$1
    local pid_file="$LOG_DIR/$name.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo -e "${YELLOW}停止 $name (PID: $pid)...${NC}"
            kill "$pid" 2>/dev/null
            sleep 2
            
            # 如果还在运行，强制停止
            if ps -p "$pid" > /dev/null 2>&1; then
                kill -9 "$pid" 2>/dev/null
            fi
            
            echo -e "${GREEN}  $name 已停止${NC}"
        else
            echo -e "${YELLOW}  $name 未运行${NC}"
        fi
        rm -f "$pid_file"
    else
        echo -e "${YELLOW}  $name PID 文件不存在${NC}"
    fi
}

# 停止所有服务
stop_service "nebula-example-gateway"
stop_service "order-service"
stop_service "user-service"
stop_service "nebula-example-web"

# 清理可能残留的进程
echo ""
echo -e "${YELLOW}清理残留进程...${NC}"
pkill -f "nebula-example-web" 2>/dev/null || true
pkill -f "user-service" 2>/dev/null || true
pkill -f "order-service" 2>/dev/null || true
pkill -f "nebula-example-gateway" 2>/dev/null || true

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  所有服务已停止${NC}"
echo -e "${GREEN}========================================${NC}"
