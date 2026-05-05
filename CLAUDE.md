# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Java 8 Spring Boot 2.7.6 project intended for JWT-based authentication/authorization. Currently a bare scaffold — no application logic, controllers, services, or JWT implementation exists yet.

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
