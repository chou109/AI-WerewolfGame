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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{roomId}")
    public Map<String, Object> getState(@PathVariable Long roomId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT state_json, update_time FROM game_state_snapshot WHERE room_id = ?",
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
                        "updateTime", row.get("update_time")
                )
        );
    }

    @PutMapping
    public Map<String, Object> saveState(@RequestBody Map<String, Object> params) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        String stateJson = params.get("stateJson").toString();
        jdbcTemplate.update(
                "INSERT INTO game_state_snapshot (room_id, state_json) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE state_json = VALUES(state_json), update_time = CURRENT_TIMESTAMP",
                roomId,
                stateJson
        );
        return Map.of("code", 200, "message", "游戏状态已保存");
    }

    @DeleteMapping("/{roomId}")
    public Map<String, Object> clearState(@PathVariable Long roomId) {
        jdbcTemplate.update("DELETE FROM game_state_snapshot WHERE room_id = ?", roomId);
        return Map.of("code", 200, "message", "游戏状态已清除");
    }
}
