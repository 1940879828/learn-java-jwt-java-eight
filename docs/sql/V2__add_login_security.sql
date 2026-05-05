-- V2: 登录安全增强 -- 失败次数限制 + 审计日志
-- 执行前请确保已执行过 init_jwt.sql

USE jwt_java_eight;

-- 1. 用户表新增锁定相关字段
ALTER TABLE sys_user ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数';
ALTER TABLE sys_user ADD COLUMN lock_time DATETIME DEFAULT NULL COMMENT '锁定时间（NULL表示未锁定），锁定时长2小时后自动解锁';

-- 2. 登录审计日志表
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
