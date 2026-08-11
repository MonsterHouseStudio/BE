#!/usr/bin/env bash
#
# 서버에서 한 번만 실행합니다 (EC2 접속 후).
#   curl -sfL https://raw.githubusercontent.com/... 로 받거나 scp 로 올려서 실행하세요.
#
#   sudo bash bootstrap-server.sh
#
# 하는 일:
#   1. k3s 설치 (Traefik·local-path 포함)
#   2. cert-manager 설치 (HTTPS 자동 발급용)
#   3. kubectl 을 일반 사용자도 쓸 수 있게 설정
set -euo pipefail

CERT_MANAGER_VERSION="v1.16.2"

echo "==> 1/3  k3s 설치"
if command -v k3s >/dev/null 2>&1; then
  echo "    이미 설치되어 있습니다. 건너뜁니다."
else
  curl -sfL https://get.k3s.io | sh -
fi

# k3s 는 kubeconfig 를 root 전용(600)으로 만듭니다.
# 매번 sudo 를 붙이지 않도록 현재 사용자에게 복사합니다.
echo "==> 2/3  kubectl 설정"
REAL_USER="${SUDO_USER:-$USER}"
USER_HOME=$(getent passwd "$REAL_USER" | cut -d: -f6)
mkdir -p "$USER_HOME/.kube"
cp /etc/rancher/k3s/k3s.yaml "$USER_HOME/.kube/config"
chown -R "$REAL_USER":"$REAL_USER" "$USER_HOME/.kube"
chmod 600 "$USER_HOME/.kube/config"

echo "    노드가 Ready 가 될 때까지 대기..."
until k3s kubectl get nodes 2>/dev/null | grep -q " Ready "; do sleep 3; done
k3s kubectl get nodes

echo "==> 3/3  cert-manager 설치 ($CERT_MANAGER_VERSION)"
if k3s kubectl get ns cert-manager >/dev/null 2>&1; then
  echo "    이미 설치되어 있습니다. 건너뜁니다."
else
  k3s kubectl apply -f \
    "https://github.com/cert-manager/cert-manager/releases/download/${CERT_MANAGER_VERSION}/cert-manager.yaml"

  echo "    cert-manager 파드 기동 대기 (2~3분 걸릴 수 있습니다)..."
  k3s kubectl -n cert-manager wait --for=condition=Available deploy --all --timeout=300s
fi

cat <<'EOF'

==========================================================
 서버 준비 완료

 다음 확인:
   kubectl get nodes
   kubectl -n cert-manager get pods

 ⚠ 배포 전에 반드시 확인하세요
   · 도메인 A 레코드가 이 서버 공인 IP 를 가리키는지
       dig +short 도메인
   · 보안그룹에서 80, 443 이 열려 있는지
       80 이 막히면 Let's Encrypt 발급이 실패합니다
==========================================================
EOF
