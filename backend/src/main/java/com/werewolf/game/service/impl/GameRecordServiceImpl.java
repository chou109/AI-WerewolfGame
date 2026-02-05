package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.GameRecord;
import com.werewolf.game.mapper.GameRecordMapper;
import com.werewolf.game.service.GameRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 游戏记录服务实现类
 */
@Service
public class GameRecordServiceImpl extends ServiceImpl<GameRecordMapper, GameRecord> implements GameRecordService {

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
                .one();
    }
}
