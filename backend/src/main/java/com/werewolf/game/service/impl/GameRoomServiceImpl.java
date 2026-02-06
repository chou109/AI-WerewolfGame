package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.GameRoom;
import com.werewolf.game.mapper.GameRoomMapper;
import com.werewolf.game.service.GameRoomService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 游戏房间服务实现类
 */
@Service
public class GameRoomServiceImpl extends ServiceImpl<GameRoomMapper, GameRoom> implements GameRoomService {

    @Override
    public GameRoom findByRoomCode(String roomCode) {
        return lambdaQuery().eq(GameRoom::getRoomCode, roomCode).one();
    }

    @Override
    public GameRoom createRoom(GameRoom gameRoom) {
        // 生成唯一房间号
        String roomCode = generateRoomCode();
        gameRoom.setRoomCode(roomCode);
        gameRoom.setStatus(1); // 1-等待中
        gameRoom.setCreateTime(LocalDateTime.now());
        save(gameRoom);
        return gameRoom;
    }

    @Override
    public boolean updateRoomStatus(Long roomId, Integer status) {
        GameRoom room = getById(roomId);
        if (room != null) {
            room.setStatus(status);
            room.setUpdateTime(LocalDateTime.now());
            return updateById(room);
        }
        return false;
    }

    @Override
    public List<GameRoom> getAvailableRooms() {
        return lambdaQuery().eq(GameRoom::getStatus, 1).list();
    }

    @Override
    public boolean startGame(Long roomId) {
        GameRoom room = getById(roomId);
        if (room != null) {
            room.setStatus(2); // 2-游戏中
            room.setStartTime(LocalDateTime.now());
            return updateById(room);
        }
        return false;
    }

    @Override
    public boolean endGame(Long roomId, String winner) {
        GameRoom room = getById(roomId);
        if (room != null) {
            room.setStatus(3); // 3-已结束
            room.setEndTime(LocalDateTime.now());
            room.setWinner(winner);
            return updateById(room);
        }
        return false;
    }

    @Override
    public boolean deleteRoom(Long roomId) {
        return removeById(roomId);
    }

    /**
     * 生成6位随机房间号
     */
    private String generateRoomCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}
