package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.GameRecord;
import com.werewolf.game.entity.GameRoom;
import com.werewolf.game.mapper.GameRecordMapper;
import com.werewolf.game.service.GameRecordService;
import com.werewolf.game.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 游戏记录服务实现类
 */
@Service
public class GameRecordServiceImpl extends ServiceImpl<GameRecordMapper, GameRecord> implements GameRecordService {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<GameRecord> getRecordsByRoomId(Long roomId) {
        return lambdaQuery()
                .eq(GameRecord::getRoomId, roomId)
                .orderByAsc(GameRecord::getDayNumber)
                .orderByAsc(GameRecord::getCreateTime)
                .list();
    }

    @Override
    public List<GameRecord> getRecordsByRoomIdAndDay(Long roomId, Integer dayNumber) {
        return lambdaQuery()
                .eq(GameRecord::getRoomId, roomId)
                .eq(GameRecord::getDayNumber, dayNumber)
                .orderByAsc(GameRecord::getCreateTime)
                .list();
    }

    @Override
    public boolean addRecord(GameRecord record) {
        record.setCreateTime(LocalDateTime.now());
        return save(record);
    }

    @Override
    public GameRecord getLatestRecordByRoomId(Long roomId) {
        return lambdaQuery()
                .eq(GameRecord::getRoomId, roomId)
                .orderByDesc(GameRecord::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public List<GameRecord> getFinishedGames() {
        return lambdaQuery()
                .eq(GameRecord::getActionType, "game_end")
                .orderByDesc(GameRecord::getCreateTime)
                .list();
    }

    @Override
    @Transactional
    public synchronized boolean finishGame(GameRecord record, String winner) {
        GameRoom room = gameRoomService.getById(record.getRoomId());
        LocalDateTime sessionStart = room == null ? null : room.getStartTime();
        boolean roomEnded = gameRoomService.endGame(record.getRoomId(), winner);
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<GameRecord> existingQuery = lambdaQuery()
                .eq(GameRecord::getRoomId, record.getRoomId())
                .eq(GameRecord::getActionType, "game_end");
        if (sessionStart != null) {
            existingQuery.ge(GameRecord::getCreateTime, sessionStart);
        }
        long existing = existingQuery.count();
        boolean recorded = existing > 0;
        if (!recorded) {
            record.setActionType("game_end");
            record.setPhase("finished");
            record.setTargetPlayer(winner);
            record.setCreateTime(LocalDateTime.now());
            recorded = save(record);
        }
        jdbcTemplate.update("DELETE FROM game_state_snapshot WHERE room_id = ?", record.getRoomId());
        return roomEnded || recorded;
    }
}
