# Chae-Chae Server

MSA(Microservices Architecture) 기반의 이커머스 백엔드 플랫폼입니다.

## 프로젝트 구조

```
msa/
├── api-gateway/          # API Gateway (Spring Cloud Gateway)
├── eureka-server/        # Service Discovery (Netflix Eureka)
├── user-service/         # 사용자 인증 및 프로필 관리
├── product-service/      # 상품 카탈로그 및 정보
├── inventory-service/    # 재고 관리 및 예약
├── order-service/        # 주문 처리 (Saga 패턴)
├── payment-service/      # 결제 처리
├── chatbot-service/      # AI 챗봇 연동
├── shared-libs/          # 공통 라이브러리
│   ├── common-exception/   # 공통 예외 처리
│   ├── common-outbox/      # Outbox 패턴 구현
│   ├── common-kafka/       # Kafka 유틸리티
│   ├── common-redis/       # Redis/Redisson 유틸리티
│   └── common-alert/       # 알림 시스템
├── monitoring/           # 모니터링 설정 (Grafana, Prometheus 등)
└── docs/                 # 문서
```

## 기술 스택

### Core
| 기술 | 버전 | 설명 |
|------|------|------|
| Java | 17 (Amazon Corretto) | 메인 언어 |
| Spring Boot | 3.5.3 | 애플리케이션 프레임워크 |
| Spring Cloud | 2025.0.0 | MSA 지원 |
| Gradle | - | 빌드 도구 |

### Spring Cloud
| 기술 | 용도 |
|------|------|
| Spring Cloud Gateway | API Gateway (WebFlux 기반) |
| Netflix Eureka | Service Discovery |
| OpenFeign | 서비스 간 HTTP 통신 |

### 데이터베이스 & 캐시
| 기술 | 버전 | 용도 |
|------|------|------|
| MySQL | 8.0 | 메인 데이터베이스 (서비스별 분리) |
| Redis | 7-alpine | 분산 캐시 (Sentinel HA 구성) |
| Caffeine | - | 로컬 캐시 (L1 캐시) |
| Redisson | 3.27.0 | Redis 클라이언트 |
| QueryDSL | 5.0.0 | Type-safe 쿼리 |

### 메시지 큐
| 기술 | 용도 |
|------|------|
| Apache Kafka | 이벤트 기반 비동기 통신 |
| Zookeeper | Kafka 코디네이션 |

### 보안
| 기술 | 버전 | 용도 |
|------|------|------|
| Spring Security | - | 보안 프레임워크 |
| JWT (jjwt) | 0.11.5 | 토큰 기반 인증 |

### Observability
| 기술 | 용도 |
|------|------|
| Prometheus | 메트릭 수집 |
| Grafana | 시각화 대시보드 |
| Loki + Promtail | 로그 수집 및 집계 |
| Tempo | 분산 트레이싱 |
| OpenTelemetry | 통합 관측성 |
| Micrometer | 메트릭 추상화 |
| Burrow | Kafka Consumer Lag 모니터링 |

### API 문서
| 기술 | 버전 |
|------|------|
| Springdoc OpenAPI | 2.7.0 |

### 테스트
| 기술 | 용도 |
|------|------|
| JUnit 5 | 테스트 프레임워크 |
| H2 | 테스트 데이터베이스 |
| Spring Kafka Test | Kafka 테스트 |
| Awaitility | 비동기 테스트 |
| JMeter | 부하 테스트 |

## 아키텍처 패턴

### Saga Pattern
- Order Service에서 분산 트랜잭션 관리
- 재고 예약 → 결제 → 주문 확정 순서로 진행
- 실패 시 보상 트랜잭션 수행

### Outbox Pattern
- Product Service에서 트랜잭셔널 메시징 구현
- DB 트랜잭션과 메시지 발행의 원자성 보장

### 2-Level Cache
- L1: Caffeine (로컬)
- L2: Redis (분산)
- 조회 성능 최적화

### Database per Service
- 각 마이크로서비스별 독립된 데이터베이스
- 데이터 격리 및 독립적 스키마 관리

## 서비스 포트

| 서비스 | 호스트 포트 | 컨테이너 포트 |
|--------|-------------|---------------|
| API Gateway | 8180 | 8080 |
| Eureka Server | 8861 | 8761 |
| User Service | 8181 | 8081 |
| Product Service | 8182 | 8082 |
| Inventory Service | 8183 | 8083 |
| Order Service | 8184 | 8084 |
| Chatbot Service | 8185 | 8085 |
| Payment Service | 8186 | 8086 |

## 인프라 구성

### Redis (Sentinel 고가용성)
- Master: 1개
- Slave: 1개
- Sentinel: 3개

### Kafka
- Broker: 1개
- Kafka-UI: 포트 8989

### 모니터링 스택
- Prometheus: 포트 9091
- Grafana: 포트 3001
- Loki: 포트 3100
- Tempo: 포트 3200

## 실행 방법

### 1. 인프라 실행
```bash
cd msa
docker-compose --profile infra up -d
```

### 2. 모니터링 스택 실행
```bash
docker-compose --profile monitoring up -d
```

### 3. 애플리케이션 실행
```bash
docker-compose --profile app up -d
```

### 전체 실행
```bash
docker-compose --profile infra --profile monitoring --profile app up -d
```

## 환경 변수

민감한 설정은 환경 변수 파일로 관리됩니다:
- `.env.gateway` - API Gateway 설정
- `.env.payment` - Payment Service 설정
- `env.mysql.*` - 서비스별 DB 설정
- `env.redis` - Redis 설정
- `env.secret.key` - JWT 시크릿 키


