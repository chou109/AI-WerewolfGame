package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.GamePlayer;
import com.werewolf.game.mapper.GamePlayerMapper;
import com.werewolf.game.service.GamePlayerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 游戏玩家服务实现类
 */
@Service
public class GamePlayerServiceImpl extends ServiceImpl<GamePlayerMapper, GamePlayer> implements GamePlayerService {

    @Override
    public List<GamePlayer> getPlayersByRoomId(Long roomId) {
        return lambdaQuery().eq(GamePlayer::getRoomId, roomId).list();
    }

    @Override
    public GamePlayer getPlayerByRoomIdAndNumber(Long roomId, Integer playerNumber) {
        return lambdaQuery()
                .eq(GamePlayer::getRoomId, roomId)
                .eq(GamePlayer::getPlayerNumber, playerNumber)
                .one();
    }

    @Override
    public GamePlayer addPlayerToRoom(Long roomId, Long userId, Integer playerNumber) {
        GamePlayer player = new GamePlayer();
        player.setRoomId(roomId);
        player.setUserId(userId);
        player.setPlayerNumber(playerNumber);
        player.setStatus(1); // 1-存活
        player.setIsSheriff(0); // 0-不是警长
        player.setCreateTime(LocalDateTime.now());
        player.setUpdateTime(LocalDateTime.now());
        save(player);
        return player;
    }

    @Override
    public boolean updatePlayerStatus(Long playerId, Integer status) {
        GamePlayer player = getById(playerId);
        if (player != null) {
            player.setStatus(status);
            player.setUpdateTime(LocalDateTime.now());
            return updateById(player);
        }
        return false;
    }

    @Override
    public boolean updatePlayerRole(Long playerId, String role) {
        GamePlayer player = getById(playerId);
        if (player != null) {
            player.setRole(role);
            player.setUpdateTime(LocalDateTime.now());
            return updateById(player);
        }
        return false;
    }

    @Override
    public boolean setSheriff(Long roomId, Long playerId) {
        // 先将所有玩家的警长状态重置
        lambdaUpdate().eq(GamePlayer::getRoomId, roomId).set(GamePlayer::getIsSheriff, 0).update();
        // 设置新的警长
        GamePlayer player = getById(playerId);
        if (player != null) {
            player.setIsSheriff(1);
            player.setUpdateTime(LocalDateTime.now());
            return updateById(player);
        }
        return false;
    }

    @Override
    public List<GamePlayer> getAlivePlayersByRoomId(Long roomId) {
        return lambdaQuery()
                .eq(GamePlayer::getRoomId, roomId)
                .eq(GamePlayer::getStatus, 1)
                .list();
    }
}
