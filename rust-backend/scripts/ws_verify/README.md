# ws_verify — Rust vs Java websocket diff harness

## Prerequisites

`socket.io-client@2.4.0` is already installed at `/tmp/wsclient/node_modules`.
Run every `node` command **from `/tmp/wsclient`** (or symlink the modules into this
directory first):

```bash
# one-time symlink (optional — avoids the cd)
ln -s /tmp/wsclient/node_modules /public/owge/rust-backend/scripts/ws_verify/node_modules
```

Node 14 is required (the same version the frontend uses).  If `node` on PATH is not
14, source nvm first:

```bash
source ~/.nvm/nvm.sh && nvm use 14
```

---

## Minting a JWT

```bash
JWT=$(python3 /public/owge/rust-backend/scripts/mint_jwt.py \
        --id 1 --username rusttester --email rust@test.local)
```

The token is valid for 24 h.  Re-mint if it expires.

---

## Running ws_capture.js

```
node ws_capture.js <baseUrl> <path> <jwt> <seconds>
```

| Server  | baseUrl                    | path                    |
|---------|----------------------------|-------------------------|
| Rust    | `http://127.0.0.1:7474`    | `/socket.io`            |
| Java dc13 | `http://127.0.0.1:8123` | `/websocket/socket.io`  |

Each received event is printed as **one JSON line** on stdout:

```json
{"kind":"authentication","payload":{...}}
{"kind":"deliver","payload":{"eventName":"unit_mission_change","value":[...],"lastSent":"<TS>"}}
{"kind":"cache_clear","payload":null}
```

Normalisation applied automatically so the two outputs are diff-able:

- `authentication` payload: `value` array sorted by `eventName`.
- Every numeric `lastSent` field (at any depth) is replaced with `"<TS>"`.
- Diagnostic lines (`[connect]`, `[done]`, errors) go to **stderr** and do not
  pollute the stdout diff stream.

---

## Diff loop for an EXPLORE mission

### Step 1 — start both capture processes

```bash
# Terminal A — Rust
cd /tmp/wsclient
JWT=$(python3 /public/owge/rust-backend/scripts/mint_jwt.py \
        --id 1 --username rusttester --email rust@test.local)
node /public/owge/rust-backend/scripts/ws_verify/ws_capture.js \
  http://127.0.0.1:7474 /socket.io "$JWT" 30 \
  > /tmp/rust_ws.jsonl 2>/tmp/rust_ws.err &
RUST_PID=$!

# Terminal B — Java dc13
node /public/owge/rust-backend/scripts/ws_verify/ws_capture.js \
  http://127.0.0.1:8123 /websocket/socket.io "$JWT" 30 \
  > /tmp/java_ws.jsonl 2>/tmp/java_ws.err &
JAVA_PID=$!
```

### Step 2 — trigger an EXPLORE mission via REST

```bash
# POST to whichever server you want to test; the other server must be running a
# universe that has the same user + planet state.  Alternatively trigger via DB:
mysql -u<user> -p<pass> <db> -e "
  UPDATE scheduled_tasks
  SET    execution_time = DATE_SUB(NOW(), INTERVAL 1 SECOND)
  WHERE  task_name LIKE '%EXPLORE%'
  LIMIT 1;"
```

### Step 3 — wait for capture to finish, then diff

```bash
wait $RUST_PID $JAVA_PID

# Filter to deliver lines only and sort for stable comparison
grep '"kind":"deliver"' /tmp/rust_ws.jsonl | sort > /tmp/rust_deliver.jsonl
grep '"kind":"deliver"' /tmp/java_ws.jsonl | sort > /tmp/java_deliver.jsonl

diff /tmp/java_deliver.jsonl /tmp/rust_deliver.jsonl
```

Zero diff output = bit-for-bit match.

### Expected events for EXPLORE mission completion

| Event name | Description |
|---|---|
| `unit_mission_change` | Running missions list updated (mission removed on completion) |
| `unit_obtained_change` | Units returned to origin planet |
| `missions_count_change` | Unresolved mission count decremented |
| `planet_owned_change` | May trigger if exploration reveals/claims a planet |
| `unit_requirements_change` | Requirements re-evaluated after exploration |
| `user_data_change` | User resource/energy state updated |

Additional events that may fire depending on game state:

- `unit_type_change` — if a unit type threshold is crossed
- `unit_unlocked_change` — if a new unit is unlocked by the exploration
- `speed_impact_group_unlocked_change` — if a speed-impact group is unlocked
- `time_special_change` — if a time-special expires during the mission window

### lastSent format

Both Java and Rust emit `lastSent` as an integer **epoch seconds** (Unix timestamp).
The harness replaces these with `"<TS>"` so clock skew between the two servers does
not produce false diffs.

---

## Quick one-liner diff command (copy-paste)

```bash
source ~/.nvm/nvm.sh && nvm use 14 --silent
JWT=$(python3 /public/owge/rust-backend/scripts/mint_jwt.py --id 1 --username rusttester --email rust@test.local)
cd /tmp/wsclient
node /public/owge/rust-backend/scripts/ws_verify/ws_capture.js http://127.0.0.1:7474 /socket.io "$JWT" 30 > /tmp/rust_ws.jsonl 2>/dev/null &
node /public/owge/rust-backend/scripts/ws_verify/ws_capture.js http://127.0.0.1:8123 /websocket/socket.io "$JWT" 30 > /tmp/java_ws.jsonl 2>/dev/null &
wait
diff <(grep '"kind":"deliver"' /tmp/java_ws.jsonl | sort) \
     <(grep '"kind":"deliver"' /tmp/rust_ws.jsonl | sort)
```
