-- JWT 双 Token 认证系统建表脚本
-- MySQL 5.7.44

CREATE DATABASE IF NOT EXISTS jwt_java_eight DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE jwt_java_eight;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(64)  NOT NULL COMMENT '用户名',
    password        VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    failed_attempts INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    lock_time       DATETIME     DEFAULT NULL COMMENT '锁定时间（NULL表示未锁定），锁定时长2小时后自动解锁',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 登录审计日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    login_ip    VARCHAR(45)  DEFAULT NULL COMMENT '登录IP',
    status      TINYINT      NOT NULL COMMENT '登录结果: 1-成功 0-失败',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    login_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录审计日志表';

-- 刷新令牌表
CREATE TABLE IF NOT EXISTS sys_refresh_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    token_hash  VARCHAR(64)  NOT NULL COMMENT 'Refresh Token的SHA256哈希',
    expire_time DATETIME     NOT NULL COMMENT '过期时间',
    device_info VARCHAR(255) DEFAULT NULL COMMENT '设备信息',
    jti_id      VARCHAR(64)  NOT NULL COMMENT 'JWT唯一标识',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_jti (jti_id),
    KEY idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌表';

-- 初始测试用户: admin / 123456
-- 密码为BCrypt加密后的123456，由Spring Security BCryptPasswordEncoder生成
INSERT INTO sys_user (username, password, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHCwf0r.IQmBz0Uoz4Lx1K', 1);
