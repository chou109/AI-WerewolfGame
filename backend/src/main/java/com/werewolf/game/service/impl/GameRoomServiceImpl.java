package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.GameRoom;
import com.werewolf.game.mapper.GameRoomMapper;
import com.werewolf.game.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
        if (gameRoom.getPassword() != null && !gameRoom.getPassword().trim().isEmpty()) {
            gameRoom.setPassword(hashRoomPassword(gameRoom.getPassword().trim()));
        } else {
            gameRoom.setPassword(null);
        }
        if (gameRoom.getBoardVersion() == null || gameRoom.getBoardVersion().trim().isEmpty()) {
            gameRoom.setBoardVersion("1");
        }
        gameRoom.setStatus(1); // 1-等待中
        gameRoom.setCreateTime(LocalDateTime.now());
        save(gameRoom);
        return gameRoom;
    }

    @Override
    public boolean updateRoomStatus(Long roomId, Integer status) {
        GameRoom room = getById(roomId);
        if (room == null || status == null) return false;
        if (Objects.equals(room.getStatus(), status)) return true;
        if (!isValidTransition(room.getStatus(), status)) return false;
        room.setStatus(status);
        room.setUpdateTime(LocalDateTime.now());
        return updateById(room);
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
        if (room != null && Objects.equals(room.getStatus(), 2)) return true;
        if (room != null && isValidTransition(room.getStatus(), 2)) {
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
    @Transactional
    public boolean endGame(Long roomId, String winner) {
        GameRoom room = getById(roomId);
        if (room != null && Objects.equals(room.getStatus(), 3)) {
            return Objects.equals(room.getWinner(), winner);
        }
        if (room != null && isValidTransition(room.getStatus(), 3)) {
            room.setStatus(3); // 3-已结束
            room.setEndTime(LocalDateTime.now());
            room.setWinner(winner);
            return updateById(room);
        }
        return false;
    }

    /**
     * 游戏状态只能向前推进，避免恢复页面或重复请求把已结束房间重新打开。
     */
    private boolean isValidTransition(Integer currentStatus, Integer nextStatus) {
        return Objects.equals(currentStatus, 1) && Objects.equals(nextStatus, 2)
                || Objects.equals(currentStatus, 2) && Objects.equals(nextStatus, 3);
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

    @Override
    public boolean verifyRoomPassword(Long roomId, String password) {
        GameRoom room = getById(roomId);
        if (room == null) {
            return false;
        }
        if (room.getPassword() == null || room.getPassword().isEmpty()) {
            return true;
        }
        return password != null && !password.trim().isEmpty()
                && room.getPassword().equals(hashRoomPassword(password.trim()));
    }

    private String hashRoomPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
