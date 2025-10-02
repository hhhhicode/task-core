plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("nu.studer.jooq") version "9.0"
}

group = "io.hcode"
version = "0.0.1-SNAPSHOT"
description = "Core backend module for the internal Project Management System."

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Starters
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// Problem+JSON for RFC 7807 error handling
	implementation("org.zalando:problem-spring-webflux:0.29.1")

	// Kotlin support
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Database migrations and JDBC for Liquibase
	implementation("org.liquibase:liquibase-core")
	implementation("org.springframework:spring-jdbc")

	// Database drivers
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")

	// jOOQ code generation dependencies
	jooqGenerator("org.postgresql:postgresql")
	jooqGenerator("org.jooq:jooq-meta-extensions-liquibase:3.19.26")
	jooqGenerator("org.liquibase:liquibase-core")

	// Test dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Testcontainers for integration tests
	testImplementation("org.testcontainers:testcontainers:1.20.4")
	testImplementation("org.testcontainers:postgresql:1.20.4")
	testImplementation("org.testcontainers:junit-jupiter:1.20.4")
	testImplementation("org.testcontainers:r2dbc:1.20.4")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// jOOQ 코드 생성 설정
jooq {
	version.set("3.19.26")

	configurations {
		create("main") {
			jooqConfiguration.apply {
				jdbc.apply {
					driver = "org.postgresql.Driver"
					url = "jdbc:postgresql://localhost:5432/task-core"
					user = "task_app"
					password = "Hjh761943!"
				}
				generator.apply {
					name = "org.jooq.codegen.DefaultGenerator"
					database.apply {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "public"
						includes = ".*"
						excludes = "databasechangelog.*"
					}
					generate.apply {
						isDeprecated = false
						isRecords = true
						isImmutablePojos = false
						isFluentSetters = true
						isJavaTimeTypes = true
					}
					target.apply {
						packageName = "io.hcode.task_core.infrastructure.jooq.generated"
						directory = "build/generated-src/jooq/main"
					}
				}
			}
		}
	}
}

// jOOQ 생성된 코드를 소스 디렉토리에 추가
sourceSets {
	main {
		java {
			srcDir("build/generated-src/jooq/main")
		}
	}
}

// Testcontainers를 위한 Docker 설정 (Rancher Desktop 지원)
tasks.withType<Test> {
	useJUnitPlatform()

	// 통합 테스트는 순차 실행 (Testcontainers 및 DB 연결 격리 보장)
	maxParallelForks = 1

	// Rancher Desktop Docker 소켓 경로 설정
	val dockerHost = System.getenv("DOCKER_HOST") ?: "unix:///Users/hcode/.rd/docker.sock"
	environment("DOCKER_HOST", dockerHost)

	// Testcontainers 설정 (Rancher Desktop에서 Ryuk 컨테이너 비활성화)
	environment("TESTCONTAINERS_RYUK_DISABLED", "true")
	environment("TESTCONTAINERS_CHECKS_DISABLE", "true")

	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showStackTraces = true
	}
}
