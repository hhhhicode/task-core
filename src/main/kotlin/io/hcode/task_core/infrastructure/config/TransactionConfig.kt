package io.hcode.task_core.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import io.r2dbc.spi.ConnectionFactory

/**
 * Transaction Manager 설정
 *
 * Liquibase가 JDBC 기반 TransactionManager를 생성하고,
 * R2DBC도 자체 TransactionManager를 생성하여 빈 충돌이 발생함.
 *
 * 애플리케이션은 R2DBC를 사용하므로 R2DBC TransactionManager를 Primary로 설정.
 */
@Configuration
class TransactionConfig {

    @Bean
    @Primary
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }
}
