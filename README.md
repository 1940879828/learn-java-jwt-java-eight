# JWT Java Eight

基于 Spring Boot 2.7.6 + Java 8 的 JWT 双 Token 认证系统。

## 技术栈

- Java 8
- Spring Boot 2.7.6
- Spring Security 5.7.5
- MySQL 5.7.44
- JWT (Access Token + Refresh Token)
- Swagger / OpenAPI 3

## 构建与运行

```bash
# 编译
mvn clean package

# 运行
mvn spring-boot:run

# 测试
mvn test

# 运行单个测试
mvn test -Dtest=JwtJavaEightApplicationTests
```

### 初始化数据库

SQL 文件位于 `docs/sql` 目录：

```bash
# 执行 RBAC 表结构和初始数据
mysql -u root -p jwt_java_eight < docs/sql/V002__create_rbac_tables.sql
```

### 快速开始

1. 执行数据库初始化脚本（见上方）

2. 启动应用：
```bash
mvn spring-boot:run
```

## docker

### 新建mysql容器

```shell
docker run -d --name mysql57 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 -v ./data:/var/lib/mysql mysql:5.7 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

### 启动容器
docker start mysql57

### 停止容器
docker stop mysql57

### 重启容器
docker restart mysql57

### 删除容器 (数据会丢失，除非使用了数据卷)
docker rm -f mysql57

### 查看容器详细信息
docker inspect mysql57

### 进入容器 shell
docker exec -it mysql57 bash

### 备份数据库
docker exec mysql57 mysqldump -uroot -p你的密码 testdb > backup.sql

### 恢复数据库
docker exec -i mysql57 mysql -uroot -p你的密码 testdb < backup.sql