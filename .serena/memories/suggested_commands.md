# 개발 명령어

## 빌드 및 실행

### 프로젝트 빌드
```bash
./gradlew build
```

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### Clean 빌드
```bash
./gradlew clean build
```

## 테스트

### 전체 테스트 실행
```bash
./gradlew test
```

### 특정 테스트 실행
```bash
./gradlew test --tests "io.hcode.task_core.TaskCoreApplicationTests"
```

### 테스트 리포트 확인
```bash
open build/reports/tests/test/index.html
```

## 코드 품질

### Kotlin 컴파일 (타입 체크)
```bash
./gradlew compileKotlin
```

### 테스트 코드 컴파일
```bash
./gradlew compileTestKotlin
```

## 의존성 관리

### 의존성 목록 확인
```bash
./gradlew dependencies
```

### 의존성 업데이트 확인
```bash
./gradlew dependencyUpdates
```

## 데이터베이스 마이그레이션

### Liquibase 상태 확인
애플리케이션 실행 시 Liquibase가 자동으로 실행됩니다.

## 유틸리티 명령어 (macOS Darwin)

### 파일 검색
```bash
find . -name "*.kt"
```

### 코드 내용 검색
```bash
grep -r "pattern" src/
```

### 디렉토리 목록
```bash
ls -la
```

### Git 상태 확인
```bash
git status
git branch
```

## IDE
- **권장 IDE**: IntelliJ IDEA
- Kotlin 플러그인 필수
- Spring Boot 플러그인 권장