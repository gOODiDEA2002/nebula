-- Nebula 数据访问演示表结构
-- 创建产品表用于演示数据访问功能

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `nebula_example` 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE `nebula_example`;

-- 删除已存在的表
DROP TABLE IF EXISTS `t_product`;

-- 创建产品表
CREATE TABLE `t_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '产品名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '产品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '产品价格',
    `category` VARCHAR(50) NOT NULL COMMENT '产品分类',
    `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '产品状态：ACTIVE, INACTIVE, DISCONTINUED',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_name` (`name`),
    INDEX `idx_price` (`price`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='产品表 - Nebula 数据访问层演示';

-- 插入示例数据
INSERT INTO `t_product` (`name`, `description`, `price`, `category`, `stock_quantity`, `status`) VALUES
('iPhone 15 Pro', '苹果最新旗舰手机，配备A17 Pro芯片', 8999.00, '电子产品', 100, 'ACTIVE'),
('MacBook Pro 16寸', '高性能笔记本电脑，适合专业工作', 19999.00, '电子产品', 50, 'ACTIVE'),
('AirPods Pro 2', '主动降噪无线耳机', 1899.00, '电子产品', 200, 'ACTIVE'),
('Nike Air Max 270', '舒适透气的运动鞋', 899.00, '服装', 150, 'ACTIVE'),
('Adidas三叶草T恤', '经典款休闲T恤', 299.00, '服装', 300, 'ACTIVE'),
('Levi\'s 501牛仔裤', '经典直筒牛仔裤', 599.00, '服装', 120, 'ACTIVE'),
('有机咖啡豆', '来自哥伦比亚的精品咖啡豆', 128.00, '食品', 80, 'ACTIVE'),
('日本和牛', 'A5级日本和牛，极致美味', 3888.00, '食品', 20, 'ACTIVE'),
('意大利面条', '正宗意大利进口面条', 35.00, '食品', 500, 'ACTIVE'),
('智能手表', '健康监测智能手表', 2299.00, '电子产品', 75, 'ACTIVE'),
('瑜伽垫', '高密度环保瑜伽垫', 199.00, '体育用品', 200, 'ACTIVE'),
('登山背包', '大容量户外登山背包', 799.00, '体育用品', 60, 'ACTIVE'),
('跑步鞋', '专业马拉松跑步鞋', 1299.00, '体育用品', 90, 'ACTIVE'),
('蓝牙音箱', '便携式高保真蓝牙音箱', 399.00, '电子产品', 180, 'ACTIVE'),
('保温杯', '304不锈钢保温杯', 89.00, '生活用品', 400, 'ACTIVE'),
('化妆镜', '带LED灯的化妆镜', 259.00, '生活用品', 120, 'ACTIVE'),
('电动牙刷', '声波震动电动牙刷', 199.00, '生活用品', 150, 'ACTIVE'),
('护肤套装', '保湿滋润护肤套装', 599.00, '美妆', 80, 'ACTIVE'),
('香水', '法国进口香水', 899.00, '美妆', 100, 'ACTIVE'),
('面膜', '补水保湿面膜10片装', 99.00, '美妆', 300, 'ACTIVE');

-- 创建一些状态为 INACTIVE 的产品
INSERT INTO `t_product` (`name`, `description`, `price`, `category`, `stock_quantity`, `status`) VALUES
('旧版iPad', '已停产的iPad型号', 2999.00, '电子产品', 10, 'INACTIVE'),
('过时手机壳', '已停产的手机保护壳', 49.00, '电子产品', 5, 'INACTIVE');

-- 创建一些状态为 DISCONTINUED 的产品
INSERT INTO `t_product` (`name`, `description`, `price`, `category`, `stock_quantity`, `status`) VALUES
('停产耳机', '已停产的耳机型号', 299.00, '电子产品', 0, 'DISCONTINUED'),
('旧款运动鞋', '已停产的运动鞋款式', 399.00, '服装', 0, 'DISCONTINUED');

-- 创建一些库存较低的产品用于测试
UPDATE `t_product` SET `stock_quantity` = 3 WHERE `name` = '日本和牛';
UPDATE `t_product` SET `stock_quantity` = 8 WHERE `name` = '旧版iPad';
UPDATE `t_product` SET `stock_quantity` = 2 WHERE `name` = '过时手机壳';

-- 验证数据插入
SELECT 
    COUNT(*) as total_products,
    SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_products,
    SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) as inactive_products,
    SUM(CASE WHEN status = 'DISCONTINUED' THEN 1 ELSE 0 END) as discontinued_products,
    SUM(CASE WHEN stock_quantity < 10 THEN 1 ELSE 0 END) as low_stock_products
FROM `t_product` 
WHERE `deleted` = 0;

COMMIT;
