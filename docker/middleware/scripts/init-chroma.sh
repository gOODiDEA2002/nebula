#!/bin/bash

# Chroma 初始化脚本
# 用于创建 Spring AI 所需的 tenant、database 和 collection

# 配置
CHROMA_HOST=${CHROMA_HOST:-"192.168.111.100"}
CHROMA_PORT=${CHROMA_PORT:-"9002"}
CHROMA_BASE_URL="http://${CHROMA_HOST}:${CHROMA_PORT}"

# Spring AI 默认配置
TENANT_NAME="SpringAiTenant"
DATABASE_NAME="SpringAiDatabase"
COLLECTION_NAME="nebula-collection"

echo "=========================================="
echo "Chroma 初始化脚本"
echo "=========================================="
echo "Chroma 地址: ${CHROMA_BASE_URL}"
echo "Tenant: ${TENANT_NAME}"
echo "Database: ${DATABASE_NAME}"
echo "Collection: ${COLLECTION_NAME}"
echo "=========================================="

# 1. 创建 Tenant
echo ""
echo "1. 创建 Tenant: ${TENANT_NAME}"
RESPONSE=$(curl -s -X POST "${CHROMA_BASE_URL}/api/v2/tenants" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"${TENANT_NAME}\"}" 2>&1)

if echo "$RESPONSE" | grep -q "error"; then
  echo "   ⚠️  Tenant 可能已存在或创建失败: $RESPONSE"
else
  echo "   ✅ Tenant 创建成功"
fi

# 2. 创建 Database
echo ""
echo "2. 创建 Database: ${DATABASE_NAME}"
RESPONSE=$(curl -s -X POST "${CHROMA_BASE_URL}/api/v2/tenants/${TENANT_NAME}/databases" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"${DATABASE_NAME}\"}" 2>&1)

if echo "$RESPONSE" | grep -q "error"; then
  echo "   ⚠️  Database 可能已存在或创建失败: $RESPONSE"
else
  echo "   ✅ Database 创建成功"
fi

# 3. 创建 Collection
echo ""
echo "3. 创建 Collection: ${COLLECTION_NAME}"
RESPONSE=$(curl -s -X POST "${CHROMA_BASE_URL}/api/v2/tenants/${TENANT_NAME}/databases/${DATABASE_NAME}/collections" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"${COLLECTION_NAME}\", \"metadata\": {\"description\": \"Nebula AI collection\"}}" 2>&1)

if echo "$RESPONSE" | grep -q "error"; then
  echo "   ⚠️  Collection 可能已存在或创建失败: $RESPONSE"
else
  echo "   ✅ Collection 创建成功"
fi

# 4. 验证 Collection
echo ""
echo "4. 验证 Collection"
RESPONSE=$(curl -s -X GET "${CHROMA_BASE_URL}/api/v2/tenants/${TENANT_NAME}/databases/${DATABASE_NAME}/collections/${COLLECTION_NAME}" 2>&1)

if echo "$RESPONSE" | grep -q "\"name\":\"${COLLECTION_NAME}\""; then
  echo "   ✅ Collection 验证成功"
  echo ""
  echo "=========================================="
  echo "Chroma 初始化完成！"
  echo "=========================================="
else
  echo "   ❌ Collection 验证失败"
  echo "   响应: $RESPONSE"
  exit 1
fi

