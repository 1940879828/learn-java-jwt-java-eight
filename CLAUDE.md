# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Java 8 Spring Boot 2.7.6 project for JWT-based authentication/authorization with RBAC (Role-Based Access Control). This is an educational/learning project.

## Important Notes

- **Do not compile by default**: Do not run `mvn compile` or `mvn clean package` unless explicitly requested by the user
- **Educational project**: Plaintext passwords, JWT secrets, and permissive CORS configuration are intentional for learning purposes

## Build & Run

- Build: `mvn clean package`
- Run: `mvn spring-boot:run`
- Test: `mvn test`
- Single test: `mvn test -Dtest=JwtJavaEightApplicationTests`

## Stack

- MySQL 5.7.44
- Java 8 (source/target)
- Spring Boot 2.7.6
- Spring Security (via `spring-boot-starter-security`)
- JUnit 5 + Spring Security Test

## Package

All code lives under `org.example.jwtjavaeight`. Main class: `JwtJavaEightApplication`.

## notion
修改完不要进行编译和运行操作，让用户自己编译检查。