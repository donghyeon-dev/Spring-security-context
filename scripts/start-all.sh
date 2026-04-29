#!/usr/bin/env bash
# Starts all four services. SECURITY_MODE=jwt|passport (default jwt).
set -euo pipefail
cd "$(dirname "$0")/.."

MODE="${SECURITY_MODE:-jwt}"
echo "[start-all] SECURITY_MODE=${MODE}"

mkdir -p logs
trap 'kill 0' EXIT

SECURITY_MODE="$MODE" ./gradlew :auth-service:bootRun       > logs/auth.log 2>&1 &
SECURITY_MODE="$MODE" ./gradlew :gateway-service:bootRun    > logs/gateway.log 2>&1 &
SECURITY_MODE="$MODE" ./gradlew :order-service:bootRun      > logs/order.log 2>&1 &
SECURITY_MODE="$MODE" ./gradlew :inventory-service:bootRun  > logs/inventory.log 2>&1 &

wait
