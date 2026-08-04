package com.werewolf.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 游戏房间实体类
 */
@Data
@TableName("game_room")
public class GameRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomCode;

    /** 房间密码（SHA-256 哈希），仅服务端使用，绝不返回前端 */
    @JsonIgnore
    private String password;

    private String roomName;

    private Integer playerCount;

    private String gameBoard;

    /** 创建房间时锁定的板子规则版本（对应 board_configs.json 的 schemaVersion） */
    private String boardVersion;

    private Integer status;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String winner;

    @JsonProperty("hasPassword")
    public boolean isHasPassword() {
        return password != null && !password.isEmpty();
    }
}
