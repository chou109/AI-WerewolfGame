package com.werewolf.game.controller;

import com.werewolf.game.util.GameStateProjection;
import com.werewolf.game.util.MapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 游戏快照接口：仅房主或房间成员可读写；非房主成员只返回按查看者脱敏的投影。
 */
@RestController
@RequestMapping("/game/state")
public class GameStateController {

    private static final int MAX_STATE_JSON_LENGTH = 8 * 1024 * 1024;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{roomId}")
    public Map<String, Object> getState(@PathVariable Long roomId,
                                        @RequestParam(name = "viewerId", required = false) Long viewerId,
                                        HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (!canAccess(roomId, userId)) {
            return MapUtil.of("code", 403, "message", "无权访问该房间的游戏状态");
        }
        if (queryRoomStatus(roomId) == null) {
            return MapUtil.of("code", 404, "message", "房间不存在");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT state_json, saved_at, update_time FROM game_state_snapshot WHERE room_id = ?",
                roomId
        );
        if (rows.isEmpty()) {
            return MapUtil.of("code", 404, "message", "暂无可恢复的游戏状态");
        }
        Map<String, Object> row = rows.get(0);
        String stateJson = row.get("state_json") == null ? "" : row.get("state_json").toString();
        boolean projected = viewerId != null || !isRoomCreator(roomId, userId);
        if (projected) {
            String projectedJson = GameStateProjection.project(stateJson, viewerId);
            if (projectedJson == null) {
                return MapUtil.of("code", 500, "message", "游戏快照投影失败");
            }
            stateJson = projectedJson;
        }
        return MapUtil.of(
                "code", 200,
                "data", MapUtil.of(
                        "stateJson", stateJson,
                        "projected", projected,
                        "savedAt", row.get("saved_at"),
                        "updateTime", row.get("update_time")
                )
        );
    }

    @PutMapping
    public Map<String, Object> saveState(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = parseLong(params.get("roomId"));
        Long userId = currentUserId(request);
        if (!isRoomCreator(roomId, userId)) {
            return MapUtil.of("code", 403, "message", "仅房主可以保存游戏状态");
        }
        String stateJson = params.get("stateJson") == null ? "" : params.get("stateJson").toString();
        Long savedAt = parseLong(params.getOrDefault("savedAt", 0));
        if (roomId == null || savedAt == null || savedAt < 0 || stateJson.trim().isEmpty()) {
            return MapUtil.of("code", 400, "message", "游戏快照参数不完整");
        }
        if (stateJson.length() > MAX_STATE_JSON_LENGTH) {
            return MapUtil.of("code", 413, "message", "游戏快照超过8MB限制");
        }
        Integer roomStatus = queryRoomStatus(roomId);
        if (roomStatus == null) {
            return MapUtil.of("code", 404, "message", "房间不存在");
        }
        if (roomStatus == 3) {
            return MapUtil.of("code", 409, "message", "已结束房间不能继续写入游戏快照");
        }
        Long currentSavedAt = querySavedAt(roomId);
        if (currentSavedAt != null && savedAt < currentSavedAt) {
            return MapUtil.of("code", 409, "message", "快照版本较旧，已保留服务端最新状态", "savedAt", currentSavedAt);
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
        return MapUtil.of("code", 200, "message", "游戏状态已保存", "savedAt", Math.max(savedAt, currentSavedAt == null ? 0 : currentSavedAt));
    }

    @DeleteMapping("/{roomId}")
    public Map<String, Object> clearState(@PathVariable Long roomId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (!isRoomCreator(roomId, userId)) {
            return MapUtil.of("code", 403, "message", "仅房主可以清除游戏状态");
        }
        jdbcTemplate.update("DELETE FROM game_state_snapshot WHERE room_id = ?", roomId);
        return MapUtil.of("code", 200, "message", "游戏状态已清除");
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        try {
            return userId == null ? null : Long.valueOf(userId.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean canAccess(Long roomId, Long userId) {
        if (roomId == null || userId == null) {
            return false;
        }
        if (isRoomCreator(roomId, userId)) {
            return true;
        }
        List<Integer> memberIds = jdbcTemplate.query(
                "SELECT user_id FROM game_player WHERE room_id = ? AND user_id = ? AND status = 1",
                (resultSet, rowNum) -> resultSet.getInt("user_id"),
                roomId,
                userId
        );
        return !memberIds.isEmpty();
    }

    private boolean isRoomCreator(Long roomId, Long userId) {
        if (roomId == null || userId == null) {
            return false;
        }
        List<Long> creatorIds = jdbcTemplate.query(
                "SELECT creator_id FROM game_room WHERE id = ?",
                (resultSet, rowNum) -> resultSet.getLong("creator_id"),
                roomId
        );
        return !creatorIds.isEmpty() && creatorIds.get(0) != null && creatorIds.get(0).equals(userId);
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