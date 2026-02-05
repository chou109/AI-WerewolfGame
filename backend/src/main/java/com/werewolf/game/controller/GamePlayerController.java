package com.werewolf.game.controller;

import com.werewolf.game.entity.GamePlayer;
import com.werewolf.game.service.GamePlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 游戏玩家控制器
 */
@RestController
@RequestMapping("/game/player")
public class GamePlayerController {

    @Autowired
    private GamePlayerService gamePlayerService;

    /**
     * 获取房间玩家列表
     */
    @GetMapping("/list/{roomId}")
    public Map<String, Object> getPlayersByRoomId(@PathVariable Long roomId) {
        List<GamePlayer> players = gamePlayerService.getPlayersByRoomId(roomId);
        return Map.of("code", 200, "data", players);
    }

    /**
     * 获取玩家信息
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getPlayerInfo(@PathVariable Long id) {
        GamePlayer player = gamePlayerService.getById(id);
        if (player != null) {
            return Map.of("code", 200, "data", player);
        } else {
            return Map.of("code", 400, "message", "玩家不存在");
        }
    }

    /**
     * 添加玩家到房间
     */
    @PostMapping("/add")
    public Map<String, Object> addPlayerToRoom(@RequestBody Map<String, Object> params) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        Long userId = Long.parseLong(params.get("userId").toString());
        Integer playerNumber = Integer.parseInt(params.get("playerNumber").toString());
        GamePlayer player = gamePlayerService.addPlayerToRoom(roomId, userId, playerNumber);
        return Map.of("code", 200, "message", "加入房间成功", "data", player);
    }

    /**
     * 更新玩家状态
     */
    @PutMapping("/updateStatus")
    public Map<String, Object> updatePlayerStatus(@RequestBody Map<String, Object> params) {
        Long playerId = Long.parseLong(params.get("playerId").toString());
        Integer status = Integer.parseInt(params.get("status").toString());
        boolean result = gamePlayerService.updatePlayerStatus(playerId, status);
        if (result) {
            return Map.of("code", 200, "message", "状态更新成功");
        } else {
            return Map.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 更新玩家角色
     */
    @PutMapping("/updateRole")
    public Map<String, Object> updatePlayerRole(@RequestBody Map<String, Object> params) {
        Long playerId = Long.parseLong(params.get("playerId").toString());
        String role = params.get("role").toString();
        boolean result = gamePlayerService.updatePlayerRole(playerId, role);
        if (result) {
            return Map.of("code", 200, "message", "角色更新成功");
        } else {
            return Map.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 设置警长
     */
    @PutMapping("/setSheriff")
    public Map<String, Object> setSheriff(@RequestBody Map<String, Object> params) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        Long playerId = Long.parseLong(params.get("playerId").toString());
        boolean result = gamePlayerService.setSheriff(roomId, playerId);
        if (result) {
            return Map.of("code", 200, "message", "警长设置成功");
        } else {
            return Map.of("code", 400, "message", "设置失败");
        }
    }

    /**
     * 获取房间存活玩家
     */
    @GetMapping("/alive/{roomId}")
    public Map<String, Object> getAlivePlayers(@PathVariable Long roomId) {
        List<GamePlayer> players = gamePlayerService.getAlivePlayersByRoomId(roomId);
        return Map.of("code", 200, "data", players);
    }
}
