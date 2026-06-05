// ws_capture.js — socket.io-client@2.x capture harness for OWGE websocket diffing.
//
// Usage:
//   node ws_capture.js <baseUrl> <path> <jwt> [seconds]
//
// Examples:
//   node ws_capture.js http://127.0.0.1:7474 /socket.io            <jwt> 15
//   node ws_capture.js http://127.0.0.1:8123 /websocket/socket.io  <jwt> 15
//
// Output: one JSON line per received event on stdout.
// Diagnostic messages (connect, errors, done) go to stderr so stdout is clean for diff.
//
// Normalisations applied so Java and Rust outputs can be diff'd bit-for-bit:
//   - 'authentication' payload: value array sorted by eventName ascending.
//   - Every numeric field named 'lastSent' (at any depth) is replaced with '<TS>'.

'use strict';

const io = require('socket.io-client');

const base      = process.argv[2];
const path      = process.argv[3];
const token     = process.argv[4];
const listenMs  = (parseInt(process.argv[5] || '15', 10)) * 1000;

if (!base || !path || !token) {
  process.stderr.write('Usage: node ws_capture.js <baseUrl> <path> <jwt> [seconds]\n');
  process.exit(1);
}

const PROTOCOL_VERSION = '0.1.0';

// ── helpers ──────────────────────────────────────────────────────────────────

/**
 * Walk obj recursively; replace any numeric value keyed 'lastSent' with '<TS>'.
 * Returns a new deep-cloned structure (does not mutate input).
 */
function normalizeTimestamps(obj) {
  if (Array.isArray(obj)) {
    return obj.map(normalizeTimestamps);
  }
  if (obj !== null && typeof obj === 'object') {
    const out = {};
    for (const [k, v] of Object.entries(obj)) {
      if (k === 'lastSent' && typeof v === 'number') {
        out[k] = '<TS>';
      } else {
        out[k] = normalizeTimestamps(v);
      }
    }
    return out;
  }
  return obj;
}

/**
 * Normalise the authentication event payload:
 *   - Parse the JSON string body the server sends (it is double-encoded on Java).
 *   - Sort the value array by eventName.
 *   - Replace lastSent timestamps.
 */
function normalizeAuth(raw) {
  let parsed = raw;
  // Java sends authentication as a JSON-encoded string; Rust may send an object.
  if (typeof parsed === 'string') {
    try { parsed = JSON.parse(parsed); } catch (_) {}
  }
  if (parsed && Array.isArray(parsed.value)) {
    const sorted = parsed.value.slice().sort((a, b) => {
      const ea = (a && a.eventName) || '';
      const eb = (b && b.eventName) || '';
      return ea < eb ? -1 : ea > eb ? 1 : 0;
    });
    parsed = Object.assign({}, parsed, { value: sorted });
  }
  return normalizeTimestamps(parsed);
}

/**
 * Normalise a deliver_message payload.
 *   - Replace lastSent timestamps.
 */
function normalizeDeliver(raw) {
  let parsed = raw;
  if (typeof parsed === 'string') {
    try { parsed = JSON.parse(parsed); } catch (_) {}
  }
  return normalizeTimestamps(parsed);
}

function emit(kind, payload) {
  process.stdout.write(JSON.stringify({ kind, payload }) + '\n');
}

// ── socket setup ─────────────────────────────────────────────────────────────

const socket = io.connect(base, {
  path,
  reconnection: false,
  transports: ['websocket', 'polling'],
});

socket.on('connect', () => {
  process.stderr.write('[connect] id=' + socket.id + '\n');
  socket.emit('authentication', JSON.stringify({ value: token, protocol: PROTOCOL_VERSION }));
});

socket.on('connect_error', (e) => process.stderr.write('[connect_error] ' + (e && e.message) + '\n'));
socket.on('error',         (e) => process.stderr.write('[error] ' + e + '\n'));
socket.on('disconnect',    (r) => process.stderr.write('[disconnect] ' + r + '\n'));

socket.on('authentication', (resp) => {
  emit('authentication', normalizeAuth(resp));
});

socket.on('deliver_message', (msg) => {
  emit('deliver', normalizeDeliver(msg));
});

socket.on('cache_clear', (msg) => {
  emit('cache_clear', normalizeTimestamps(msg));
});

setTimeout(() => {
  process.stderr.write('[done]\n');
  socket.close();
  process.exit(0);
}, listenMs);
