# jinmifood.com VPS 배포 가이드

권장 구성은 AWS Lightsail 서울 리전의 Ubuntu 24.04 LTS, 2 vCPU, 메모리 4GB 이상입니다.
초기 테스트는 2GB에서도 가능하지만 Spring Boot 빌드와 PostgreSQL을 동시에 실행할 때 여유가 적습니다.

## 1. VPS 만들기

1. AWS Lightsail 콘솔에서 `인스턴스 생성`을 선택합니다.
2. 리전은 `서울(ap-northeast-2)`을 선택합니다.
3. 플랫폼은 Linux/Unix, OS 전용, Ubuntu 24.04 LTS를 선택합니다.
4. 플랜은 4GB 메모리를 권장합니다.
5. 인스턴스 이름은 `jinmifood-prod`로 지정합니다.
6. SSH 키는 새 키를 생성하거나 기존 공개 키를 업로드합니다. 개인 키는 채팅, 이메일, 저장소에 올리지 않습니다.
7. 네트워킹에서 고정 IP를 만들어 인스턴스에 연결합니다.
8. 방화벽은 SSH 22번만 허용합니다. 가능하면 접속 원본을 본인 IP로 제한합니다. 80, 443, 8080, 5432는 열지 않습니다.

## 2. 서버 기본 준비

SSH 키로 접속한 뒤 Docker 공식 저장소 방식으로 Docker Engine과 Compose 플러그인을 설치합니다.
설치 후 아래 항목을 확인합니다.

```sh
docker --version
docker compose version
```

서버에는 `/opt/jinmifood` 경로로 프로젝트를 배치하고, 소유권은 배포 사용자에게 지정합니다.

## 3. Cloudflare Named Tunnel 만들기

1. Cloudflare 대시보드에서 `Networking > Tunnels > Create a tunnel`로 이동합니다.
2. Cloudflared 터널 이름을 `jinmifood-prod`로 생성합니다.
3. Public Hostname에 `jinmifood.com`을 추가하고 Service를 `HTTP`, URL을 `app:8080`으로 설정합니다.
4. `www.jinmifood.com`도 사용할 경우 같은 방식으로 하나 더 추가합니다.
5. Docker 설치 명령 전체를 실행하지 말고 표시된 토큰만 서버의 비밀 설정 단계에서 입력합니다.

터널 토큰은 서버 접속 권한과 같은 비밀입니다. 채팅이나 소스 저장소에 붙여 넣지 않습니다.

## 4. 비밀 값 입력과 배포

프로젝트 루트에서 다음 스크립트를 실행합니다.

```sh
chmod +x deploy/*.sh
./deploy/init-secrets.sh
./deploy/deploy.sh
```

`init-secrets.sh`는 입력 내용을 화면에 표시하지 않으며 다음 파일을 권한 600으로 생성합니다.

- `.env.production`: DB, 관리자, Toss 설정
- `secrets/cloudflare-tunnel-token.txt`: Cloudflare Tunnel 토큰

두 경로는 `.gitignore`에 포함되어 있습니다. Toss는 먼저 테스트 키로 전체 결제 흐름을 검증한 뒤 운영 심사 완료 후 운영 키로 교체합니다.

## 5. 배포 확인

```sh
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 app cloudflared
curl -fsS https://jinmifood.com/actuator/health
```

정상 응답은 `UP` 상태를 포함합니다. 관리자 로그인, 상품 이미지 업로드, 장바구니, 주문 생성, Toss 테스트 승인과 실패 복귀를 순서대로 확인합니다.

## 6. 백업과 운영

```sh
./deploy/backup.sh
```

이 스크립트는 PostgreSQL 덤프와 상품 이미지 압축 파일을 만들고 서버 내 30일 초과 백업을 제거합니다. 서버 자체가 손상되는 경우를 대비해 백업은 Lightsail 스냅샷이나 별도 오브젝트 스토리지에도 복사해야 합니다.

운영 전에는 다음 항목을 반드시 완료합니다.

- Lightsail 자동 스냅샷 또는 별도 원격 백업 설정
- AWS 결제 예산 알림 설정
- Cloudflare 계정 2단계 인증 활성화
- Toss 운영 키 전환 후 소액 실결제, 취소, 중복 콜백 검증
- 실제 사업자 정보, 배송·교환·개인정보 처리방침 검수
