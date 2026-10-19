package com.werewolf.game.llm;

import java.util.Map;

/** AI 角色决策请求：客户端只能提交动作名称和受限的公开上下文。 */
public class AiDecisionRequest {
    private Long roomId;
    private Long gamePlayerId;
    private Long aiPlayerId;
    private String action;
    private Map<String, String> publicContext;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getGamePlayerId() { return gamePlayerId; }
    public void setGamePlayerId(Long gamePlayerId) { this.gamePlayerId = gamePlayerId; }
    public Long getAiPlayerId() { return aiPlayerId; }
    public void setAiPlayerId(Long aiPlayerId) { this.aiPlayerId = aiPlayerId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Map<String, String> getPublicContext() { return publicContext; }
    public void setPublicContext(Map<String, String> publicContext) { this.publicContext = publicContext; }
}
