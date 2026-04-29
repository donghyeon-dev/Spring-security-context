#!/usr/bin/env bash
# End-to-end smoke test. Requires services running on 8080-8083.
set -euo pipefail

echo "==> Login"
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"tenant-A","userId":"u-1001"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
echo "JWT (truncated): ${TOKEN:0:40}..."

echo
echo "==> /orders/me via gateway"
curl -s http://localhost:8080/orders/me -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo
echo "==> POST /orders (calls inventory)"
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sku":"A1","qty":2}' | python3 -m json.tool

echo
echo "==> async context propagation"
curl -s http://localhost:8080/orders/async-context-check \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
