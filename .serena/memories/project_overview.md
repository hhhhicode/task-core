# Task Core - 프로젝트 개요

## 프로젝트 정보
- **프로젝트명**: task-core
- **설명**: Core backend module for the internal Project Management System
- **그룹**: io.hcode
- **버전**: 0.0.1-SNAPSHOT

## 프로젝트 목적
내부 프로젝트 관리 시스템을 위한 핵심 백엔드 모듈입니다.

## 프로젝트 구조
```
task-core/
├── src/
│   ├── main/
│   │   ├── kotlin/io/hcode/task_core/
│   │   │   └── TaskCoreApplication.kt
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/changelog/
│   └── test/
│       └── kotlin/io/hcode/task_core/
│           └── TaskCoreApplicationTests.kt
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## 주요 특징
- Reactive 프로그래밍 패러다임 (WebFlux, R2DBC)
- Kotlin Coroutines 지원
- PostgreSQL 데이터베이스
- Liquibase를 통한 데이터베이스 마이그레이션
- JOOQ를 통한 타입 세이프 SQL 쿼리