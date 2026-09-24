---
name: verify-production-universe
description: Verifies the health of a live OWGE production universe using the test account, and provides composable primitives (OAuth login, universe list, universe-alive check) for calling any authenticated universe REST endpoint. Use when asked to verify/smoke-test a live production universe, check whether a universe REST backend is healthy, resolve a universe ID to its restBaseUrl, or make an authenticated call into an OWGE universe.
---

# Verify Production Universe

Verify a live OWGE production universe with the test account, and reuse the
primitives below for any authenticated universe REST call.

All scripts need `curl` and `jq`.

## Environment (single source of truth)

| Variable | Meaning |
|----------|---------|
| `OWGE_AI_USERNAME` | test account username/email |
| `OWGE_AI_PASSWORD` | test account password |
| `OWGE_AI_KGDW_URL` | base URL of the account auth server (KGDW) |
| `OWGE_AI_KGDW_U` | target universe ID to resolve/verify |

## The three primitives (composable)

All follow one rule: **stdout is exactly the artifact, diagnostics go to stderr.**
That's what lets you `$()` them and pipe them.

### 1. `scripts/oauth_login.sh`
```
TOKEN=$(./scripts/oauth_login.sh)
```
- Reads `OWGE_AI_USERNAME/PASSWORD/KGDW_URL`.
- POST `$OWGE_AI_KGDW_URL/oauth/token` (password grant) → **prints only the JWT**.
- exit: `0` ok · `1` env/usage · `2` auth failed.

### 2. `scripts/list_universes.sh`
```
./scripts/list_universes.sh            # -> full JSON list
./scripts/list_universes.sh <id>       # -> only that universe's restBaseUrl
```
- Reads `OWGE_AI_KGDW_URL`. GET `/universe/findOfficials`.
- With an ID arg it prints **only** the matching `restBaseUrl` (empty line if not found).
- exit: `0` ok · `1` env/usage · `3` ID not found.

### 3. `scripts/check_universe.sh`
```
./scripts/check_universe.sh "$TOKEN" "$OWGE_AI_KGDW_U"
```
- Resolves `restBaseUrl` via `list_universes.sh`, then GET `<restBaseUrl>/game/user/exists`
  with `Authorization: Bearer <token>`.
- On 200 it prints the body (JSON boolean: is the test user registered in this universe?).
- exit: `0` alive(200) · `1` env/usage · `3` ID not found · `4` universe unhealthy.

## The one-liner (the health check)

```bash
TOKEN=$(./scripts/oauth_login.sh)
./scripts/check_universe.sh "$TOKEN" "$OWGE_AI_KGDW_U"
```

## Reusing the primitives for OTHER endpoints

Because the primitives are the reusable machinery, a new endpoint is just glue —
no re-deriving auth or universe resolution:

```bash
TOKEN=$(./scripts/oauth_login.sh)
BASE=$(./scripts/list_universes.sh "$OWGE_AI_KGDW_U")
curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/game/user/profile"
```

> [!IMPORTANT]
> Every `/game/*` endpoint in a universe requires the `Authorization: Bearer` header.
> The token comes from `oauth_login.sh`; `restBaseUrl` comes from `list_universes.sh`.

## Mental model (the 4 steps)

1. **Login** → JWT (`oauth_login.sh`).
2. **Universe list** → JSON of official universes (`list_universes.sh`).
3. **Resolve** → pick the universe whose `id == OWGE_AI_KGDW_U`, take `restBaseUrl`.
4. **Verify** → `GET <restBaseUrl>/game/user/exists` with the Bearer token; `200` means the
   universe REST backend is healthy; body is a JSON boolean for user presence.

## Failure codes (self-diagnosing)

| code | meaning |
|------|---------|
| 0 | success / alive |
| 1 | env var or usage error |
| 2 | auth/login failed |
| 3 | universe ID not found |
| 4 | universe unhealthy / non-200 |
