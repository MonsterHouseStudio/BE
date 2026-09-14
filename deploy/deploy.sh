#!/usr/bin/env bash
# MONSTER HOUSE 배포/업데이트 스크립트.
# 사용:  cd /opt/monsterhouse/BE/deploy && ./deploy.sh
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "❌ .env 가 없습니다. 'cp .env.example .env' 후 값을 채우세요."
  exit 1
fi

echo "▶ 최신 코드 받기 (BE·FE)"
git -C ..    pull --ff-only
git -C ../../FE pull --ff-only

echo "▶ 빌드 + 기동 (MySQL → Backend → Frontend → Caddy)"
docker compose -f docker-compose.prod.yml --env-file .env up -d --build

echo "▶ 상태"
docker compose -f docker-compose.prod.yml ps

cat <<'EOF'

✅ 배포 완료.
   - 사이트:   https://<도메인>
   - 관리자:   https://<도메인>/admin
   확인:  docker compose -f docker-compose.prod.yml logs -f backend
   (Flyway 마이그레이션은 backend 기동 시 자동 적용됩니다)
EOF
