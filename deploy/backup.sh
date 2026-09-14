#!/usr/bin/env bash
# MySQL 매일 백업. cron 에 등록:
#   0 4 * * *  /opt/monsterhouse/BE/deploy/backup.sh >> /var/log/mh-backup.log 2>&1
# (매일 새벽 4시. 14일치 보관)
set -euo pipefail

cd "$(dirname "$0")"
set -a; . ./.env; set +a

DIR=/opt/monsterhouse/backups
mkdir -p "$DIR"
STAMP=$(date +%Y%m%d-%H%M)
FILE="$DIR/monsterhouse-$STAMP.sql.gz"

docker compose -f docker-compose.prod.yml exec -T mysql \
  mysqldump -u root -p"$DB_ROOT_PASSWORD" \
  --single-transaction --routines --triggers "$DB_NAME" | gzip > "$FILE"

echo "$(date '+%F %T')  백업 완료: $FILE ($(du -h "$FILE" | cut -f1))"

# 14일 지난 백업 삭제
find "$DIR" -name 'monsterhouse-*.sql.gz' -mtime +14 -delete

# 업로드 이미지도 함께 백업 (볼륨 → tar)
docker run --rm -v monsterhouse_uploads:/u -v "$DIR":/b alpine \
  tar czf "/b/uploads-$STAMP.tar.gz" -C /u . 2>/dev/null || true
find "$DIR" -name 'uploads-*.tar.gz' -mtime +14 -delete
