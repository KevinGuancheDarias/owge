//! Shared serde serialization helpers for Jackson parity.

use chrono::NaiveDateTime;
use serde::Serializer;

/// Serialize an `f32` the way Jackson serializes a Java `float`: via its
/// shortest round-trippable decimal.
///
/// `serde_json` has no f32 number type, so a plain `f32` field is widened to
/// `f64` before formatting, which exposes the binary32 rounding (e.g. `0.15f32`
/// becomes `0.15000000596046448`). Java's Jackson prints the shortest decimal
/// that round-trips the `float` (`0.15`). We reproduce that by formatting the
/// `f32` to its shortest decimal (Rust's `f32` `Display` is shortest/round-trip)
/// and re-parsing as `f64` so serde_json emits that exact, shorter value.
pub fn serialize_f32<S: Serializer>(value: &f32, serializer: S) -> Result<S::Ok, S::Error> {
    let as_f64: f64 = value.to_string().parse().unwrap_or(*value as f64);
    serializer.serialize_f64(as_f64)
}

/// `serialize_f32` for an `Option<f32>` (emits `null` for `None`; pair with
/// `skip_serializing_if = "Option::is_none"` when Java omits nulls).
pub fn serialize_opt_f32<S: Serializer>(
    value: &Option<f32>,
    serializer: S,
) -> Result<S::Ok, S::Error> {
    match value {
        Some(v) => serialize_f32(v, serializer),
        None => serializer.serialize_none(),
    }
}

/// Serialize an `Option<NaiveDateTime>` the way Jackson serializes a
/// `java.util.Date`/`Instant` field with `WRITE_DATES_AS_TIMESTAMPS=true`: an
/// epoch-millis number (treating the naive value as UTC, matching how the rest
/// of the codebase computes millis from `NaiveDateTime`, e.g. `time_special_bo`).
/// Pair with `skip_serializing_if = "Option::is_none"` when Java omits nulls.
pub fn serialize_opt_millis<S: Serializer>(
    value: &Option<NaiveDateTime>,
    serializer: S,
) -> Result<S::Ok, S::Error> {
    match value {
        Some(dt) => serializer.serialize_i64(dt.and_utc().timestamp_millis()),
        None => serializer.serialize_none(),
    }
}

