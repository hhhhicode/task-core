package io.hcode.task_core.infrastructure.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.sql.DataSource

/**
 * Liquibase 설정
 *
 * 비즈니스 의미: 데이터베이스 마이그레이션을 관리합니다.
 *
 * 기술적 배경:
 * - R2DBC와 JDBC DataSource가 모두 있을 때 명시적으로 JDBC를 사용하도록 설정
 * - Spring Boot Auto-configuration이 R2DBC를 우선하므로 JDBC DataSource를 직접 생성
 * - Liquibase는 JDBC 전용이므로 별도의 DataSource가 필요
 */
@Configuration
@EnableConfigurationProperties(LiquibaseProperties::class)
class LiquibaseConfig {

    /**
     * Liquibase 전용 JDBC DataSource
     *
     * R2DBC와 별개로 마이그레이션 전용 DataSource를 생성합니다.
     */
    @Bean
    fun liquibaseDataSource(env: Environment): DataSource {
        val config = HikariConfig()
        config.jdbcUrl = env.getProperty("spring.datasource.url")
        config.username = env.getProperty("spring.datasource.username")
        config.password = env.getProperty("spring.datasource.password")
        config.driverClassName = env.getProperty("spring.datasource.driver-class-name")
        config.maximumPoolSize = 2  // Liquibase는 적은 풀 사이즈로 충분
        return HikariDataSource(config)
    }

    @Bean
    fun liquibase(
        liquibaseDataSource: DataSource,
        properties: LiquibaseProperties
    ): SpringLiquibase {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = liquibaseDataSource
        liquibase.changeLog = properties.changeLog
        liquibase.contexts = properties.contexts?.joinToString(",")
        liquibase.defaultSchema = properties.defaultSchema
        liquibase.isDropFirst = properties.isDropFirst
        return liquibase
    }
}
