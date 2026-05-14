-- 用户数据库
CREATE DATABASE IF NOT EXISTS food_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_user;

CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username`    VARCHAR(50)  UNIQUE NOT NULL,
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    `nickname`    VARCHAR(50),
    `avatar`      VARCHAR(255),
    `role`        TINYINT      NOT NULL COMMENT '0=学生, 1=商家',
    `phone`       VARCHAR(20),
    `email`       VARCHAR(100),
    `status`      TINYINT DEFAULT 1 COMMENT '0=禁用, 1=正常',
    `create_time` DATETIME DEFAULT NOW(),
    `update_time` DATETIME DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `merchant` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT       UNIQUE NOT NULL,
    `shop_name`   VARCHAR(100) NOT NULL,
    `shop_desc`   VARCHAR(500),
    `logo`        VARCHAR(255),
    `address`     VARCHAR(255),
    `category`    VARCHAR(50)  COMMENT '主营类目',
    `status`      TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT NOW(),
    `update_time` DATETIME DEFAULT NOW(),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文章数据库
CREATE DATABASE IF NOT EXISTS food_article DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_article;

CREATE TABLE IF NOT EXISTS `article` (
    `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
    `merchant_id`   BIGINT       NOT NULL COMMENT '发布商家',
    `title`         VARCHAR(200) NOT NULL,
    `content`       TEXT         NOT NULL,
    `cover_image`   VARCHAR(255),
    `category`      VARCHAR(50)  COMMENT '分类',
    `status`        TINYINT DEFAULT 1 COMMENT '0=草稿, 1=已发布, 2=下架',
    `view_count`    INT DEFAULT 0,
    `like_count`    INT DEFAULT 0,
    `collect_count` INT DEFAULT 0,
    `coupon_id`     BIGINT COMMENT '关联优惠券ID',
    `create_time`   DATETIME DEFAULT NOW(),
    `update_time`   DATETIME DEFAULT NOW(),
    INDEX `idx_merchant_id` (`merchant_id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_interaction` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT  NOT NULL,
    `article_id`  BIGINT  NOT NULL,
    `type`        TINYINT NOT NULL COMMENT '1=点赞, 2=收藏',
    `create_time` DATETIME DEFAULT NOW(),
    UNIQUE KEY `uk_user_article_type` (`user_id`, `article_id`, `type`),
    INDEX `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 优惠券数据库
CREATE DATABASE IF NOT EXISTS food_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_order;

CREATE TABLE IF NOT EXISTS `coupon` (
    `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
    `merchant_id`    BIGINT        NOT NULL COMMENT '商家用户ID',
    `article_id`     BIGINT        COMMENT '关联文章ID',
    `title`          VARCHAR(100)  NOT NULL,
    `description`    VARCHAR(500),
    `type`           TINYINT       NOT NULL COMMENT '1=满减券, 2=折扣券, 3=免费券',
    `threshold`      DECIMAL(10,2) DEFAULT 0 COMMENT '满减门槛金额',
    `discount`       DECIMAL(10,2) NOT NULL COMMENT '减免金额或折扣率(0.8=八折)',
    `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '券原价, 0=免费领取',
    `total_count`    INT           NOT NULL COMMENT '发行总量',
    `claimed_count`  INT           NOT NULL DEFAULT 0 COMMENT '已领取数',
    `limit_per_user` INT           NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    `start_time`     DATETIME      NOT NULL COMMENT '有效期开始',
    `end_time`       DATETIME      NOT NULL COMMENT '有效期结束',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '0=下架, 1=上架',
    `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME      DEFAULT NOW(),
    `update_time`    DATETIME      DEFAULT NOW(),
    INDEX `idx_merchant_id` (`merchant_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `coupon_id`    BIGINT       NOT NULL,
    `coupon_code`  VARCHAR(50)  NOT NULL UNIQUE COMMENT '券码',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未使用, 1=已使用, 2=已过期',
    `pay_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实际支付金额, 0=免费',
    `used_time`    DATETIME,
    `create_time`  DATETIME     DEFAULT NOW(),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_coupon_id` (`coupon_id`),
    INDEX `idx_coupon_code` (`coupon_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `coupon_order` (
    `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`        BIGINT        NOT NULL,
    `coupon_id`      BIGINT        NOT NULL,
    `user_coupon_id` BIGINT,
    `amount`         DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待支付, 1=已支付, 2=已退款',
    `pay_time`       DATETIME,
    `create_time`    DATETIME      DEFAULT NOW(),
    `update_time`    DATETIME      DEFAULT NOW(),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
