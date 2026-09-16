# 배포 (AWS Lightsail · Docker Compose + Caddy)

Lightsail 인스턴스 한 대(4GB)에 Docker Compose 로
**MySQL + Backend + Frontend + Caddy(자동 HTTPS)** 를 올립니다.
같은 도메인에서 화면과 API 를 함께 내보내므로 CORS 문제가 없습니다.

> 서버는 **AWS Lightsail**, 도메인·DNS 는 **가비아** 에 그대로 둡니다.
> 배포 구성은 우분투 서버면 어느 호스팅이든 동일하게 동작합니다(카페24·가비아·EC2도 동일).

```
가비아 도메인(monsterhouse.co.kr) → Lightsail 고정 IP
   └ Caddy :443 (자동 HTTPS)
        ├ /api·/uploads → Backend :8080
        └ 그 외          → Frontend (nginx)
   Docker: MySQL + Backend + Frontend + Caddy
```

---

## 0. 서버·도메인 준비 (사장님)

가이드: `deploy/SERVER-SETUP-AWS-LIGHTSAIL.md` (사장님용 · PDF 전달본)

1. **Lightsail 인스턴스** 생성 — Ubuntu 22.04, **4GB 플랜**, **서울 리전(ap-northeast-2)**.
2. **고정 IP(Static IP)** 생성 후 인스턴스에 연결. ⚠ 안 붙이면 재시작 때 IP 가 바뀝니다.
3. Lightsail **방화벽(Networking)** 에서 **80 · 443** 추가 (22 는 기본 열림).
4. **가비아 DNS** → `monsterhouse.co.kr` 와 `www` 의 **A레코드**를 **고정 IP** 로.
5. 개발자에게 전달: **고정 IP** + **기본 SSH 키(.pem)**. 접속 계정은 `ubuntu`.

## 1. 서버 접속 & 기본 세팅 (최초 1회)

```bash
# 로컬에서 키 권한 조정 후 접속 (계정은 ubuntu, 비밀번호 없음)
chmod 600 default-key.pem
ssh -i default-key.pem ubuntu@<고정IP>

# --- 이하 서버 안에서 ---
# Docker + Compose
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # 로그아웃 후 재접속(재접속해야 docker 명령이 sudo 없이 됩니다)

# 코드 (BE·FE 를 형제로)
sudo mkdir -p /opt/monsterhouse && sudo chown $USER /opt/monsterhouse
cd /opt/monsterhouse
git clone https://github.com/MonsterHouseStudio/BE.git
git clone https://github.com/MonsterHouseStudio/FE.git
```

## 2. 비밀값 설정

```bash
cd /opt/monsterhouse/BE/deploy
cp .env.example .env
openssl rand -base64 48        # JWT_SECRET 로 붙여넣기
nano .env                      # 값 채우기 (DB 비번, JWT, 도메인, ACME 이메일, 첫 관리자)
```

## 3. 배포

```bash
chmod +x deploy.sh backup.sh
./deploy.sh
```

- MySQL → Backend(마이그레이션 자동) → Frontend → Caddy 순으로 뜹니다.
- Caddy 가 도메인 인증서를 자동 발급합니다(수십 초). `docker compose -f docker-compose.prod.yml logs -f caddy` 로 확인.
- ⚠ 인증서 발급은 **가비아 A레코드가 고정 IP 로 전파된 뒤**에 됩니다(전파 몇 분~수십 분).

## 4. 점검

- `https://monsterhouse.co.kr` 접속 (자물쇠 확인)
- `https://monsterhouse.co.kr/admin` → **.env 의 첫 관리자 계정**으로 로그인
- 예약 한 건 넣어보고, 이미지 업로드 확인

## 5. 첫 관리자 계정 잠그기 (중요)

로그인 확인했으면 **시드를 끕니다**:

```bash
nano .env      # ADMIN_SEED_ENABLED=false
./deploy.sh    # 재배포
```

(계정은 이미 DB 에 있으니 사라지지 않습니다. 시드만 꺼집니다.)

## 6. 자동 백업 (권장)

```bash
crontab -e
# 매일 새벽 4시 — DB + 업로드 이미지 백업, 14일 보관
0 4 * * *  /opt/monsterhouse/BE/deploy/backup.sh >> /var/log/mh-backup.log 2>&1
```

---

## 업데이트 (코드 바뀔 때마다)

```bash
cd /opt/monsterhouse/BE/deploy && ./deploy.sh
```
`git pull` → 재빌드 → 무중단에 가깝게 교체됩니다.

## 자주 쓰는 명령

```bash
C="docker compose -f docker-compose.prod.yml"
$C ps                 # 상태
$C logs -f backend    # 백엔드 로그
$C restart backend    # 재시작
$C down               # 전체 정지 (볼륨=DB·이미지·인증서는 보존)
```

## 배포 전 체크리스트

- [ ] Lightsail 고정 IP 연결됨 + 방화벽 80·443 열림
- [ ] 가비아 A레코드가 고정 IP 로 (apex + www)
- [ ] `.env` — DB 비번·JWT_SECRET·SITE_ORIGIN·ACME_EMAIL(실제 메일)
- [ ] `lib/payment.ts` 의 **실제 계좌번호** (FE) + `isPlaceholder: false`  ← ✅ 완료(신한/안혜준)
- [ ] 일본어 감수 (`ja.json`, `messages_ja.properties`)
- [ ] 첫 로그인 후 `ADMIN_SEED_ENABLED=false`

## 운영 종료 시 (비용 정지)

```
Lightsail 콘솔 → 인스턴스 삭제 + 고정 IP 해제(Release)
```
(고정 IP 를 해제 안 하면 미사용 고정 IP 소액 과금이 계속됩니다.)
