# Chae Chae Server - MSA Architecture

## 프로젝트 구조

```
chae-chae-server/
├── eureka-server/          # 서비스 디스커버리 (포트: 8761)
├── api-gateway/            # API 게이트웨이 (포트: 8080)
├── config-server/          # 설정 서버 (포트: 8888)
├── user-service/           # 사용자/인증 서비스 (포트: 8081)
├── product-service/        # 상품 서비스 (포트: 8082)
├── inventory-service/      # 재고 서비스 (포트: 8083)
├── order-service/          # 주문 서비스 (포트: 8084)
├── chatbot-service/        # 챗봇 서비스 (포트: 8085)
├── monitoring/             # 모니터링 설정
└── docker-compose.yml      # 인프라 통합 실행 파일
```

## 아키텍처

### 마이크로서비스
1. **User Service** (8081)
   - 사용자 관리
   - 인증/인가 (JWT)
   - 독립 DB: `user_db` (포트: 3307)

2. **Product Service** (8082)
   - 상품 관리
   - 상품 카탈로그
   - Redis 캐싱
   - Caffeine 로컬 캐시
   - 독립 DB: `product_db` (포트: 3308)

3. **Inventory Service** (8083)
   - 재고 관리
   - 입출고 처리
   - Kafka 이벤트 처리
   - 독립 DB: `inventory_db` (포트: 3309)

4. **Order Service** (8084)
   - 주문 생성/관리
   - Feign Client로 다른 서비스 호출
   - Kafka 이벤트 발행
   - 독립 DB: `order_db` (포트: 3310)

5. **Chatbot Service** (8085)
   - 챗봇 기능
   - 외부 API 연동
   - 독립 DB: `chatbot_db` (포트: 3311)

### 인프라 서비스
- **Eureka Server** (8761): 서비스 디스커버리
- **API Gateway** (8080): 통합 API 엔드포인트
- **Config Server** (8888): 중앙 설정 관리
- **Prometheus** (9090): 메트릭 수집
- **Grafana** (3000): 모니터링 대시보드

### 외부 의존성
- **MySQL**: 각 서비스별 독립 데이터베이스
- **Redis** (6379): 캐싱 및 세션 관리
- **Kafka** (9092): 이벤트 스트리밍
- **Zookeeper** (2181): Kafka 코디네이션

## 실행 방법

### 1. 인프라 서비스 시작 (Docker Compose)
```bash
docker-compose up -d
```

이 명령으로 다음 서비스가 시작됩니다:
- MySQL 데이터베이스들 (5개)
- Redis
- Kafka & Zookeeper
- Prometheus
- Grafana

### 2. 마이크로서비스 실행 순서

#### 순서대로 실행 (필수):
```bash
# 1. Eureka Server 시작
./gradlew :eureka-server:bootRun

# 2. Config Server 시작 (선택적)
./gradlew :config-server:bootRun

# 3. API Gateway 시작
./gradlew :api-gateway:bootRun

# 4. 비즈니스 서비스들 시작 (순서 무관)
./gradlew :user-service:bootRun
./gradlew :product-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :order-service:bootRun
./gradlew :chatbot-service:bootRun
```

#### 또는 개별 서비스 디렉토리에서:
```bash
cd user-service
./gradlew bootRun
```

### 3. 전체 프로젝트 빌드
```bash
./gradlew build
```

## API 라우팅

API Gateway를 통한 라우팅 규칙:

- `http://localhost:8080/api/users/**` → User Service
- `http://localhost:8080/api/products/**` → Product Service
- `http://localhost:8080/api/inventory/**` → Inventory Service
- `http://localhost:8080/api/orders/**` → Order Service
- `http://localhost:8080/api/chatbot/**` → Chatbot Service

## 모니터링

### Eureka Dashboard
```
http://localhost:8761
```

### Prometheus
```
http://localhost:9090
```

### Grafana
```
http://localhost:3000
기본 계정: admin / admin
```

### Swagger UI (각 서비스별)
- User Service: `http://localhost:8081/swagger-ui.html`
- Product Service: `http://localhost:8082/swagger-ui.html`
- Inventory Service: `http://localhost:8083/swagger-ui.html`
- Order Service: `http://localhost:8084/swagger-ui.html`
- Chatbot Service: `http://localhost:8085/swagger-ui.html`

## 데이터베이스 연결 정보

각 서비스는 독립된 데이터베이스를 사용합니다:

| 서비스 | 데이터베이스 | 포트 |
|--------|------------|------|
| User Service | user_db | 3307 |
| Product Service | product_db | 3308 |
| Inventory Service | inventory_db | 3309 |
| Order Service | order_db | 3310 |
| Chatbot Service | chatbot_db | 3311 |

**공통 접속 정보:**
- Username: `root`
- Password: `password`

## 서비스 간 통신

### 동기 통신
- **Feign Client**: Order Service → User Service, Inventory Service
- **Load Balancing**: Eureka를 통한 자동 로드밸런싱

### 비동기 통신
- **Kafka**: Order Service ↔ Inventory Service
  - 주문 이벤트
  - 재고 변경 이벤트

## 개발 시 주의사항

1. **서비스 실행 순서**: Eureka Server → API Gateway → 비즈니스 서비스
2. **포트 충돌**: 각 서비스는 고유한 포트를 사용
3. **데이터베이스**: Docker Compose로 먼저 DB 실행 필요
4. **Kafka**: 이벤트 기반 서비스 사용 전 Kafka 실행 확인
5. **Redis**: 캐싱 기능 사용 전 Redis 실행 확인

## 다음 단계

1. 기존 모놀리식 코드를 각 서비스로 마이그레이션
2. 서비스 간 API 계약 정의 (Feign Client 인터페이스)
3. 공통 모델/DTO는 각 서비스에 복제 또는 공유 라이브러리 구성
4. 분산 트랜잭션 처리 (Saga 패턴 고려)
5. 서킷 브레이커 추가 (Resilience4j)
6. 중앙 로깅 시스템 구성 (ELK Stack)
7. API 문서 통합 (Swagger Aggregation)

## 문제 해결

### Eureka에 서비스가 등록되지 않는 경우
- Eureka Server가 먼저 실행되었는지 확인
- application.yml의 `eureka.client.service-url.defaultZone` 확인

### 데이터베이스 연결 실패
- Docker Compose로 MySQL이 실행 중인지 확인
- 포트가 올바른지 확인 (각 서비스별 다른 포트)

### Kafka 연결 실패
- Zookeeper와 Kafka가 실행 중인지 확인
- `docker-compose ps`로 상태 확인