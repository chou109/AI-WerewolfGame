package com.werewolf.game.llm;

import java.util.List;

public class AiModelChatRequest {
    private Long roomId;
    private Long gamePlayerId;
    private Long aiPlayerId;
    private List<Message> messages;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getGamePlayerId() { return gamePlayerId; }
    public void setGamePlayerId(Long gamePlayerId) { this.gamePlayerId = gamePlayerId; }
    public Long getAiPlayerId() { return aiPlayerId; }
    public void setAiPlayerId(Long aiPlayerId) { this.aiPlayerId = aiPlayerId; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public static class Message {
        private String role;
        private String content;
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
