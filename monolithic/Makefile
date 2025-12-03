# 주의: 들여쓰기는 반드시 탭(Tab)을 사용해야 함!

.PHONY: dev prod down clean build help

# 개발 환경 실행
dev:
	docker-compose -f docker-compose-dev.yml up -d

# 운영 환경 실행 (백그라운드)
prod:
	docker-compose -f docker-compose-dev.yml -f docker-compose-prod.yml up -d

# 컨테이너 중지
down:
	docker-compose -f docker-compose-dev.yml down

# 컨테이너 중지 + 볼륨 삭제 + 이미지 삭제
clean:
	docker-compose down -v --rmi all

# 애플리케이션 빌드
build:
	docker build -t chae-chae:latest .

# 개발 환경 재빌드 후 실행
dev-build:
	docker-compose -f docker-compose.dev.yml up --build

# 도움말 출력
help:
	@echo "사용 가능한 명령어:"
	@echo "  make dev       - 개발 환경 실행"
	@echo "  make prod      - 운영 환경 실행"
	@echo "  make down      - 컨테이너 중지"
	@echo "  make clean     - 모든 리소스 삭제"
	@echo "  make build     - 이미지 빌드"
	@echo "  make dev-build - 재빌드 후 개발 환경 실행"