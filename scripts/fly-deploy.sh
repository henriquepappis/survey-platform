#!/usr/bin/env bash
set -euo pipefail

# Deploy helper for Fly.io using the existing Dockerfile.
# Reads app/region from fly.toml and forwards any extra args to flyctl deploy.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FLY_TOML="$ROOT_DIR/fly.toml"

if [[ ! -f "$FLY_TOML" ]]; then
  echo "fly.toml not found in repo root ($ROOT_DIR)" >&2
  exit 1
fi

if ! command -v flyctl >/dev/null 2>&1; then
  echo "flyctl not found. Install it (brew install flyctl) and login with 'flyctl auth login'." >&2
  exit 1
fi

parse_field() {
  local key=$1
  awk -F"=" -v key="$key" '
    $1 ~ "^" key "[[:space:]]*" {
      gsub(/[[:space:]]|'\''|"/, "", $2);
      print $2;
      exit;
    }
  ' "$FLY_TOML"
}

APP_NAME="${APP_NAME:-$(parse_field "app")}"
IMAGE_LABEL="${IMAGE_LABEL:-$(date +%Y%m%d-%H%M%S)}"

if [[ -z "$APP_NAME" ]]; then
  echo "App name not found in fly.toml; set APP_NAME env var." >&2
  exit 1
fi

cd "$ROOT_DIR"

ARGS=(
  "--config" "fly.toml"
  "--app" "$APP_NAME"
  "--remote-only"
  "--image-label" "$IMAGE_LABEL"
)

if [[ $# -gt 0 ]]; then
  # Allow overriding/adding flyctl flags, e.g. ./scripts/fly-deploy.sh --strategy immediate
  ARGS+=("$@")
fi

echo "Deploying '$APP_NAME' to Fly.io (image label: $IMAGE_LABEL)..."
flyctl deploy "${ARGS[@]}"
