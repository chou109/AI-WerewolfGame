package com.werewolf.game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.werewolf.game.entity.GamePlayer;

import java.util.List;

/**
 * 游戏玩家服务接口
 */
public interface GamePlayerService extends IService<GamePlayer> {
    /**
     * 根据房间ID获取玩家列表
     */
    List<GamePlayer> getPlayersByRoomId(Long roomId);

    /**
     * 根据房间ID和玩家编号获取玩家
     */
    GamePlayer getPlayerByRoomIdAndNumber(Long roomId, Integer playerNumber);

    /**
     * 添加玩家到房间
     */
    GamePlayer addPlayerToRoom(Long roomId, Long userId, Integer playerNumber);

    /**
     * 更新玩家状态
     */
    boolean updatePlayerStatus(Long playerId, Integer status);

    /**
     * 更新玩家角色
     */
    boolean updatePlayerRole(Long playerId, String role);

    /**
     * 设置警长
     */
    boolean setSheriff(Long roomId, Long playerId);

    /**
     * 获取房间内存活的玩家
     */
    List<GamePlayer> getAlivePlayersByRoomId(Long roomId);
}
