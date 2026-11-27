#!/usr/bin/env bash
# Build + publish + chuyển phiên bản + restart pm2, có kiểm tra sức khỏe và gợi ý rollback.
# Chạy trên server Ubuntu, từ thư mục gốc của dự án:   ./deploy/deploy.sh
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/fittrack-api}"
PM2_NAME="fittrack-api"
HEALTH_URL="http://127.0.0.1:5080/api/v1/health"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

STAMP="$(date +%Y%m%d%H%M%S)"
RELEASE="$APP_DIR/releases/$STAMP"

echo "==> Publish -> $RELEASE"
mkdir -p "$RELEASE" /var/log/fittrack-api
dotnet publish "$ROOT/src/FitTrack.Api" -c Release -o "$RELEASE" --nologo

PREV="$(readlink -f "$APP_DIR/current" 2>/dev/null || true)"
ln -sfn "$RELEASE" "$APP_DIR/current"

echo "==> Restart pm2"
if pm2 describe "$PM2_NAME" >/dev/null 2>&1; then
  pm2 restart "$PM2_NAME" --update-env
else
  pm2 start "$ROOT/deploy/ecosystem.config.js"
fi
pm2 save >/dev/null

echo "==> Kiểm tra sức khỏe (tối đa 30s)"
for i in $(seq 1 15); do
  if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
    echo "OK: $(curl -fsS "$HEALTH_URL")"
    # Giữ 5 release gần nhất để rollback
    ls -1dt "$APP_DIR"/releases/* | tail -n +6 | xargs -r rm -rf
    exit 0
  fi
  sleep 2
done

echo "!! API không lên được. Xem log: pm2 logs $PM2_NAME --lines 80"
if [ -n "$PREV" ] && [ -d "$PREV" ]; then
  echo "!! Rollback nhanh:  ln -sfn $PREV $APP_DIR/current && pm2 restart $PM2_NAME"
fi
exit 1
