use crate::db::Db;
use crate::websocket::emitter;
use crate::OwgeResult;

pub struct UnitTypeEmitter;

impl UnitTypeEmitter {
    /// Emits `unit_type_change` — the per-user unit-type info list.
    ///
    /// Java: `socketIoService.sendMessage(userId, UNIT_TYPE_CHANGE, () -> unitTypeBo.findUnitTypesWithUserInfo(userId))`.
    pub async fn emit_unit_type_change(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "unit_type_change", || async move {
            Ok(serde_json::to_value(
                crate::bo::UnitTypeBo::find_unit_types_with_user_info(db, user_id).await?,
            )?)
        })
        .await
    }
}
