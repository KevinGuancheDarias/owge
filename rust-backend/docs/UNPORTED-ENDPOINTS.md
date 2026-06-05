# Unported endpoints

Endpoints present in the Java backend that are not (fully) implemented in the
Rust port.

```
GET    /open/sponsor
GET    /open/websocket-sync/rule_change
GET    /open/websocket-sync/speed_group_change
POST   /admin/system/notify-updated-version
POST   /admin/system/run-hang-missions
DELETE /admin/cache/drop-all
GET    /game/deliver-backdoor/ping-user
GET    /admin/users/{id}/suspicions
```
