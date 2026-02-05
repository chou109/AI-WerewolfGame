package com.werewolf.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 游戏玩家实体类
 */
@Data
@TableName("game_player")
public class GamePlayer implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private Long userId;

    private Integer playerNumber;

    private String role;

    private String personality;

    private String strategy;

    private Integer status;

    private Integer isSheriff;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
