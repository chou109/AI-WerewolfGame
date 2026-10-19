package com.werewolf.game.llm;

import java.util.List;

/** 临时测试请求：密钥只用于本次调用，不由该接口保存。 */
public class AiModelTransientRequest {
    private String apiKey;
    private String apiBaseUrl;
    private String modelType;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private List<AiModelChatRequest.Message> messages;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public List<AiModelChatRequest.Message> getMessages() { return messages; }
    public void setMessages(List<AiModelChatRequest.Message> messages) { this.messages = messages; }
}
