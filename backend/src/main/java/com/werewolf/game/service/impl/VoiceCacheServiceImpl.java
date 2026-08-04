package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.werewolf.game.entity.VoiceCache;
import com.werewolf.game.mapper.VoiceCacheMapper;
import com.werewolf.game.service.VoiceCacheService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 云端语音缓存服务实现
 */
@Service
public class VoiceCacheServiceImpl implements VoiceCacheService {

    @Resource
    private VoiceCacheMapper voiceCacheMapper;

    @Override
    public byte[] findAudio(String cacheKey) {
        VoiceCache cache = voiceCacheMapper.selectOne(
                new LambdaQueryWrapper<VoiceCache>().eq(VoiceCache::getCacheKey, cacheKey));
        return cache == null ? null : cache.getAudio();
    }

    @Override
    public void saveAudio(String cacheKey, String provider, String voice, String responseFormat, byte[] audio) {
        VoiceCache cache = new VoiceCache();
        cache.setCacheKey(cacheKey);
        cache.setProvider(provider);
        cache.setVoice(voice);
        cache.setResponseFormat(responseFormat);
        cache.setAudio(audio);
        cache.setCreatedAt(System.currentTimeMillis());
        VoiceCache existing = voiceCacheMapper.selectById(cacheKey);
        if (existing == null) {
            try {
                voiceCacheMapper.insert(cache);
            } catch (Exception e) {
                // 并发插入冲突时回退为更新
                voiceCacheMapper.updateById(cache);
            }
        } else {
            voiceCacheMapper.updateById(cache);
        }
    }
}
