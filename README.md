# Spring Security Context — JWT + Gateway Passport (MSA Sample)

MSA 환경에서 **`SecurityContextHolder`** 를 통해 `tenantId / userId / tokenId / requestId`
를 서비스 흐름 전반에 일관되게 유지하는 샘플입니다.
두 가지 인증 흐름을 같은 코드 베이스에서 **프로파일 전환**으로 비교할 수 있습니다.

| 모드 | 다운스트림 헤더 | 검증 위치 | 외부 키 노출 |
|---|---|---|---|
| `jwt` (기본) | `Authorization: Bearer <JWT>` | 모든 서비스 | 모든 서비스 |
| `passport` | `X-Passport: <JWS>` | 게이트웨이 + 모든 서비스 | 게이트웨이만 |

분산 트레이싱은 **Micrometer Tracing + OpenTelemetry (OTLP HTTP)** 를 사용하여
모든 서비스가 동일한 `traceId`를 공유하고, 인증 직후 현재 Span 에
`tenant.id / user.id / token.id / request.id / auth.source` 속성을 추가합니다.

## 모듈 구조

```
spring-security-context/
├── common-security/         # 공통 라이브러리 (Servlet 기반)
├── auth-service/            # 8081 - JWT 발급
├── gateway-service/         # 8080 - JWT 검증 + (passport 모드) Passport 발급
├── order-service/           # 8082 - Feign 호출 측
├── inventory-service/       # 8083 - Feign 피호출 측
├── observability/           # docker-compose (Jaeger/OTLP)
└── scripts/                 # start-all.sh, demo.sh
```

## 핵심 클래스

| 클래스 | 위치 | 역할 |
|---|---|---|
| `TenantPrincipal` | `common-security` | `Authentication.getPrincipal()`에 담기는 record |
| `SecurityContextUtils` | `common-security` | 현재 컨텍스트 헬퍼 |
| `JwtAuthenticationFilter` | `common-security` | JWT 모드 인증 필터 |
| `PassportAuthenticationFilter` | `common-security` | Passport 모드 인증 필터 |
| `PassportIssuer / PassportVerifier` | `common-security` | 내부 패스포트 발급/검증 |
| `FeignAuthRelayInterceptor` | `common-security` | 모드에 따라 Authorization 또는 X-Passport 헤더 릴레이 |
| `MdcContextFilter` / `MdcTaskDecorator` | `common-security` | MDC 주입 + 비동기 전파 |
| `SecurityAwareTaskExecutorConfig` | `common-security` | `@Async`에서 SecurityContext + MDC 전파 |
| `TracingSpanEnricher` | `common-security` | 인증 직후 현재 OTel Span 에 비즈니스 속성 추가 |
| `AuthenticationGlobalFilter` | `gateway-service` | JWT 검증 → (passport 모드) Passport 발급 후 헤더 치환 |

## 빠른 시작

### 1. 빌드

```bash
./gradlew build
```

### 2. (선택) Jaeger 기동 — 분산 트레이싱 확인용

```bash
docker compose -f observability/docker-compose.observability.yml up -d
# Jaeger UI: http://localhost:16686
```

### 3. 모드별 실행

```bash
# JWT 모드 (기본)
SECURITY_MODE=jwt ./scripts/start-all.sh

# Passport 모드
SECURITY_MODE=passport ./scripts/start-all.sh
```

### 4. 데모 호출

```bash
./scripts/demo.sh
```

수동 호출 예시:

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"tenant-A","userId":"u-1001"}' | jq -r .accessToken)

# 게이트웨이 경유 — 비즈니스 코드는 두 모드 모두에서 동일한 응답
curl -s http://localhost:8080/orders -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' \
     -d '{"sku":"A1","qty":2}' | jq .

# Async 컨텍스트 전파 검증 — 워커 스레드에서도 tenantId/userId/tokenId 동일
curl -s http://localhost:8080/orders/async-context-check \
     -H "Authorization: Bearer $TOKEN" | jq .
```

## 흐름 (Passport 모드)

```
Client ── JWT ──▶ gateway-service
                    │ JWT 검증
                    │ Passport 발급 (jti=originalTokenId, exp=+30s)
                    │ Authorization 제거 / X-Passport 추가
                    ▼
                 order-service
                    │ PassportAuthenticationFilter → SecurityContext + MDC + Span tag
                    │ Feign relay: X-Passport 그대로 전달
                    ▼
                 inventory-service
                    │ 동일 SecurityContext / 동일 tokenId 복원
                    ▼
                 응답 (양쪽 service의 tenantId/userId/tokenId 동일)
```

## SECURITY_MODE 별 차이점 (정리)

| 항목 | jwt | passport |
|---|---|---|
| 외부 → 게이트웨이 | `Authorization: Bearer JWT` | 동일 |
| 게이트웨이 → 서비스 | JWT 그대로 forwarding | `Authorization` 제거, `X-Passport` 첨부 |
| 서비스 인증 필터 | `JwtAuthenticationFilter` | `PassportAuthenticationFilter` |
| Feign relay 헤더 | `Authorization` | `X-Passport` |
| 토큰 만료 영향 | 진행 중 호출도 만료될 수 있음 | 짧은 Passport(기본 30s)로 격리 |
| 외부 IdP 변경 영향 | 모든 서비스 영향 | 게이트웨이만 변경 |

## 로그 패턴

```
HH:mm:ss.SSS LEVEL [traceId/spanId] [tenantId/userId/tokenId/requestId] [authSource] logger - msg
```

## 테스트

```bash
./gradlew :common-security:test
```

## 데모 사용자

| tenantId | userId | roles |
|---|---|---|
| tenant-A | u-1001 | USER |
| tenant-A | u-1002 | USER, ADMIN |
| tenant-B | u-2001 | USER |

## 알려진 제한

- 데모 비밀키가 `application.yml`에 평문으로 들어있음 — 실제 환경에서는 Vault/KMS 사용
- 토큰 폐기(revocation) 미구현
- 실제 DB 없음 (in-memory)
