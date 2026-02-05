package com.werewolf.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI玩家实体类
 */
@Data
@TableName("ai_player")
public class AiPlayer implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String modelType;

    private String modelName;

    private String personality;

    private String strategy;

    private String apiKey;

    private String apiBaseUrl;

    private Integer temperature;

    private Integer maxTokens;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
