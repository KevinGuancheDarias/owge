use crate::OwgeResult;
use crate::websocket::emitter;
use sqlx::MySqlConnection;

pub struct UnitTypeEmitter;

impl UnitTypeEmitter {
    /// Emits `unit_type_change` — the per-user unit-type info list.
    ///
    /// Java: `socketIoService.sendMessage(userId, UNIT_TYPE_CHANGE, () -> unitTypeBo.findUnitTypesWithUserInfo(userId))`.
    pub async fn emit_unit_type_change(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(conn, user_id, "unit_type_change", |conn| {
            Box::pin(async move {
                Ok(serde_json::to_value(
                    crate::bo::UnitTypeBo::find_unit_types_with_user_info(&mut *conn, user_id)
                        .await?,
                )?)
            })
        })
        .await
    }
}
