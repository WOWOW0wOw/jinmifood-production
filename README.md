# 진미푸드 쇼핑몰(ai활용)

Spring Boot 3.5, Thymeleaf, PostgreSQL 기반의 건어물 쇼핑몰과 관리자 페이지입니다.

## 로컬 실행

IntelliJ에서 `D:\jinmi\pom.xml`을 Maven 프로젝트로 열고 `JinmiShopApplication`을 실행합니다.

팀 개발에서는 `.env.example`을 `.env`로 복사한 뒤 로컬 테스트 키를 입력하는 방식을 권장합니다. `.env`는 Git에서 제외되며 애플리케이션 시작 시 자동으로 읽습니다. 기존 IntelliJ 실행 설정의 환경변수도 계속 사용할 수 있습니다.

- 쇼핑몰: http://localhost:8080
- 관리자: http://localhost:8080/admin
- 개발용 관리자: `admin` / `Admin!12345`

개발용 비밀번호는 운영 환경에서 사용할 수 없습니다. 운영 배포 시 `.env.production`의 관리자 계정을 반드시 강력한 값으로 변경합니다.

## 설정과 비밀값 관리

비밀값은 소스, YAML, Docker 이미지에 저장하지 않습니다.

| 환경변수 | 용도 | 필수 환경 |
|---|---|---|
| `TOSS_MODE` | `test` 또는 `live` | 운영 |
| `TOSS_CLIENT_KEY` | 브라우저 결제용 클라이언트 키 | 결제 사용 시 |
| `TOSS_SECRET_KEY` | 서버 결제 승인용 시크릿 키 | 결제 사용 시 |
| `APP_PUBLIC_BASE_URL` | Toss 성공·실패 콜백 기준 URL | 운영 |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | 관리자 계정 | 운영 |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL 접속 정보 | 운영 |

서버 시작 시 키 쌍 누락, 테스트/라이브 키 혼용, 운영 HTTPS 누락, 예시 키와 약한 관리자 비밀번호를 검사합니다. 키 원문은 설정 객체의 문자열이나 로그에 출력하지 않습니다. Toss 연결 제한시간은 `TOSS_CONNECT_TIMEOUT`, 응답 제한시간은 `TOSS_READ_TIMEOUT`으로 조정합니다.

### Toss 키 교체

1. Toss 개발자센터에서 새 클라이언트 키와 시크릿 키를 발급합니다.
2. `.env.production`의 `TOSS_MODE`, `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY`를 한 번에 변경합니다.
3. `./deploy/deploy.sh`로 재배포합니다.
4. 테스트 결제의 승인·취소·웹훅을 확인한 다음 이전 키를 폐기합니다.

채팅, 이슈, 커밋 메시지에 키 원문을 남기지 마세요. 노출된 키는 즉시 재발급해야 합니다.

## 구현 기능

- 반응형 상품 목록·검색·카테고리·상품 상세
- 이메일 회원가입·로그인·마이페이지와 회원별 주문내역
- 주문 시 포인트 할인, 결제완료 상품금액의 10% 적립, 취소 시 포인트 환급·회수
- 세션 장바구니, 수량 상한, 실시간 재고 재검증, 동시 주문 재고 잠금
- 주문 접수, 비회원 주문번호·연락처 조회, 관리자 주문 상태 및 재고 복구
- 관리자 상품 등록·수정, URL 식별자 중복 검증, JPG/PNG/WEBP 이미지 업로드
- 이미지 교체 시 상품정보가 섞이지 않도록 전용 수정 폼과 회귀 테스트 적용
- 원산지·제조사·중량·보관방법·소비기한 관리
- CSRF, BCrypt, CSP, HSTS, 클릭재킹·MIME 스니핑 차단, 보안 쿠키
- 로그인 및 비회원 주문조회 IP 요청 제한
- 403·404·413·429·500 사용자 안내 화면
- 이용약관·개인정보처리·교환/환불 정책 초안
- Flyway 운영 DB 마이그레이션과 Actuator 상태 점검

## 운영 배포

운영 구성은 `Cloudflare Named Tunnel → Spring Boot → PostgreSQL`이며 외부에 애플리케이션과 DB 포트를 공개하지 않습니다.
VPS 준비부터 배포, 검증, 백업 절차는 [VPS_DEPLOYMENT.md](VPS_DEPLOYMENT.md)를 따릅니다.

운영 서버에서 아래 스크립트를 실행하면 DB 비밀번호를 자동 생성하고, 관리자·Toss·Cloudflare 값은 화면에 표시하지 않고 입력받습니다.

```sh
chmod +x deploy/*.sh
./deploy/init-secrets.sh
./deploy/deploy.sh
```

배포 상태와 로그 확인:

```sh
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 app cloudflared
curl -fsS https://jinmifood.com/actuator/health
```

DB와 상품 이미지 수동 백업:

```sh
./deploy/backup.sh
```

`backups/`를 다른 서버 또는 오브젝트 스토리지에도 주기적으로 별도 보관해야 합니다. 애플리케이션 업데이트는 새 코드를 받은 후 `./deploy/deploy.sh`를 다시 실행하면 됩니다.

## 운영 전 필수 확인

- PG사 계약 및 카드 결제 승인·취소·웹훅 연동
- 실제 사업자 정보, 통신판매업 정보, 개인정보 보호책임자, 반품 주소·비용 반영
- 식품 표시사항, 알레르기 정보, 실제 상품 사진 검수
- 문자/알림톡·송장 연동
- 일일 DB·이미지 백업과 장애 알림 구성

`/policies`의 내용은 화면 구성을 위한 초안이므로 실제 공개 전 사업자 상황과 관계 법령에 맞는 전문가 검토가 필요합니다.

## 운영 자동화

최초 1회 실행하면 매일 한국시간 03:30 백업 타이머와 2GB 스왑을 구성합니다.

```sh
chmod +x deploy/*.sh
./deploy/install-operations.sh
```

상태 확인 및 수동 백업 검증:

```sh
systemctl list-timers jinmifood-backup.timer --no-pager
sudo systemctl start jinmifood-backup.service
sudo systemctl status jinmifood-backup.service --no-pager
swapon --show
```

GitHub Actions는 push와 pull request마다 Java 21 환경에서 `mvn verify`를 실행합니다.
