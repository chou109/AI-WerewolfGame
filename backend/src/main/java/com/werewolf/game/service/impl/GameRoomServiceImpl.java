package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.GameRoom;
import com.werewolf.game.mapper.GameRoomMapper;
import com.werewolf.game.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 游戏房间服务实现类
 */
@Service
public class GameRoomServiceImpl extends ServiceImpl<GameRoomMapper, GameRoom> implements GameRoomService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    public List<GameRoom> getAllRooms() {
        return lambdaQuery()
                .orderByDesc(GameRoom::getCreateTime)
                .list();
    }

    @Override
    public List<GameRoom> getAvailableRooms() {
        return lambdaQuery()
                .eq(GameRoom::getStatus, 1)
                .orderByDesc(GameRoom::getCreateTime)
                .list();
    }

    @Override
    @Transactional
    public boolean startGame(Long roomId) {
        GameRoom room = getById(roomId);
        if (room != null) {
            jdbcTemplate.update(
                    "UPDATE game_player SET is_sheriff = 0, role = NULL, update_time = CURRENT_TIMESTAMP " +
                            "WHERE room_id = ? AND status = 1",
                    roomId
            );
            jdbcTemplate.update("DELETE FROM game_state_snapshot WHERE room_id = ?", roomId);
            room.setStatus(2); // 2-游戏中
            room.setStartTime(LocalDateTime.now());
            room.setEndTime(null);
            room.setWinner(null);
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
