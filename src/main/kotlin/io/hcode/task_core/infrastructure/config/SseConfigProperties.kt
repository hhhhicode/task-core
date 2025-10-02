package io.hcode.task_core.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * SSE (Server-Sent Events) 설정 프로퍼티
 *
 * 비즈니스 의미: SSE 연결 유지 및 성능 관련 설정을 중앙 집중 관리합니다.
 *
 * 기술적 배경:
 * - Spring Boot @ConfigurationProperties로 타입 안전한 설정 관리
 * - application.yml의 task.sse.* 프로퍼티와 바인딩
 * - 프로파일별 다른 값 설정 가능 (local: 15초, test: 1초)
 *
 * 설정 예시:
 * ```yaml
 * task:
 *   sse:
 *     heartbeat-seconds: 15
 * ```
 */
@Component
@ConfigurationProperties(prefix = "task.sse")
data class SseConfigProperties(
    /**
     * Heartbeat 전송 간격 (초)
     *
     * 비즈니스 의미: 프록시/로드밸런서 타임아웃 방지를 위한 주기적 신호 간격
     *
     * 기술적 세부사항:
     * - 기본값: 15초 (PRD 요구사항)
     * - 너무 짧으면: 불필요한 네트워크 트래픽
     * - 너무 길면: 프록시가 연결을 끊을 수 있음
     * - 일반적인 프록시 타임아웃: 30-60초
     */
    var heartbeatSeconds: Long = 15
)
