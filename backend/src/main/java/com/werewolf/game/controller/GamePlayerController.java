package com.werewolf.game.controller;

import com.werewolf.game.entity.GamePlayer;
import com.werewolf.game.service.GamePlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import com.werewolf.game.util.MapUtil;

/**
 * 游戏玩家控制器
 */
@RestController
@RequestMapping("/game/player")
public class GamePlayerController {

    @Autowired
    private GamePlayerService gamePlayerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取房间玩家列表
     */
    @GetMapping("/list/{roomId}")
    public Map<String, Object> getPlayersByRoomId(@PathVariable Long roomId) {
        List<GamePlayer> players = gamePlayerService.getPlayersByRoomId(roomId);
        return MapUtil.of("code", 200, "data", players);
    }

    /**
     * 获取玩家信息
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getPlayerInfo(@PathVariable Long id) {
        GamePlayer player = gamePlayerService.getById(id);
        if (player != null) {
            return MapUtil.of("code", 200, "data", player);
        } else {
            return MapUtil.of("code", 400, "message", "玩家不存在");
        }
    }

    /**
     * 添加玩家到房间
     */
    @PostMapping("/add")
    public Map<String, Object> addPlayerToRoom(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        if (!isRoomOwner(roomId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以安排玩家入座");
        }
        Long userId = Long.parseLong(params.get("userId").toString());
        Integer playerNumber = Integer.parseInt(params.get("playerNumber").toString());
        
        // 获取AI玩家ID和玩家名称（如果有）
        Long aiPlayerId = params.containsKey("aiPlayerId") && params.get("aiPlayerId") != null ? Long.parseLong(params.get("aiPlayerId").toString()) : null;
        String playerName = params.containsKey("playerName") ? params.get("playerName").toString() : null;
        
        GamePlayer player = gamePlayerService.addPlayerToRoom(roomId, userId, playerNumber, aiPlayerId, playerName);
        return MapUtil.of("code", 200, "message", "加入房间成功", "data", player);
    }

    /**
     * 更新玩家状态
     */
    @PutMapping("/updateStatus")
    public Map<String, Object> updatePlayerStatus(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long playerId = Long.parseLong(params.get("playerId").toString());
        if (!isRoomOwnerByPlayer(playerId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以修改玩家状态");
        }
        Integer status = Integer.parseInt(params.get("status").toString());
        boolean result = gamePlayerService.updatePlayerStatus(playerId, status);
        if (result) {
            return MapUtil.of("code", 200, "message", "状态更新成功");
        } else {
            return MapUtil.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 更新玩家角色
     */
    @PutMapping("/updateRole")
    public Map<String, Object> updatePlayerRole(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long playerId = Long.parseLong(params.get("playerId").toString());
        if (!isRoomOwnerByPlayer(playerId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以修改玩家角色");
        }
        String role = params.get("role").toString();
        boolean result = gamePlayerService.updatePlayerRole(playerId, role);
        if (result) {
            return MapUtil.of("code", 200, "message", "角色更新成功");
        } else {
            return MapUtil.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 设置警长
     */
    @PutMapping("/setSheriff")
    public Map<String, Object> setSheriff(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        if (!isRoomOwner(roomId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以设置警长");
        }
        Long playerId = Long.parseLong(params.get("playerId").toString());
        boolean result = gamePlayerService.setSheriff(roomId, playerId);
        if (result) {
            return MapUtil.of("code", 200, "message", "警长设置成功");
        } else {
            return MapUtil.of("code", 400, "message", "设置失败");
        }
    }

    private boolean isRoomOwner(Long roomId, HttpServletRequest request) {
        if (roomId == null || request == null) {
            return false;
        }
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return false;
        }
        List<Long> creatorIds = jdbcTemplate.query(
                "SELECT creator_id FROM game_room WHERE id = ?",
                (resultSet, rowNum) -> resultSet.getLong("creator_id"),
                roomId
        );
        return !creatorIds.isEmpty() && creatorIds.get(0) != null
                && creatorIds.get(0).equals(Long.valueOf(userId.toString()));
    }

    private boolean isRoomOwnerByPlayer(Long playerId, HttpServletRequest request) {
        if (playerId == null || request == null) {
            return false;
        }
        List<Long> roomIds = jdbcTemplate.query(
                "SELECT room_id FROM game_player WHERE id = ?",
                (resultSet, rowNum) -> resultSet.getLong("room_id"),
                playerId
        );
        return !roomIds.isEmpty() && isRoomOwner(roomIds.get(0), request);
    }

    /**
     * 获取房间存活玩家
     */
    @GetMapping("/alive/{roomId}")
    public Map<String, Object> getAlivePlayers(@PathVariable Long roomId) {
        List<GamePlayer> players = gamePlayerService.getAlivePlayersByRoomId(roomId);
        return MapUtil.of("code", 200, "data", players);
    }
    
    /**
     * 从房间移除玩家
     */
    @PostMapping("/remove")
    public Map<String, Object> removePlayerFromRoom(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        if (!isRoomOwner(roomId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以移除玩家");
        }
        Long playerId = Long.parseLong(params.get("playerId").toString());
        boolean result = gamePlayerService.removePlayerFromRoom(roomId, playerId);
        if (result) {
            return MapUtil.of("code", 200, "message", "玩家移除成功");
        } else {
            return MapUtil.of("code", 400, "message", "玩家移除失败");
        }
    }
}
