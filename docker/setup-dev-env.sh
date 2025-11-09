#!/bin/bash
# 创建所有必要的目录
ROOT_DIR=/Users/andy/DevOps/SourceCode/nebula-projects/nebula-data
mkdir -p $ROOT_DIR/{redis,rabbitmq,minio,elasticsearch,mysql,xxl-job,nacos,mongodb,chroma}
mkdir -p $ROOT_DIR/mysql/data
mkdir -p $ROOT_DIR/mysql/conf
# mkdir -p $ROOT_DIR/mongodb/backup
mkdir -p $ROOT_DIR/nacos/logs
mkdir -p $ROOT_DIR/mysql/init 
# 拷贝数据库初始化脚本
cp sql/xxl-job.sql $ROOT_DIR/mysql/init/xxl-job.sql
cp sql/nacos.sql $ROOT_DIR/mysql/init/nacos.sql 

# 设置目录权限
sudo chmod -R 755 $ROOT_DIR/

# 设置各个服务的版本号
REDIS_VERSION=8.2.1
RABBITMQ_VERSION=3.12-management
MINIO_VERSION=RELEASE.2025-04-22T22-12-26Z
ELASTICSEARCH_VERSION=7.17.19
MYSQL_VERSION=8.3.0 
XXL_JOB_VERSION=2.4.1
MONGO_VERSION=8.0
NACOS_VERSION=v2.5.1
CHROMA_VERSION=latest

# 设置各个服务的端口号
REDIS_PORT=6379
RABBITMQ_PORT=5672
RABBITMQ_PORT_MANAGEMENT=15672
MINIO_PORT=9000
MINIO_PORT_MANAGEMENT=9090
ELASTICSEARCH_PORT=9200
MYSQL_PORT=3306
XXL_JOB_PORT=9001
MONGO_PORT=27017
NACOS_PORT=8848
NACOS_PORT_JMX=9848
CHROMA_PORT=9002

# 设置各个服务的账户名和密码
REDIS_USERNAME=redis
REDIS_PASSWORD=redis123
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
MINIO_USERNAME=minioadmin
MINIO_PASSWORD=minioadmin
MYSQL_ROOT_USERNAME=root
MYSQL_ROOT_PASSWORD=root
MYSQL_INIT_DATABASE=nebula
MYSQL_INIT_USERNAME=nebula
MYSQL_INIT_PASSWORD=nebula123
XXL_JOB_DATABASE_USERNAME=root
XXL_JOB_DATABASE_PASSWORD=root
MONGO_USERNAME=admin
MONGO_PASSWORD=admin123
NACOS_USERNAME=root
NACOS_PASSWORD=root

# 创建 MySQL 初始化脚本（为 XXL-Job 和 Nacos 创建数据库）
sudo tee $ROOT_DIR/mysql/init/init-databases.sql > /dev/null <<'EOF'
-- 创建 XXL-Job 数据库
CREATE DATABASE IF NOT EXISTS `xxl_job` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建 Nacos 数据库
CREATE DATABASE IF NOT EXISTS `nacos` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 显示创建的数据库
SHOW DATABASES;
EOF

# 拷贝scripts目录
cp -r scripts $ROOT_DIR/scripts

# 创建 docker-compose.yml 文件
cat <<EOF > $ROOT_DIR/docker-compose.yml
services:
  # Redis缓存
  redis:
    image: redis:$REDIS_VERSION
    container_name: nebula-redis
    ports:
      - "$REDIS_PORT:6379"
    volumes:
      - $ROOT_DIR/redis:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 5
    environment:
      - REDIS_PASSWORD=$REDIS_PASSWORD

  # RabbitMQ消息队列
  rabbitmq:
    image: rabbitmq:$RABBITMQ_VERSION
    container_name: nebula-rabbitmq
    ports:
      - "$RABBITMQ_PORT:5672"
      - "$RABBITMQ_PORT_MANAGEMENT:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=$RABBITMQ_USERNAME
      - RABBITMQ_DEFAULT_PASS=$RABBITMQ_PASSWORD
    volumes:
      - $ROOT_DIR/rabbitmq:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
      interval: 30s
      timeout: 10s
      retries: 5

  # MinIO对象存储
  minio:
    image: minio/minio:$MINIO_VERSION
    container_name: nebula-minio
    ports:
      - "$MINIO_PORT:9000"
      - "$MINIO_PORT_MANAGEMENT:9001"
    environment:
      - MINIO_ROOT_USER=$MINIO_USERNAME
      - MINIO_ROOT_PASSWORD=$MINIO_PASSWORD
    command: server /data --console-address ":9001"  # 修正：使用容器内路径
    volumes:
      - $ROOT_DIR/minio:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

  # Elasticsearch搜索引擎
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:$ELASTICSEARCH_VERSION
    container_name: nebula-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
      - bootstrap.memory_lock=true
    ulimits:
      memlock:
        soft: -1
        hard: -1
    ports:
      - "$ELASTICSEARCH_PORT:9200"
      - "$ELASTICSEARCH_PORT_TRANSPORT:9300"
    volumes:
      - $ROOT_DIR/elasticsearch:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:$ELASTICSEARCH_PORT"]
      interval: 30s
      timeout: 10s
      retries: 5

  # MySQL数据库
  mysql:
    image: mysql:$MYSQL_VERSION
    container_name: nebula-mysql
    environment:
      - MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD
      - MYSQL_DATABASE=$MYSQL_INIT_DATABASE
      - MYSQL_USER=$MYSQL_INIT_USERNAME
      - MYSQL_PASSWORD=$MYSQL_INIT_PASSWORD
    ports:
      - "$MYSQL_PORT:3306"
    volumes:
      - $ROOT_DIR/mysql/data:/var/lib/mysql
      - $ROOT_DIR/mysql/conf:/etc/mysql/conf.d
      - $ROOT_DIR/mysql/init:/docker-entrypoint-initdb.d
    command: 
      - --default-authentication-plugin=mysql_native_password
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot"]
      interval: 30s
      timeout: 10s
      retries: 5

  # XXL-Job任务调度
  xxl-job:
    # image: xuxueli/xxl-job-admin:$XXL_JOB_VERSION
    # image: wangpenghua/xxl-job-admin:2.4.1
    container_name: nebula-xxl-job
    ports:
      - "$XXL_JOB_PORT:8080"
    volumes:
      - $ROOT_DIR/xxl-job:/data/applogs
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
      - SPRING_DATASOURCE_USERNAME=$XXL_JOB_DATABASE_USERNAME
      - SPRING_DATASOURCE_PASSWORD=$XXL_JOB_DATABASE_PASSWORD
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:$XXL_JOB_PORT/xxl-job-admin/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  # # MongoDB数据库
  # mongodb:
  #   image: mongo:$MONGO_VERSION
  #   container_name: nebula-mongodb
  #   ports:
  #     - "$MONGO_PORT:27017"
  #   environment:
  #     - MONGO_INITDB_ROOT_USERNAME=$MONGO_USERNAME
  #     - MONGO_INITDB_ROOT_PASSWORD=$MONGO_PASSWORD
  #   volumes:
  #     - $ROOT_DIR/mongodb:/data/db
  #     - $ROOT_DIR/mongodb/backup:/data/backup
  #   command: 
  #     - --auth
  #     - --bind_ip_all
  #   healthcheck:
  #     test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
  #     interval: 30s
  #     timeout: 10s
  #     retries: 5

  # Nacos配置中心
  nacos:
    image: nacos/nacos-server:$NACOS_VERSION
    container_name: nebula-nacos
    ports:
      - "$NACOS_PORT:8848"
      - "$NACOS_PORT_JMX:9848"
    environment:
      - MODE=standalone
      - SPRING_DATASOURCE_PLATFORM=mysql
      - MYSQL_SERVICE_HOST=mysql
      - MYSQL_SERVICE_DB_NAME=nacos
      - MYSQL_SERVICE_PORT=$MYSQL_PORT
      - MYSQL_SERVICE_USER=$MYSQL_ROOT_USERNAME
      - MYSQL_SERVICE_PASSWORD=$MYSQL_ROOT_PASSWORD
      - NACOS_AUTH_ENABLE=true
      - NACOS_AUTH_IDENTITY_KEY=secretKey
      - NACOS_AUTH_IDENTITY_VALUE=secretKey
      - NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789
    volumes:
      - $ROOT_DIR/nacos:/home/nacos/data
      - $ROOT_DIR/nacos/logs:/home/nacos/logs
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:$NACOS_PORT/nacos/"]
      interval: 30s
      timeout: 10s
      retries: 5
  # Chrome
  chroma:
    image: chromadb/chroma:$CHROMA_VERSION
    container_name: nebula-chroma
    volumes:
      - $ROOT_DIR/chroma:/data
    environment:
      - CHROMA_SERVER_HOST=0.0.0.0
      - CHROMA_SERVER_PORT=8000
      - CHROMA_PERSIST_DIRECTORY=/data
    ports:
      - "$CHROMA_PORT:8000"
    restart: unless-stopped
EOF

echo "切换到目录 $ROOT_DIR 并运行："
echo "cd $ROOT_DIR && docker-compose up -d"