package com.werewolf.game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.werewolf.game.entity.GameRecord;

import java.util.List;

/**
 * 游戏记录服务接口
 */
public interface GameRecordService extends IService<GameRecord> {
    /**
     * 根据房间ID获取游戏记录
     */
    List<GameRecord> getRecordsByRoomId(Long roomId);

    /**
     * 根据房间ID和天数获取游戏记录
     */
    List<GameRecord> getRecordsByRoomIdAndDay(Long roomId, Integer dayNumber);

    /**
     * 添加游戏记录
     */
    boolean addRecord(GameRecord record);

    /**
     * 获取房间的最新游戏记录
     */
    GameRecord getLatestRecordByRoomId(Long roomId);

    List<GameRecord> getFinishedGames();

    /**
     * 获取已结束对局的轻量摘要（不含完整 actionContent）
     */
    List<GameRecord> getFinishedGameSummaries();

    /**
     * 获取对局记录详情（含完整复盘数据）
     */
    GameRecord getRecordDetail(Long id);

    boolean finishGame(GameRecord record, String winner);

    /**
     * 对局记录统计（玩家场次/胜率/角色偏好、板子胜率）
     */
    java.util.Map<String, Object> getStats();
}
