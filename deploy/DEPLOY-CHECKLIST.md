# 배포 실행 체크리스트 (개발자용)

사장님이 **IAM 계정 정보(콘솔 URL · 사용자이름 · 임시 비밀번호)** 를 전달하면,
개발자가 아래 순서대로 실행한다. IAM 콘솔 권한으로 **서버 생성부터 배포까지 직접** 처리.

> 선행 조건(사장님): AWS 계정 + IAM 사용자(AdministratorAccess) 생성 완료.
> 참고 문서: `SERVER-SETUP-AWS-ACCOUNT.md`(계정), `SERVER-SETUP-AWS-LIGHTSAIL.md`(서버 세부), `README.md`(배포 런북).

---

## 0. 접속 확인
- [ ] IAM 콘솔 로그인 → 임시 비밀번호를 **새 비밀번호로 변경**
- [ ] (권장) 이 IAM 사용자에도 MFA 등록
- [ ] 리전을 **서울(ap-northeast-2)** 로

## 1. Lightsail 서버 생성
- [ ] Lightsail → 인스턴스 생성 → **Ubuntu 22.04 (OS Only)**, **4GB 플랜**, 서울
- [ ] 인스턴스 이름 `monsterhouse`
- [ ] **고정 IP 생성 → 인스턴스에 연결** → IP 메모
- [ ] 방화벽(Networking)에 **80, 443** 추가 (22는 기본)
- [ ] 계정 → SSH 키 → **기본 키(.pem) 다운로드**

## 2. 서버 기본 세팅 (SSH)
- [ ] `chmod 600 default-key.pem` → `ssh -i default-key.pem ubuntu@<고정IP>`
- [ ] Docker 설치: `curl -fsSL https://get.docker.com | sh`
- [ ] `sudo usermod -aG docker $USER` → 재접속
- [ ] `sudo mkdir -p /opt/monsterhouse && sudo chown $USER /opt/monsterhouse`
- [ ] `cd /opt/monsterhouse && git clone …/BE.git && git clone …/FE.git`

## 3. 비밀값(.env)
- [ ] `cd BE/deploy && cp .env.example .env`
- [ ] `openssl rand -base64 48` → `JWT_SECRET`
- [ ] 채우기: `DB_PASSWORD`, `DB_ROOT_PASSWORD`(서로 다르게, 강하게)
- [ ] `SITE_DOMAIN=monsterhouse.co.kr`, `SITE_ORIGIN=https://monsterhouse.co.kr`
- [ ] `ACME_EMAIL=`(실제 메일 — 인증서 알림)
- [ ] `ADMIN_SEED_ENABLED=true`, `ADMIN_SEED_USERNAME`, `ADMIN_SEED_PASSWORD`(강한 값)
- [ ] (선택) MAIL_*, LINE_* — 실제 값 있으면

## 4. 도메인 연결 (가비아)
- [ ] 가비아 DNS: `monsterhouse.co.kr` A레코드 → **고정 IP**
- [ ] `www` A레코드 → 같은 고정 IP
- [ ] 전파 대기(몇 분~수십 분). **Caddy 인증서 발급 전 반드시 완료**
  - (가비아 접근이 필요 — 사장님 계정 또는 함께 진행)

## 5. 배포
- [ ] `chmod +x deploy.sh backup.sh && ./deploy.sh`
- [ ] `docker compose -f docker-compose.prod.yml logs -f caddy` 로 인증서 발급 확인

## 6. 점검
- [ ] `https://monsterhouse.co.kr` 자물쇠 확인
- [ ] `/admin` → .env 첫 관리자 계정 로그인
- [ ] 예약 1건 생성 + 이미지 업로드 확인
- [ ] 통역 신청 1건(일본어) 접수 확인

## 7. 잠그기 / 백업
- [ ] 로그인 확인 후 `.env` → `ADMIN_SEED_ENABLED=false` → `./deploy.sh` 재배포
- [ ] `crontab -e` 로 매일 4시 `backup.sh` 등록

## 8. 사장님 도움
- [ ] 루트 계정 MFA 켜기 확인
- [ ] Billing 결제 알림(예: 월 $30 초과 시 메일) 설정

---

## ⚠ 배포 전 콘텐츠 블로커 (사장님 입력 필요)
- [x] FE 실제 계좌(신한/안혜준) — **완료**
- [ ] **실제 대표 이메일** → `Footer.tsx`, `AboutPage.tsx`, `InterpreterPage.tsx` 의 `contact@monsterhouse.example` 교체
- [ ] **실제 개인정보 이메일** → `PrivacyPage.tsx` 의 `privacy@monsterhouse.example` 교체
- [ ] **실제 LINE 공식계정 URL** → `.env` 의 `VITE_LINE_ADD_FRIEND_URL`
- [ ] (선택) 인스타그램 실제 계정 → `Footer.tsx`
- [ ] (선택) 일본어 네이티브 최종 감수 — `2026-09-16-i18n-ja-review.md` §4 참고
