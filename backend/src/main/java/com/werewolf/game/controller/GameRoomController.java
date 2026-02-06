package com.werewolf.game.controller;

import com.werewolf.game.entity.GameRoom;
import com.werewolf.game.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 游戏房间控制器
 */
@RestController
@RequestMapping("/game/room")
public class GameRoomController {

    @Autowired
    private GameRoomService gameRoomService;

    /**
     * 创建游戏房间
     */
    @PostMapping("/create")
    public Map<String, Object> createRoom(@RequestBody GameRoom gameRoom) {
        GameRoom room = gameRoomService.createRoom(gameRoom);
        return Map.of("code", 200, "message", "房间创建成功", "data", room);
    }

    /**
     * 获取房间信息
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getRoomInfo(@PathVariable Long id) {
        GameRoom room = gameRoomService.getById(id);
        if (room != null) {
            return Map.of("code", 200, "data", room);
        } else {
            return Map.of("code", 400, "message", "房间不存在");
        }
    }

    /**
     * 根据房间号获取房间
     */
    @GetMapping("/byCode/{code}")
    public Map<String, Object> getRoomByCode(@PathVariable String code) {
        GameRoom room = gameRoomService.findByRoomCode(code);
        if (room != null) {
            return Map.of("code", 200, "data", room);
        } else {
            return Map.of("code", 400, "message", "房间不存在");
        }
    }

    /**
     * 获取可用房间列表
     */
    @GetMapping("/available")
    public Map<String, Object> getAvailableRooms() {
        List<GameRoom> rooms = gameRoomService.getAvailableRooms();
        return Map.of("code", 200, "data", rooms);
    }

    /**
     * 更新房间状态
     */
    @PutMapping("/updateStatus")
    public Map<String, Object> updateRoomStatus(@RequestBody Map<String, Object> params) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        Integer status = Integer.parseInt(params.get("status").toString());
        boolean result = gameRoomService.updateRoomStatus(roomId, status);
        if (result) {
            return Map.of("code", 200, "message", "状态更新成功");
        } else {
            return Map.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 开始游戏
     */
    @PutMapping("/start")
    public Map<String, Object> startGame(@RequestBody Map<String, Object> params) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        boolean result = gameRoomService.startGame(roomId);
        if (result) {
            return Map.of("code", 200, "message", "游戏开始");
        } else {
            return Map.of("code", 400, "message", "操作失败");
        }
    }

    /**
     * 结束游戏
     */
    @PutMapping("/end")
    public Map<String, Object> endGame(@RequestBody Map<String, Object> params) {
        Long roomId = Long.parseLong(params.get("roomId").toString());
        String winner = params.get("winner").toString();
        boolean result = gameRoomService.endGame(roomId, winner);
        if (result) {
            return Map.of("code", 200, "message", "游戏结束");
        } else {
            return Map.of("code", 400, "message", "操作失败");
        }
    }

    /**
     * 删除房间
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteRoom(@PathVariable Long id) {
        boolean result = gameRoomService.deleteRoom(id);
        if (result) {
            return Map.of("code", 200, "message", "房间删除成功");
        } else {
            return Map.of("code", 400, "message", "房间删除失败");
        }
    }
}
