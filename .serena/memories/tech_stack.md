# 기술 스택

## 언어 및 플랫폼
- **언어**: Kotlin 1.9.25
- **JVM 버전**: Java 21
- **빌드 도구**: Gradle (Kotlin DSL)

## 프레임워크
- **Spring Boot**: 3.5.6
- **Spring WebFlux**: Reactive 웹 프레임워크
- **Spring Data R2DBC**: Reactive 데이터베이스 액세스

## 데이터베이스
- **DBMS**: PostgreSQL
- **드라이버**: 
  - r2dbc-postgresql (Reactive)
  - postgresql (JDBC - Liquibase용)
- **ORM/Query Builder**: JOOQ
- **마이그레이션**: Liquibase

## 주요 라이브러리
- **jackson-module-kotlin**: Kotlin JSON 직렬화/역직렬화
- **reactor-kotlin-extensions**: Reactor Kotlin 확장
- **kotlinx-coroutines-reactor**: Coroutines와 Reactor 통합
- **spring-boot-starter-validation**: 벨리데이션

## 테스트
- **테스트 프레임워크**: JUnit 5 (JUnit Platform)
- **Kotlin 테스트**: kotlin-test-junit5
- **Reactive 테스트**: reactor-test
- **Coroutines 테스트**: kotlinx-coroutines-test

## 플러그인
- kotlin("jvm")
- kotlin("plugin.spring")
- org.springframework.boot
- io.spring.dependency-management