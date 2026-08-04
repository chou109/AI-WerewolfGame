package com.werewolf.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Stores the latest resumable game snapshot for each room.
 */
@RestController
@RequestMapping("/game/state")
public class GameStateController {

    private static final int MAX_STATE_JSON_LENGTH = 8 * 1024 * 1024;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{roomId}")
    public Map<String, Object> getState(@PathVariable Long roomId) {
        if (queryRoomStatus(roomId) == null) {
            return Map.of("code", 404, "message", "房间不存在");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT state_json, saved_at, update_time FROM game_state_snapshot WHERE room_id = ?",
                roomId
        );
        if (rows.isEmpty()) {
            return Map.of("code", 404, "message", "暂无可恢复的游戏状态");
        }
        Map<String, Object> row = rows.get(0);
        return Map.of(
                "code", 200,
                "data", Map.of(
                        "stateJson", row.get("state_json"),
                        "savedAt", row.get("saved_at"),
                        "updateTime", row.get("update_time")
                )
        );
    }

    @PutMapping
    public Map<String, Object> saveState(@RequestBody Map<String, Object> params) {
        Long roomId = parseLong(params.get("roomId"));
        String stateJson = params.get("stateJson") == null ? "" : params.get("stateJson").toString();
        Long savedAt = parseLong(params.getOrDefault("savedAt", 0));
        if (roomId == null || savedAt == null || savedAt < 0 || stateJson.trim().isEmpty()) {
            return Map.of("code", 400, "message", "游戏快照参数不完整");
        }
        if (stateJson.length() > MAX_STATE_JSON_LENGTH) {
            return Map.of("code", 413, "message", "游戏快照超过8MB限制");
        }
        Integer roomStatus = queryRoomStatus(roomId);
        if (roomStatus == null) {
            return Map.of("code", 404, "message", "房间不存在");
        }
        if (roomStatus == 3) {
            return Map.of("code", 409, "message", "已结束房间不能继续写入游戏快照");
        }
        Long currentSavedAt = querySavedAt(roomId);
        if (currentSavedAt != null && savedAt < currentSavedAt) {
            return Map.of("code", 409, "message", "快照版本较旧，已保留服务端最新状态", "savedAt", currentSavedAt);
        }
        jdbcTemplate.update(
                "INSERT INTO game_state_snapshot (room_id, state_json, saved_at) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "state_json = IF(VALUES(saved_at) >= saved_at, VALUES(state_json), state_json), " +
                        "update_time = IF(VALUES(saved_at) >= saved_at, CURRENT_TIMESTAMP, update_time), " +
                        "saved_at = GREATEST(saved_at, VALUES(saved_at))",
                roomId,
                stateJson,
                savedAt
        );
        return Map.of("code", 200, "message", "游戏状态已保存", "savedAt", Math.max(savedAt, currentSavedAt == null ? 0 : currentSavedAt));
    }

    @DeleteMapping("/{roomId}")
    public Map<String, Object> clearState(@PathVariable Long roomId) {
        jdbcTemplate.update("DELETE FROM game_state_snapshot WHERE room_id = ?", roomId);
        return Map.of("code", 200, "message", "游戏状态已清除");
    }

    private Integer queryRoomStatus(Long roomId) {
        List<Integer> statuses = jdbcTemplate.query(
                "SELECT status FROM game_room WHERE id = ?",
                (resultSet, rowNum) -> resultSet.getInt("status"),
                roomId
        );
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    private Long querySavedAt(Long roomId) {
        List<Long> timestamps = jdbcTemplate.query(
                "SELECT saved_at FROM game_state_snapshot WHERE room_id = ?",
                (resultSet, rowNum) -> resultSet.getLong("saved_at"),
                roomId
        );
        return timestamps.isEmpty() ? null : timestamps.get(0);
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
