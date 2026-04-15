-- OAuth 客户端示例数据库初始化脚本
-- 使用前请先创建数据库：CREATE DATABASE oauth_client_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE oauth_client_demo;

-- 本地用户表
CREATE TABLE IF NOT EXISTS `local_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `nickname` VARCHAR(100) COMMENT '昵称',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `mobile` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `company_name` VARCHAR(200) COMMENT '企业名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地用户表';

-- 用户第三方绑定表
CREATE TABLE IF NOT EXISTS `user_binding` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '本地用户ID',
    `open_id` VARCHAR(64) NOT NULL COMMENT 'Vocoor 用户 OpenID',
    `source` VARCHAR(32) NOT NULL DEFAULT 'vocoor' COMMENT '来源',
    `nickname` VARCHAR(100) COMMENT '用户昵称（冗余）',
    `avatar` VARCHAR(500) COMMENT '用户头像（冗余）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_open_id` (`open_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户第三方绑定表';

-- 提示信息
SELECT 'OAuth Client 数据库初始化完成！' AS message;


