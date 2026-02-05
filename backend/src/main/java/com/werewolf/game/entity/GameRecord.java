package com.werewolf.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 游戏记录实体类
 */
@Data
@TableName("game_record")
public class GameRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private Integer dayNumber;

    private String phase;

    private String actionType;

    private String actionContent;

    private Long playerId;

    private String targetPlayer;

    private LocalDateTime createTime;
}
