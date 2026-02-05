package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.AiPlayer;
import com.werewolf.game.mapper.AiPlayerMapper;
import com.werewolf.game.service.AiPlayerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI玩家服务实现类
 */
@Service
public class AiPlayerServiceImpl extends ServiceImpl<AiPlayerMapper, AiPlayer> implements AiPlayerService {

    @Override
    public List<AiPlayer> getAvailableAiPlayers() {
        return lambdaQuery().eq(AiPlayer::getStatus, 1).list();
    }

    @Override
    public AiPlayer getAiPlayerById(Long id) {
        return getById(id);
    }

    @Override
    public AiPlayer createAiPlayer(AiPlayer aiPlayer) {
        aiPlayer.setCreateTime(LocalDateTime.now());
        aiPlayer.setUpdateTime(LocalDateTime.now());
        aiPlayer.setStatus(1);
        save(aiPlayer);
        return aiPlayer;
    }

    @Override
    public boolean updateAiPlayer(AiPlayer aiPlayer) {
        aiPlayer.setUpdateTime(LocalDateTime.now());
        return updateById(aiPlayer);
    }

    @Override
    public boolean deleteAiPlayer(Long id) {
        return removeById(id);
    }
}
