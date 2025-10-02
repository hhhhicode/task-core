package io.hcode.task_core.infrastructure.sse

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * SSE 이벤트 ID 생성기
 *
 * 비즈니스 의미: SSE 재구독 시 놓친 이벤트를 식별하기 위한 순차적 ID를 생성합니다.
 *
 * 기술적 배경:
 * - AtomicLong으로 thread-safe한 모노톤 증가 보장
 * - SSE Last-Event-ID 헤더 기반 재구독 지원
 * - 애플리케이션 재시작 시 1부터 다시 시작 (메모리 기반)
 *
 * 재구독 시나리오:
 * 1. 클라이언트가 이벤트 ID 100까지 수신
 * 2. 네트워크 단절로 연결 끊김
 * 3. 클라이언트가 Last-Event-ID: 100 헤더로 재구독
 * 4. 서버는 ID > 100인 이벤트만 전송
 *
 * 주의사항:
 * - 프로덕션 환경에서는 Redis 등 외부 저장소 사용 고려
 * - 현재는 단일 서버 인스턴스용 (수평 확장 시 중앙 집중 ID 관리 필요)
 */
@Component
class EventIdGenerator {

    /**
     * 현재 이벤트 ID
     *
     * 비즈니스 의미: 각 도메인 이벤트에 부여되는 고유 순차 번호
     */
    private val currentId = AtomicLong(0)

    /**
     * 다음 이벤트 ID 생성
     *
     * 비즈니스 의미: 새로운 이벤트 발행 시 순차적으로 증가하는 ID를 할당합니다.
     *
     * @return 모노톤 증가하는 이벤트 ID
     */
    fun nextId(): Long = currentId.incrementAndGet()

    /**
     * 현재 최신 이벤트 ID 조회
     *
     * 비즈니스 의미: 재구독 시 어느 이벤트부터 전송해야 하는지 판단하기 위해 사용
     *
     * @return 현재까지 발행된 가장 큰 이벤트 ID
     */
    fun getCurrentId(): Long = currentId.get()
}
