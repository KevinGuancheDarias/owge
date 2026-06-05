//! Plain request/transfer POJOs mirroring the Java
//! `com.kevinguanchedarias.owgejava.pojo` package. Unlike `dto` (which is mostly
//! *output*), these are primarily *input* shapes deserialized from the frontend
//! request body, so they use `#[serde(rename_all = "camelCase")]` on the
//! `Deserialize` side.

pub mod unit_mission_information;

pub use unit_mission_information::{SelectedUnit, UnitMissionInformation};
