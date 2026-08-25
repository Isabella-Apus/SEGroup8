#!/usr/bin/env bash
set -Eeuo pipefail

release_id="${1:?release id is required}"
deploy_root="${2:?deploy root is required}"
backend_service="${3:-segroup8-backend}"
nginx_service="${4:-nginx}"
archive="/tmp/segroup8-${release_id}.tar.gz"

if [[ "$deploy_root" != /* || "$deploy_root" == "/" ]]; then
  echo "DEPLOY_PATH must be a non-root absolute path" >&2
  exit 2
fi

backend_dir="$deploy_root/backend/target"
frontend_dir="$deploy_root/frontend"
backend_jar="$backend_dir/platform-backend-0.0.1-SNAPSHOT.jar"
previous_jar="$backend_dir/platform-backend.previous.jar"
stage_dir="$(mktemp -d "$deploy_root/.deploy-${release_id}.XXXXXX")"

cleanup() {
  rm -rf -- "$stage_dir"
  rm -f -- "$archive"
}
trap cleanup EXIT

tar -xzf "$archive" -C "$stage_dir"
test -f "$stage_dir/backend/app.jar"
test -f "$stage_dir/frontend/index.html"

mkdir -p "$backend_dir" "$frontend_dir"
if [[ -f "$backend_jar" ]]; then
  cp -f -- "$backend_jar" "$previous_jar"
fi
install -m 0644 "$stage_dir/backend/app.jar" "$backend_jar.new"
mv -f -- "$backend_jar.new" "$backend_jar"

if ! sudo systemctl restart "$backend_service"; then
  if [[ -f "$previous_jar" ]]; then
    cp -f -- "$previous_jar" "$backend_jar"
    sudo systemctl restart "$backend_service" || true
  fi
  echo "Backend restart failed; previous JAR restored" >&2
  exit 1
fi

sudo nginx -t
rm -rf -- "$frontend_dir/dist.previous"
if [[ -d "$frontend_dir/dist" ]]; then
  mv -- "$frontend_dir/dist" "$frontend_dir/dist.previous"
fi
mv -- "$stage_dir/frontend" "$frontend_dir/dist"

if ! sudo systemctl reload "$nginx_service"; then
  rm -rf -- "$frontend_dir/dist"
  if [[ -d "$frontend_dir/dist.previous" ]]; then
    mv -- "$frontend_dir/dist.previous" "$frontend_dir/dist"
    sudo systemctl reload "$nginx_service" || true
  fi
  echo "Nginx reload failed; previous frontend restored" >&2
  exit 1
fi

echo "Release $release_id deployed successfully"
