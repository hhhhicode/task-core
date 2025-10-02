package io.hcode.task_core.integration

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 통합 테스트 베이스 클래스
 *
 * 비즈니스 의미: 실제 PostgreSQL 데이터베이스를 사용한 통합 테스트 환경을 제공합니다.
 *
 * 기술적 배경:
 * - Testcontainers를 사용하여 Docker 기반 PostgreSQL 컨테이너 실행
 * - 모든 통합 테스트가 동일한 컨테이너를 공유하여 성능 최적화
 * - Spring Boot Test 전체 컨텍스트 로딩
 * - WebTestClient 자동 구성
 *
 * 사용 방법:
 * ```kotlin
 * class MyControllerIntegrationTests : IntegrationTestBase() {
 *     @Autowired
 *     lateinit var webTestClient: WebTestClient
 *
 *     @Test
 *     fun `test something`() {
 *         // test code
 *     }
 * }
 * ```
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
abstract class IntegrationTestBase {

    companion object {
        /**
         * PostgreSQL Testcontainer
         *
         * 비즈니스 의미: 격리된 테스트 환경을 위한 PostgreSQL 데이터베이스
         *
         * 기술적 세부사항:
         * - postgres:15 이미지 사용
         * - 모든 테스트 클래스에서 공유 (성능 최적화)
         * - 테스트 종료 시 자동으로 컨테이너 정리
         */
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }

        /**
         * Spring 애플리케이션 프로퍼티 동적 설정
         *
         * 비즈니스 의미: Testcontainer의 동적 포트를 Spring 설정에 주입합니다.
         *
         * @param registry Spring 동적 프로퍼티 레지스트리
         */
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // JDBC URL에서 포트 추출 - JDBC와 R2DBC가 동일한 포트 사용 보장
            // jdbc:postgresql://localhost:32812/test_db에서 32812를 추출
            val jdbcUrl = postgresContainer.jdbcUrl
            val port = jdbcUrl.substringAfter("localhost:").substringBefore("/")
            val r2dbcUrl = "r2dbc:postgresql://${postgresContainer.host}:${port}/${postgresContainer.databaseName}"

            registry.add("spring.r2dbc.url") { r2dbcUrl }
            registry.add("spring.r2dbc.username") { postgresContainer.username }
            registry.add("spring.r2dbc.password") { postgresContainer.password }

            // JDBC DataSource 설정 (Liquibase가 이를 사용)
            registry.add("spring.datasource.url") { jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }

            // Liquibase 명시적 활성화 및 설정
            registry.add("spring.liquibase.enabled") { "true" }
            registry.add("spring.liquibase.change-log") { "classpath:db/changelog/db.changelog-master.yaml" }
        }
    }
}
