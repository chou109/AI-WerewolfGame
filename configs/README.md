# 板子规则配置说明

本目录不再存放运行时配置（旧的 9p_standard.json / 12p_miracle_merchant.json / 12p_wolfking_guard.json 已删除，避免与真实规则脱节）。

板子规则唯一数据源为 `backend/src/main/resources/configs/board_configs.json`（由后端 `/game/board/*` 接口对外提供），前端副本位于 `frontend/src/game/board_configs.json`，`frontend/src/game/rules.js` 直接 import 该 JSON。

修改板子配置时，必须同步更新上述两份文件；建议修改后端副本后通过 `GET /game/board/configs` 导出内容覆盖前端副本。