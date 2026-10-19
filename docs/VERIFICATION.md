# Verification

This document records the local verification performed for the Bank Heist Game Services Platform.

## Environment

The project was verified locally on Windows using:

- Java 17
- Maven Wrapper
- Docker Desktop
- Docker Compose
- Docker Desktop Kubernetes
- PostgreSQL 18
- Spring Boot
- TypeScript / Node.js

## Java Game Verification

The Java Swing game and client-side game-services integration were tested using Maven:

```powershell
.\mvnw.cmd -f BankHeist\pom.xml test
