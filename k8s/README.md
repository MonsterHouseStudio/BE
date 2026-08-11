# MONSTER HOUSE — k3s 배포

단일 노드 k3s 기준입니다. EKS 로 옮길 때도 매니페스트는 그대로 쓰고
스토리지클래스와 Ingress 어노테이션만 바꾸면 됩니다.

## 왜 EKS 가 아니라 k3s 인가

| 방식 | 월 비용 | 비고 |
|---|---|---|
| EC2 + docker compose | ~$15 | 가장 저렴, 쿠버네티스 경험은 없음 |
| **EC2 + k3s** | **~$30** | t3.medium 1대. 이 문서 기준 |
| EKS | $100~155 | 컨트롤플레인만 월 $73 |

하루 예약 몇 건 규모에 EKS 컨트롤플레인 비용을 상시로 태울 이유가 없습니다.
매니페스트는 동일하므로 트래픽이 늘면 그대로 EKS 로 옮길 수 있습니다.

## 배포 전 준비물 (직접 하셔야 합니다)

| 항목 | 비고 |
|---|---|
| **EC2 인스턴스** | **t3.medium 권장** (2 vCPU / 4GB). t3.small 은 아래 계산 참고 |
| **도메인** | HTTPS 를 받으려면 필수입니다 |
| **DNS A 레코드** | 도메인 → EC2 공인 IP. 탄력적 IP 를 붙이세요 |
| **보안그룹** | 22(SSH), **80**, 443 개방 |
| SSH 키 | `ssh user@서버` 가 되는 상태 |

**80 을 반드시 열어야 합니다.** Let's Encrypt 가 HTTP-01 챌린지로
`http://도메인/.well-known/acme-challenge/...` 에 접속해 소유권을 확인합니다.
443 만 열면 발급이 실패합니다.

**인스턴스 크기** — 파드 요청 합계는 CPU 650m / 메모리 약 1.15Gi 입니다
(MySQL 512Mi + 백엔드 512Mi + 프론트 2×64Mi). 여기에 k3s 자체가 400~500MB 를 씁니다.
t3.small(2GB)은 여유가 거의 없어 빌드나 트래픽이 몰리면 OOM 이 납니다.

## 서버 준비 (한 번만)

```bash
scp k8s/bootstrap-server.sh user@서버:~
ssh user@서버 'sudo bash bootstrap-server.sh'
```

k3s(Traefik·local-path 포함)와 cert-manager 를 설치합니다.
cert-manager 는 k3s 에 기본 포함되지 않아 따로 깔아야 하는데,
없으면 Ingress 의 `cert-manager.io/cluster-issuer` 어노테이션이 조용히 무시됩니다.

## 1. 이미지 빌드

아래 명령은 모두 **BE 저장소 루트**에서 실행합니다.

```bash
docker build -t monsterhouse/backend:latest .
```

프론트는 **빌드 시점에** API 주소가 번들에 박힙니다. 나중에 ConfigMap 을 고쳐도
반영되지 않으니 여기서 정확히 넣으세요.

⚠ 프론트는 **별도 저장소**입니다 — https://github.com/MonsterHouseStudio/FE
아래는 BE 와 FE 를 형제 폴더로 clone 한 기준입니다.

```bash
docker build -t monsterhouse/frontend:latest \
  --build-arg VITE_USE_MOCK=false \
  --build-arg VITE_API_BASE_URL=/api \
  ../FE
```

단일 노드 k3s 는 레지스트리 없이 이미지를 직접 넣을 수 있습니다.

```bash
docker save monsterhouse/backend:latest | sudo k3s ctr images import -
docker save monsterhouse/frontend:latest | sudo k3s ctr images import -
```

## 2. 비밀값 준비

```bash
cp k8s/02-secret.example.yaml k8s/02-secret.yaml
openssl rand -base64 48        # JWT_SECRET 에 붙여넣기
```

`02-secret.yaml` 은 `.gitignore` 에 있습니다. 커밋되지 않는지 확인하세요.

## 3. 도메인 반영

두 파일의 `monsterhouse.example.com` 을 실제 도메인으로 바꿉니다.

- `01-config.yaml` — `SITE_BASE_URL`, `CORS_ALLOWED_ORIGINS`
- `40-ingress.yaml` — `tls.hosts`, `rules.host`

HTTPS 준비가 안 됐으면 `40-ingress.yaml` 의 `cert-manager.io/cluster-issuer`
어노테이션과 `tls:` 블록을 주석 처리하고, `01-config.yaml` 의
`JWT_COOKIE_SECURE` 를 `"false"` 로 내리세요.
**이걸 안 내리면 HTTP 에서 브라우저가 리프레시 쿠키를 저장하지 않아
로그인 직후 새로고침하면 풀립니다.**

## 4. 배포

로컬에서 한 줄이면 됩니다. 이미지 빌드 → 서버 전송 → 적용 → 롤아웃 확인까지 합니다.

```bash
./k8s/deploy.sh user@서버주소
```

매니페스트만 다시 적용할 때는 빌드를 건너뜁니다.

```bash
./k8s/deploy.sh user@서버주소 --skip-build
```

번호 순서대로 적용됩니다(네임스페이스 → 설정 → DB → 앱 → 인그레스 → 인증서).

### HTTPS 발급 순서

**staging 으로 먼저 확인하세요.** Let's Encrypt 운영 서버는 실패 횟수 제한이 강해서
(같은 도메인 1시간 5회) DNS 나 방화벽 문제로 몇 번 실패하면 한동안 막힙니다.

1. `40-ingress.yaml` 의 `cluster-issuer` 를 `letsencrypt-staging` 으로 바꿔 배포
2. 발급 성공 확인

   ```bash
   kubectl -n monsterhouse get certificate     # READY=True 대기
   kubectl get clusterissuer                   # READY=True 여야 함
   ```
3. 성공하면 `letsencrypt-prod` 로 바꾸고 기존 시크릿을 지운 뒤 재발급

   ```bash
   kubectl -n monsterhouse delete secret monsterhouse-tls
   kubectl apply -f k8s/40-ingress.yaml
   ```

staging 인증서는 브라우저가 신뢰하지 않아 경고가 뜹니다 — 그게 정상입니다.

### 인증서가 안 나올 때

```bash
kubectl get clusterissuer                                  # 여기부터 보세요
kubectl -n monsterhouse describe certificate monsterhouse-tls
kubectl -n monsterhouse get challenge
```

실제로 겪은 것들:

| 증상 | 원인 |
|---|---|
| `ClusterIssuer READY=False`, `forbidden domain "example.com"` | `50-cert-manager.yaml` 의 이메일을 안 바꿈 |
| `challenge` 가 pending 에서 안 넘어감 | 80 포트 차단 또는 DNS 미전파 (`dig +short 도메인` 확인) |
| `no matches for kind "ClusterIssuer"` | cert-manager 미설치 — `bootstrap-server.sh` 먼저 실행

## 5. 확인

```bash
kubectl -n monsterhouse get pods
kubectl -n monsterhouse logs -l app=backend --tail=50
kubectl -n monsterhouse port-forward svc/backend 8080:8080
curl localhost:8080/actuator/health/readiness
```

---

## 스케일아웃 전에 확인할 것

백엔드 `replicas` 를 2 이상으로 올리기 전에 아래를 처리해야 합니다.
`20-backend.yaml` 상단에도 같은 체크리스트가 있습니다.

| # | 항목 | 상태 |
|---|---|---|
| 1 | 업로드 저장소를 S3 로 | ❌ 직접 변경 필요 |
| 2 | `@Scheduled` 중복 실행 방지 | ✅ ShedLock 적용됨 |
| 3 | `strategy` 를 RollingUpdate 로 | ❌ PVC 제거 후 변경 |
| 4 | 레이트리밋 | ⚠ 파드별 인메모리 (실효 한도 ×N) |

### 1. S3 전환

`S3StorageService` 는 이미 구현되어 있어 설정만 바꾸면 됩니다.

```yaml
# 01-config.yaml
STORAGE_TYPE: "s3"
AWS_S3_BUCKET: "실제-버킷명"
CLOUDFRONT_DOMAIN: "cdn.example.com"   # 없으면 비워두면 S3 직접 URL 사용
```

자격증명은 코드에 넣지 않습니다. `DefaultCredentialsProvider` 가
환경변수 → 프로파일 → 인스턴스 역할 순으로 찾으므로,
노드에 IAM 역할을 붙이는 것이 가장 안전합니다.

그다음 `20-backend.yaml` 에서 `volumeMounts`/`volumes` 의 `uploads` 항목과
`backend-uploads` PVC 를 제거하고, `strategy` 를 바꿉니다.

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

### 4. 레이트리밋

`RateLimiter` 가 `ConcurrentHashMap` 을 씁니다. 파드마다 따로 세므로
"60초당 5회"가 파드 2개면 실효 10회가 됩니다.
동작이 깨지는 건 아니고 스팸 방어가 그만큼 헐거워지는 것뿐이라,
파드 2~3개까지는 감수하고 그 이상 늘릴 때 Redis 로 옮기는 것을 권합니다.

---

## 알아둘 것

**Flyway 와 다중 파드** — 파드 여러 개가 동시에 떠도 안전합니다.
Flyway 가 `flyway_schema_history` 에 락을 잡아 한 번만 실행됩니다.

**MySQL 백업** — PVC 는 백업이 아닙니다. 노드 디스크가 날아가면 같이 날아갑니다.
예약 데이터가 들어 있으므로 `mysqldump` 를 외부 저장소로 보내는
CronJob 을 별도로 걸어두세요.

**graceful shutdown** — `terminationGracePeriodSeconds: 45` 는
`server.shutdown=graceful` 과 알림 스레드풀의 `awaitTerminationSeconds=20`
보다 넉넉하게 잡은 값입니다. 이보다 줄이면 배포 중 발송 대기 중이던
메일·LINE 알림이 잘립니다.
