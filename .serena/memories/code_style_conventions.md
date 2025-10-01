# 코드 스타일 및 컨벤션

## Kotlin 코드 스타일
표준 Kotlin 코딩 컨벤션을 따릅니다.

## 네이밍 컨벤션

### 패키지명
- **스타일**: snake_case
- **예시**: `io.hcode.task_core`

### 클래스명
- **스타일**: PascalCase
- **예시**: `TaskCoreApplication`, `TaskCoreApplicationTests`

### 함수명 및 변수명
- **스타일**: camelCase
- **예시**: `runApplication`, `contextLoads`

### 상수
- **스타일**: UPPER_SNAKE_CASE (Kotlin 표준)

## 파일 구조
- 패키지 구조는 도메인 또는 기능별로 구성
- 테스트 파일은 대응하는 소스 파일과 동일한 패키지 구조 유지
- 테스트 클래스명: `{ClassName}Tests` 형식

## 코드 스타일
- **들여쓰기**: 탭 사용 (IntelliJ IDEA 기본 설정)
- **컴파일러 옵션**: `-Xjsr305=strict` (null-safety 강화)

## Spring 관련
- `@SpringBootApplication` 어노테이션 사용
- 메인 함수는 최상위 함수로 정의
- `runApplication<T>()` 함수 사용

## 테스트
- `@SpringBootTest` 어노테이션 사용
- JUnit 5 테스트 작성
- 테스트 함수명: camelCase 또는 백틱으로 감싼 설명적인 이름

## 주석 및 문서화
- 비즈니스 로직이나 복잡한 로직에는 반드시 주석 추가
- 모든 필드, 변수, DTO 속성에는 비즈니스 의도를 설명하는 주석 작성
- KDoc 사용 권장 (public API의 경우)