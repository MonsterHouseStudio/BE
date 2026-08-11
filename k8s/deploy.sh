#!/usr/bin/env bash
#
# 로컬(개발 PC)에서 실행합니다. 이미지를 빌드해 서버로 보내고 배포까지 합니다.
#
#   ./k8s/deploy.sh user@1.2.3.4
#   ./k8s/deploy.sh user@1.2.3.4 --skip-build     # 매니페스트만 다시 적용
#
# 전제:
#   · BE 저장소 루트에서 실행 (FE 는 ../FE 에 있다고 가정)
#   · 서버에 bootstrap-server.sh 를 이미 실행해둔 상태
#   · ssh 키로 접속이 되는 상태
#
# 레지스트리를 쓰지 않는 이유:
#   단일 노드라 이미지를 직접 넣는 편이 간단하고 비용도 0 입니다.
#   노드가 늘어나면 그때 ECR 같은 레지스트리로 바꾸세요.
set -euo pipefail

TARGET="${1:-}"
SKIP_BUILD="${2:-}"

if [[ -z "$TARGET" ]]; then
  echo "사용법: $0 user@서버주소 [--skip-build]" >&2
  exit 1
fi

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BE_DIR="$(dirname "$HERE")"
FE_DIR="$(dirname "$BE_DIR")/FE"

if [[ ! -d "$FE_DIR" ]]; then
  echo "FE 디렉터리를 찾을 수 없습니다: $FE_DIR" >&2
  echo "BE 와 FE 를 형제 폴더로 clone 했는지 확인하세요." >&2
  exit 1
fi

if [[ "$SKIP_BUILD" != "--skip-build" ]]; then
  # ★ 아키텍처 확인이 먼저입니다.
  #   x86 PC 에서 빌드한 이미지를 ARM 서버(Oracle Ampere, AWS Graviton)에 넣으면
  #   파드가 CrashLoopBackOff 로 돌면서 로그에 "exec format error" 만 남습니다.
  #   이미지도 정상, 매니페스트도 정상이라 원인을 찾기 어렵습니다.
  LOCAL_ARCH=$(docker version --format '{{.Server.Arch}}')
  REMOTE_ARCH=$(ssh "$TARGET" 'uname -m')
  case "$REMOTE_ARCH" in
    aarch64|arm64) REMOTE_ARCH=arm64 ;;
    x86_64|amd64)  REMOTE_ARCH=amd64 ;;
  esac
  echo "==> 아키텍처  로컬=$LOCAL_ARCH  서버=$REMOTE_ARCH"

  if [[ "$LOCAL_ARCH" == "$REMOTE_ARCH" ]]; then
    echo "==> 1/3  로컬 빌드"
    docker build -t monsterhouse/backend:latest "$BE_DIR"

    # ⚠ VITE_* 는 빌드 시점에 번들에 박힙니다.
    #   여기서 빠뜨리면 운영에 목 데이터가 나갑니다.
    docker build -t monsterhouse/frontend:latest \
      --build-arg VITE_USE_MOCK=false \
      --build-arg VITE_API_BASE_URL=/api \
      "$FE_DIR"

    echo "==> 2/3  이미지 전송 (수 분 걸릴 수 있습니다)"
    # 파일로 떨구지 않고 파이프로 바로 넘깁니다.
    for img in backend frontend; do
      echo "    monsterhouse/$img"
      docker save "monsterhouse/$img:latest" \
        | ssh "$TARGET" 'sudo k3s ctr images import -'
    done
  else
    # 아키텍처가 다르면 서버에서 직접 빌드합니다.
    #
    # buildx + QEMU 에뮬레이션으로 크로스 빌드도 가능하지만,
    # Gradle 빌드가 에뮬레이션에서 10배 이상 느려집니다(30분 이상).
    # 소스를 보내 서버에서 네이티브로 빌드하는 편이 훨씬 빠릅니다.
    echo "==> 1/3  아키텍처가 달라 서버에서 빌드합니다"
    echo "    서버에 docker 가 필요합니다:  sudo apt install -y docker.io"

    ssh "$TARGET" 'rm -rf ~/mh-src && mkdir -p ~/mh-src'
    # .git·node_modules·build 산출물은 보내지 않습니다.
    tar -C "$(dirname "$BE_DIR")" \
        --exclude='.git' --exclude='node_modules' --exclude='build' \
        --exclude='dist' --exclude='uploads' --exclude='.gradle' \
        -czf - "$(basename "$BE_DIR")" "$(basename "$FE_DIR")" \
      | ssh "$TARGET" 'tar -xzf - -C ~/mh-src'

    echo "==> 2/3  서버에서 빌드 + k3s 에 적재"
    ssh "$TARGET" "
      set -e
      cd ~/mh-src
      sudo docker build -t monsterhouse/backend:latest ./$(basename "$BE_DIR")
      sudo docker build -t monsterhouse/frontend:latest \
        --build-arg VITE_USE_MOCK=false \
        --build-arg VITE_API_BASE_URL=/api \
        ./$(basename "$FE_DIR")
      for img in backend frontend; do
        sudo docker save monsterhouse/\$img:latest | sudo k3s ctr images import -
      done
    "
  fi
else
  echo "==> 빌드·전송 건너뜀"
fi

echo "==> 3/3  매니페스트 적용"
# 02-secret.yaml 은 로컬에만 있고 커밋되지 않습니다. 함께 보냅니다.
if [[ ! -f "$HERE/02-secret.yaml" ]]; then
  echo "02-secret.yaml 이 없습니다." >&2
  echo "  cp k8s/02-secret.example.yaml k8s/02-secret.yaml 후 값을 채우세요." >&2
  exit 1
fi

ssh "$TARGET" 'rm -rf ~/mh-k8s && mkdir -p ~/mh-k8s'
scp -q "$HERE"/*.yaml "$TARGET:~/mh-k8s/"
# .example 은 실제 적용 대상이 아닙니다 (CHANGE_ME 가 그대로 들어갑니다).
ssh "$TARGET" 'rm -f ~/mh-k8s/02-secret.example.yaml'

ssh "$TARGET" 'kubectl apply -f ~/mh-k8s/'

echo "==> 롤아웃 대기"
ssh "$TARGET" '
  kubectl -n monsterhouse rollout status statefulset/mysql --timeout=300s
  kubectl -n monsterhouse rollout status deploy/backend  --timeout=300s
  kubectl -n monsterhouse rollout status deploy/frontend --timeout=180s
  echo
  kubectl -n monsterhouse get pods
  echo
  echo "--- 인증서 상태 (Ready=True 가 될 때까지 몇 분 걸립니다) ---"
  kubectl -n monsterhouse get certificate 2>/dev/null || echo "  (cert-manager 미설정)"
'

cat <<'EOF'

==========================================================
 배포 완료

 인증서가 아직 Ready 가 아니면:
   kubectl -n monsterhouse describe certificate monsterhouse-tls
   kubectl -n monsterhouse get challenge

 흔한 원인
   · 도메인 A 레코드가 아직 전파 안 됨
   · 보안그룹에서 80 이 막힘 (HTTP-01 챌린지가 80 을 씁니다)
==========================================================
EOF
