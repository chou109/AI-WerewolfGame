package com.werewolf.game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.werewolf.game.entity.GameRoom;

import java.util.List;

/**
 * 游戏房间服务接口
 */
public interface GameRoomService extends IService<GameRoom> {
    /**
     * 根据房间号查找房间
     */
    GameRoom findByRoomCode(String roomCode);

    /**
     * 创建游戏房间
     */
    GameRoom createRoom(GameRoom gameRoom);

    /**
     * 更新房间状态
     */
    boolean updateRoomStatus(Long roomId, Integer status);

    /**
     * 获取房间大厅列表（包含等待中、进行中和已结束房间）
     */
    List<GameRoom> getAllRooms();

    /**
     * 兼容旧接口：获取等待中的房间
     */
    List<GameRoom> getAvailableRooms();

    /**
     * 开始游戏
     */
    boolean startGame(Long roomId);

    /**
     * 结束游戏
     */
    boolean endGame(Long roomId, String winner);

    /**
     * 删除房间
     */
    boolean deleteRoom(Long roomId);

    /**
     * 校验房间密码；无密码房间直接通过
     */
    boolean verifyRoomPassword(Long roomId, String password);
}
