package com.werewolf.game.controller;

import com.werewolf.game.entity.GameRecord;
import com.werewolf.game.service.GameRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 游戏记录控制器
 */
@RestController
@RequestMapping("/game/record")
public class GameRecordController {

    @Autowired
    private GameRecordService gameRecordService;

    /**
     * 获取房间游戏记录
     */
    @GetMapping("/list/{roomId}")
    public Map<String, Object> getRecordsByRoomId(@PathVariable Long roomId) {
        List<GameRecord> records = gameRecordService.getRecordsByRoomId(roomId);
        return Map.of("code", 200, "data", records);
    }

    /**
     * 获取房间某天的游戏记录
     */
    @GetMapping("/day/{roomId}/{day}")
    public Map<String, Object> getRecordsByDay(@PathVariable Long roomId, @PathVariable Integer day) {
        List<GameRecord> records = gameRecordService.getRecordsByRoomIdAndDay(roomId, day);
        return Map.of("code", 200, "data", records);
    }

    /**
     * 添加游戏记录
     */
    @PostMapping("/add")
    public Map<String, Object> addRecord(@RequestBody GameRecord record) {
        boolean result = gameRecordService.addRecord(record);
        if (result) {
            return Map.of("code", 200, "message", "记录添加成功");
        } else {
            return Map.of("code", 400, "message", "添加失败");
        }
    }

    @GetMapping("/finished")
    public Map<String, Object> getFinishedGames() {
        return Map.of("code", 200, "data", gameRecordService.getFinishedGames());
    }

    @PostMapping("/finish")
    public Map<String, Object> finishGame(@RequestBody Map<String, Object> params) {
        GameRecord record = new GameRecord();
        record.setRoomId(Long.parseLong(params.get("roomId").toString()));
        record.setDayNumber(Integer.parseInt(params.getOrDefault("dayNumber", 1).toString()));
        record.setPhase(params.getOrDefault("phase", "finished").toString());
        record.setActionType("game_end");
        record.setActionContent(params.getOrDefault("actionContent", "").toString());
        String winner = params.getOrDefault("winner", "未知").toString();
        boolean result = gameRecordService.finishGame(record, winner);
        return result
                ? Map.of("code", 200, "message", "游戏结算已记录")
                : Map.of("code", 400, "message", "游戏结算记录失败");
    }

    /**
     * 获取房间最新记录
     */
    @GetMapping("/latest/{roomId}")
    public Map<String, Object> getLatestRecord(@PathVariable Long roomId) {
        GameRecord record = gameRecordService.getLatestRecordByRoomId(roomId);
        if (record != null) {
            return Map.of("code", 200, "data", record);
        } else {
            return Map.of("code", 400, "message", "暂无记录");
        }
    }
}
