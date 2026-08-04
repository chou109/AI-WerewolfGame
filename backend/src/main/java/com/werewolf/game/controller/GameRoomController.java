package com.werewolf.game.controller;

import com.werewolf.game.entity.GameRoom;
import com.werewolf.game.service.GameLoopLeaseService;
import com.werewolf.game.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.werewolf.game.util.MapUtil;

/**
 * 游戏房间控制器
 */
@RestController
@RequestMapping("/game/room")
public class GameRoomController {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private GameLoopLeaseService gameLoopLeaseService;

    /**
     * 创建游戏房间
     */
    @PostMapping("/create")
    public Map<String, Object> createRoom(@RequestBody GameRoom gameRoom, HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            try {
                gameRoom.setCreatorId(Long.valueOf(userId.toString()));
            } catch (NumberFormatException ignored) {
                // 保留请求体中的 creatorId
            }
        }
        GameRoom room = gameRoomService.createRoom(gameRoom);
        return MapUtil.of("code", 200, "message", "房间创建成功", "data", room);
    }

    /**
     * 校验房间密码；无密码房间直接通过
     */
    @PostMapping("/verify")
    public Map<String, Object> verifyRoomPassword(@RequestBody Map<String, Object> params) {
        Long roomId = parseLong(params.get("roomId"));
        String password = params.get("password") == null ? "" : params.get("password").toString();
        if (roomId == null) {
            return MapUtil.of("code", 400, "message", "房间编号无效");
        }
        boolean verified = gameRoomService.verifyRoomPassword(roomId, password);
        if (verified) {
            return MapUtil.of("code", 200, "message", "密码验证通过");
        }
        return MapUtil.of("code", 403, "message", "房间密码错误");
    }

    /**
     * 获取房间信息
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getRoomInfo(@PathVariable Long id) {
        GameRoom room = gameRoomService.getById(id);
        if (room != null) {
            return MapUtil.of("code", 200, "data", room);
        } else {
            return MapUtil.of("code", 400, "message", "房间不存在");
        }
    }

    /**
     * 根据房间号获取房间
     */
    @GetMapping("/byCode/{code}")
    public Map<String, Object> getRoomByCode(@PathVariable String code) {
        GameRoom room = gameRoomService.findByRoomCode(code);
        if (room != null) {
            return MapUtil.of("code", 200, "data", room);
        } else {
            return MapUtil.of("code", 400, "message", "房间不存在");
        }
    }

    /**
     * 获取可用房间列表
     */
    @GetMapping("/available")
    public Map<String, Object> getAvailableRooms() {
        List<GameRoom> rooms = gameRoomService.getAvailableRooms();
        return MapUtil.of("code", 200, "data", rooms);
    }

    /**
     * 获取大厅房间列表。创建后的房间即使已开始或结束，也应能在大厅中追踪。
     */
    @GetMapping("/list")
    public Map<String, Object> getRoomList() {
        List<GameRoom> rooms = gameRoomService.getAllRooms();
        return MapUtil.of("code", 200, "data", rooms);
    }

    /**
     * 更新房间状态
     */
    @PutMapping("/updateStatus")
    public Map<String, Object> updateRoomStatus(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = parseLong(params.get("roomId"));
        Integer status = parseInteger(params.get("status"));
        if (roomId == null || status == null) {
            return MapUtil.of("code", 400, "message", "房间状态参数不完整");
        }
        if (!isRoomOwner(roomId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以修改房间状态");
        }
        boolean result = gameRoomService.updateRoomStatus(roomId, status);
        if (result) {
            return MapUtil.of("code", 200, "message", "状态更新成功");
        } else {
            return MapUtil.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 开始游戏
     */
    @PutMapping("/start")
    public Map<String, Object> startGame(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = parseLong(params.get("roomId"));
        if (roomId == null) return MapUtil.of("code", 400, "message", "房间编号无效");
        if (!isRoomOwner(roomId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以开始游戏");
        }
        boolean result = gameRoomService.startGame(roomId);
        if (result) {
            return MapUtil.of("code", 200, "message", "游戏开始");
        } else {
            return MapUtil.of("code", 400, "message", "操作失败");
        }
    }

    /**
     * 结束游戏
     */
    @PutMapping("/end")
    public Map<String, Object> endGame(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = parseLong(params.get("roomId"));
        String winner = params.get("winner") == null ? "" : params.get("winner").toString().trim();
        if (roomId == null || winner.isEmpty()) return MapUtil.of("code", 400, "message", "结束房间参数不完整");
        if (!isRoomOwner(roomId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以结束游戏");
        }
        boolean result = gameRoomService.endGame(roomId, winner);
        if (result) {
            return MapUtil.of("code", 200, "message", "游戏结束");
        } else {
            return MapUtil.of("code", 400, "message", "操作失败");
        }
    }

    /**
     * 删除房间
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteRoom(@PathVariable Long id, HttpServletRequest request) {
        if (!isRoomOwner(id, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以删除房间");
        }
        boolean result = gameRoomService.deleteRoom(id);
        if (result) {
            return MapUtil.of("code", 200, "message", "房间删除成功");
        } else {
            return MapUtil.of("code", 400, "message", "房间删除失败");
        }
    }

    /**
     * 游戏推进租约：防止多标签页/多端重复推进同一局。
     * acquire/renew/release 仅房主可用，status 房间成员均可查询。
     */
    @PostMapping("/loopLock")
    public Map<String, Object> loopLock(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long roomId = parseLong(params.get("roomId"));
        String action = params.get("action") == null ? "" : params.get("action").toString().trim().toLowerCase();
        String sessionId = params.get("sessionId") == null ? "" : params.get("sessionId").toString().trim();
        if (roomId == null || sessionId.isEmpty()) {
            return MapUtil.of("code", 400, "message", "租约参数不完整");
        }
        boolean writableAction = Arrays.asList("acquire", "renew", "release").contains(action);
        if (writableAction && !isRoomOwner(roomId, request)) {
            return MapUtil.of("code", 403, "message", "仅房主可以操作游戏推进租约");
        }
        switch (action) {
            case "acquire":
                return MapUtil.of("code", 200, "data", gameLoopLeaseService.acquire(roomId, sessionId));
            case "renew":
                return MapUtil.of("code", 200, "data", gameLoopLeaseService.renew(roomId, sessionId));
            case "release":
                return MapUtil.of("code", 200, "data", gameLoopLeaseService.release(roomId, sessionId));
            case "status":
                return MapUtil.of("code", 200, "data", gameLoopLeaseService.status(roomId));
            default:
                return MapUtil.of("code", 400, "message", "不支持的租约操作");
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
        GameRoom room = gameRoomService.getById(roomId);
        return room != null && room.getCreatorId() != null
                && room.getCreatorId().equals(Long.valueOf(userId.toString()));
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
