-- Nebula 分片演示表结构
-- 创建订单表用于演示分库分表功能

-- ====================================
-- 创建分片数据库 nebula_sharding_0
-- ====================================
CREATE DATABASE IF NOT EXISTS `nebula_sharding_0`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `nebula_sharding_0`;

-- 删除已存在的表
DROP TABLE IF EXISTS `t_order_0`;
DROP TABLE IF EXISTS `t_order_1`;

-- 创建订单表 t_order_0 (分表0)
CREATE TABLE `t_order_0` (
    `id` BIGINT NOT NULL COMMENT '订单ID（主键，雪花算法生成）',
    `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（分库键）',
    `product_id` BIGINT DEFAULT NULL COMMENT '产品ID',
    `product_name` VARCHAR(255) NOT NULL COMMENT '产品名称',
    `quantity` INT DEFAULT 1 COMMENT '购买数量',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额（分片演示字段）',
    `total_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '总金额',
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING, PAID, SHIPPED, COMPLETED, CANCELLED',
    `payment_method` VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    `shipping_address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='订单表 - 分片0 (ds0.t_order_0)';

-- 创建订单表 t_order_1 (分表1)
CREATE TABLE `t_order_1` (
    `id` BIGINT NOT NULL COMMENT '订单ID（主键，雪花算法生成）',
    `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（分库键）',
    `product_id` BIGINT DEFAULT NULL COMMENT '产品ID',
    `product_name` VARCHAR(255) NOT NULL COMMENT '产品名称',
    `quantity` INT DEFAULT 1 COMMENT '购买数量',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额（分片演示字段）',
    `total_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '总金额',
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING, PAID, SHIPPED, COMPLETED, CANCELLED',
    `payment_method` VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    `shipping_address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='订单表 - 分片1 (ds0.t_order_1)';

-- 插入示例数据到 nebula_sharding_0 的表中
-- 根据分片规则: user_id % 2 = 0 的数据会路由到 ds0 (nebula_sharding_0)
-- 根据分表规则: id % 2 决定路由到 t_order_0 或 t_order_1

-- 插入到 t_order_0 (user_id 偶数, id 偶数)
INSERT INTO `t_order_0` (`id`, `order_no`, `user_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `amount`, `total_amount`, `status`) VALUES
(2000000000000000000, 'ORDER_000001', 100, 1, 'iPhone 15 Pro', 1, 8999.00, 8999.00, 8999.00, 'PAID'),
(2000000000000000002, 'ORDER_000002', 102, 2, 'MacBook Pro', 1, 15999.00, 15999.00, 15999.00, 'PENDING'),
(2000000000000000004, 'ORDER_000003', 104, 3, 'iPad Air', 2, 4999.00, 9998.00, 9998.00, 'SHIPPED');

-- 插入到 t_order_1 (user_id 偶数, id 奇数)  
INSERT INTO `t_order_1` (`id`, `order_no`, `user_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `amount`, `total_amount`, `status`) VALUES
(2000000000000000001, 'ORDER_000004', 100, 4, 'Apple Watch', 1, 2999.00, 2999.00, 2999.00, 'COMPLETED'),
(2000000000000000003, 'ORDER_000005', 102, 5, 'AirPods Pro', 1, 1999.00, 1999.00, 1999.00, 'CANCELLED'),
(2000000000000000005, 'ORDER_000006', 104, 6, 'Magic Keyboard', 1, 1299.00, 1299.00, 1299.00, 'PAID');

-- ====================================
-- 创建分片数据库 nebula_sharding_1
-- ====================================
CREATE DATABASE IF NOT EXISTS `nebula_sharding_1`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `nebula_sharding_1`;

-- 删除已存在的表
DROP TABLE IF EXISTS `t_order_0`;
DROP TABLE IF EXISTS `t_order_1`;

-- 创建订单表 t_order_0 (分表0)
CREATE TABLE `t_order_0` (
    `id` BIGINT NOT NULL COMMENT '订单ID（主键，雪花算法生成）',
    `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（分库键）',
    `product_id` BIGINT DEFAULT NULL COMMENT '产品ID',
    `product_name` VARCHAR(255) NOT NULL COMMENT '产品名称',
    `quantity` INT DEFAULT 1 COMMENT '购买数量',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额（分片演示字段）',
    `total_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '总金额',
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING, PAID, SHIPPED, COMPLETED, CANCELLED',
    `payment_method` VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    `shipping_address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='订单表 - 分片0 (ds1.t_order_0)';

-- 创建订单表 t_order_1 (分表1)
CREATE TABLE `t_order_1` (
    `id` BIGINT NOT NULL COMMENT '订单ID（主键，雪花算法生成）',
    `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（分库键）',
    `product_id` BIGINT DEFAULT NULL COMMENT '产品ID',
    `product_name` VARCHAR(255) NOT NULL COMMENT '产品名称',
    `quantity` INT DEFAULT 1 COMMENT '购买数量',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额（分片演示字段）',
    `total_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '总金额',
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING, PAID, SHIPPED, COMPLETED, CANCELLED',
    `payment_method` VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    `shipping_address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='订单表 - 分片1 (ds1.t_order_1)';

-- 插入示例数据到 nebula_sharding_1 的表中
-- 根据分片规则: user_id % 2 = 1 的数据会路由到 ds1 (nebula_sharding_1)
-- 根据分表规则: id % 2 决定路由到 t_order_0 或 t_order_1

-- 插入到 t_order_0 (user_id 奇数, id 偶数)
INSERT INTO `t_order_0` (`id`, `order_no`, `user_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `amount`, `total_amount`, `status`) VALUES
(2000000000000000006, 'ORDER_000007', 101, 7, 'iPhone 15', 1, 6999.00, 6999.00, 6999.00, 'PAID'),
(2000000000000000008, 'ORDER_000008', 103, 8, 'MacBook Air', 1, 9999.00, 9999.00, 9999.00, 'PENDING'),
(2000000000000000010, 'ORDER_000009', 105, 9, 'iPad Pro', 1, 8999.00, 8999.00, 8999.00, 'SHIPPED');

-- 插入到 t_order_1 (user_id 奇数, id 奇数)
INSERT INTO `t_order_1` (`id`, `order_no`, `user_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `amount`, `total_amount`, `status`) VALUES
(2000000000000000007, 'ORDER_000010', 101, 10, 'Apple TV', 1, 1499.00, 1499.00, 1499.00, 'COMPLETED'),
(2000000000000000009, 'ORDER_000011', 103, 11, 'HomePod', 1, 2299.00, 2299.00, 2299.00, 'CANCELLED'),
(2000000000000000011, 'ORDER_000012', 105, 12, 'Studio Display', 1, 11999.00, 11999.00, 11999.00, 'PAID');

-- ====================================
-- 验证数据插入结果
-- ====================================

-- 查询 nebula_sharding_0 中的数据
SELECT 'nebula_sharding_0.t_order_0' as table_name, COUNT(*) as record_count FROM `nebula_sharding_0`.`t_order_0` WHERE `deleted` = 0
UNION ALL
SELECT 'nebula_sharding_0.t_order_1' as table_name, COUNT(*) as record_count FROM `nebula_sharding_0`.`t_order_1` WHERE `deleted` = 0
UNION ALL
-- 查询 nebula_sharding_1 中的数据  
SELECT 'nebula_sharding_1.t_order_0' as table_name, COUNT(*) as record_count FROM `nebula_sharding_1`.`t_order_0` WHERE `deleted` = 0
UNION ALL
SELECT 'nebula_sharding_1.t_order_1' as table_name, COUNT(*) as record_count FROM `nebula_sharding_1`.`t_order_1` WHERE `deleted` = 0;

-- 分片规则验证
-- 用户ID为偶数(100,102,104) -> nebula_sharding_0 (ds0)
-- 用户ID为奇数(101,103,105) -> nebula_sharding_1 (ds1)
-- 订单ID为偶数 -> t_order_0
-- 订单ID为奇数 -> t_order_1

COMMIT;
