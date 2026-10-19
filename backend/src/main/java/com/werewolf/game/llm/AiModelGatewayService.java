package com.werewolf.game.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolf.game.entity.AiPlayer;
import com.werewolf.game.mapper.AiPlayerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;

@Service
public class AiModelGatewayService {
    private static final int MAX_MESSAGES = 12;
    private static final int MAX_MESSAGE_CHARS = 12000;
    private static final int MAX_TOTAL_CHARS = 36000;
    private static final Set<String> ALLOWED_HOSTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "api.openai.com", "api.deepseek.com", "api.anthropic.com", "api-inference.modelscope.cn"
    )));
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final AiPlayerMapper aiPlayerMapper;
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<Long, RateWindow> rateWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> inFlightByUser = new ConcurrentHashMap<>();

    @Autowired
    public AiModelGatewayService(JdbcTemplate jdbcTemplate, AiPlayerMapper aiPlayerMapper) {
        this(jdbcTemplate, aiPlayerMapper, createRestTemplate());
    }

    AiModelGatewayService(JdbcTemplate jdbcTemplate, AiPlayerMapper aiPlayerMapper, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiPlayerMapper = aiPlayerMapper;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(35).toMillis());
        return new RestTemplate(factory);
    }

    public String complete(Long userId, AiModelChatRequest request) {
        validateRequest(request);
        if (!isRoomCreator(request.getRoomId(), userId) || !isAiSeat(request.getRoomId(), request.getGamePlayerId(), request.getAiPlayerId())) {
            throw new SecurityException("无权为该房间中的 AI 玩家发起模型调用");
        }
        AtomicInteger inFlight = beginRequest(userId);
        try {
            AiPlayer aiPlayer = aiPlayerMapper.selectById(request.getAiPlayerId());
            if (aiPlayer == null || aiPlayer.getStatus() == null || aiPlayer.getStatus() != 1) {
                throw new IllegalArgumentException("AI 玩家不存在或已停用");
            }
            if (blank(aiPlayer.getApiKey())) {
                throw new IllegalStateException("该 AI 玩家尚未在服务端配置模型密钥");
            }
            return invoke(aiPlayer, request.getMessages());
        } finally {
            inFlight.decrementAndGet();
        }
    }

    public com.fasterxml.jackson.databind.JsonNode listModels(Long userId, AiModelTransientRequest request) {
        validateTransientConfig(request, false);
        AtomicInteger inFlight = beginRequest(userId);
        try {
            boolean anthropic = isAnthropic(request.getModelType());
            validateProviderUrl(request.getApiBaseUrl(), anthropic);
            HttpHeaders headers = transientHeaders(request.getApiKey(), anthropic);
            ResponseEntity<String> response = restTemplate.exchange(modelsEndpoint(request.getApiBaseUrl()), HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
            JsonNode root = MAPPER.readTree(response.getBody());
            JsonNode models = root == null ? null : root.get("data");
            if (models == null || !models.isArray()) throw new IllegalStateException("模型服务返回格式无效");
            return models;
        } catch (HttpStatusCodeException exception) {
            throw new IllegalStateException("模型服务连接失败（HTTP " + exception.getRawStatusCode() + ")");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("模型服务暂时不可用");
        } finally {
            inFlight.decrementAndGet();
        }
    }

    public String completeTransient(Long userId, AiModelTransientRequest request) {
        validateTransientConfig(request, true);
        AtomicInteger inFlight = beginRequest(userId);
        try {
            AiPlayer temporary = new AiPlayer();
            temporary.setApiKey(request.getApiKey());
            temporary.setApiBaseUrl(request.getApiBaseUrl());
            temporary.setModelType(request.getModelType());
            temporary.setModelName(request.getModelName());
            temporary.setTemperature(request.getTemperature());
            temporary.setMaxTokens(request.getMaxTokens());
            return invoke(temporary, request.getMessages());
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private AtomicInteger beginRequest(Long userId) {
        enforceQuota(userId);
        AtomicInteger inFlight = inFlightByUser.computeIfAbsent(userId, ignored -> new AtomicInteger());
        if (inFlight.incrementAndGet() > 4) {
            inFlight.decrementAndGet();
            throw new RateLimitException("模型并发请求过多，请稍后重试");
        }
        return inFlight;
    }

    private HttpHeaders transientHeaders(String apiKey, boolean anthropic) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (anthropic) {
            headers.set("x-api-key", apiKey.trim());
            headers.set("anthropic-version", "2023-06-01");
        } else {
            headers.setBearerAuth(apiKey.trim());
        }
        return headers;
    }

    private String modelsEndpoint(String base) {
        String value = base.trim().replaceAll("/+$", "");
        if (value.endsWith("/models")) return value;
        if (value.endsWith("/v1")) return value + "/models";
        return value + "/v1/models";
    }

    private void validateTransientConfig(AiModelTransientRequest request, boolean requireChat) {
        if (request == null || blank(request.getApiKey()) || request.getApiKey().length() > 512 || blank(request.getApiBaseUrl()) || request.getApiBaseUrl().length() > 255 || blank(request.getModelType()) || request.getModelType().length() > 32) {
            throw new IllegalArgumentException("模型测试配置不完整或密钥长度无效");
        }
        if (requireChat) {
            if (blank(request.getModelName()) || request.getModelName().length() > 255) throw new IllegalArgumentException("模型名称无效");
            AiModelChatRequest chat = new AiModelChatRequest();
            chat.setRoomId(1L);
            chat.setGamePlayerId(1L);
            chat.setAiPlayerId(1L);
            chat.setMessages(request.getMessages());
            validateRequest(chat);
        }
    }

    static void validateProviderUrl(String rawUrl, boolean anthropic) {
        if (blank(rawUrl)) throw new IllegalArgumentException("模型服务地址未配置");
        try {
            URI uri = URI.create(rawUrl.trim());
            String host = uri.getHost();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !ALLOWED_HOSTS.contains(normalizedHost) || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw new IllegalArgumentException("该模型服务地址未加入服务端允许列表");
            }
            if (anthropic != "api.anthropic.com".equals(normalizedHost)) {
                throw new IllegalArgumentException("模型类型与服务地址不匹配");
            }
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            boolean validPath = path.isEmpty() || "/".equals(path) || "/v1".equals(path) || "/models".equals(path) || "/v1/models".equals(path);
            validPath |= anthropic
                    ? ("/messages".equals(path) || "/v1/messages".equals(path))
                    : ("/chat/completions".equals(path) || "/v1/chat/completions".equals(path));
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null || !validPath) {
                throw new IllegalArgumentException("模型服务地址格式不受支持");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("模型服务地址无效");
        }
    }

    private void enforceQuota(Long userId) {
        if (userId == null) throw new SecurityException("登录状态无效");
        long now = System.currentTimeMillis();
        RateWindow window = rateWindows.computeIfAbsent(userId, ignored -> new RateWindow(now));
        if (!window.allow(now)) throw new RateLimitException("模型调用频率过高，请稍后重试");
        if (rateWindows.size() > 5000) rateWindows.entrySet().removeIf(entry -> now - entry.getValue().startedAt > 120000);
    }

    private static class RateWindow {
        private long startedAt;
        private int count;
        private RateWindow(long now) { this.startedAt = now; }
        private synchronized boolean allow(long now) {
            if (now - startedAt >= 60000 || now < startedAt) { startedAt = now; count = 0; }
            return ++count <= 30;
        }
    }

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) { super(message); }
    }

    private String invoke(AiPlayer player, List<AiModelChatRequest.Message> messages) {
        boolean anthropic = isAnthropic(player.getModelType());
        validateProviderUrl(player.getApiBaseUrl(), anthropic);
        String url = endpoint(player.getApiBaseUrl(), anthropic);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (anthropic) {
            headers.set("x-api-key", player.getApiKey().trim());
            headers.set("anthropic-version", "2023-06-01");
        } else {
            headers.setBearerAuth(player.getApiKey().trim());
        }

        Map<String, Object> payload = anthropic ? anthropicPayload(player, messages) : openAiPayload(player, messages);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            JsonNode root = MAPPER.readTree(response.getBody());
            String content = extractContent(root, anthropic);
            if (blank(content)) throw new IllegalStateException("模型返回了空内容");
            return content;
        } catch (HttpStatusCodeException exception) {
            // 不把 Provider 原始响应带回客户端：上游错误可能包含敏感请求信息。
            throw new IllegalStateException("模型服务调用失败（HTTP " + exception.getRawStatusCode() + ")");
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("模型服务暂时不可用");
        }
    }

    private Map<String, Object> openAiPayload(AiPlayer player, List<AiModelChatRequest.Message> messages) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("model", player.getModelName());
        payload.put("messages", messages);
        payload.put("temperature", normalizeTemperature(player.getTemperature()));
        payload.put("max_tokens", boundedMaxTokens(player.getMaxTokens()));
        payload.put("response_format", Collections.singletonMap("type", "json_object"));
        return payload;
    }

    private Map<String, Object> anthropicPayload(AiPlayer player, List<AiModelChatRequest.Message> messages) {
        StringBuilder system = new StringBuilder();
        java.util.ArrayList<Map<String, String>> userMessages = new java.util.ArrayList<>();
        for (AiModelChatRequest.Message message : messages) {
            if ("system".equals(message.getRole())) {
                if (system.length() > 0) system.append('\n');
                system.append(message.getContent());
            } else {
                Map<String, String> item = new java.util.LinkedHashMap<>();
                item.put("role", message.getRole());
                item.put("content", message.getContent());
                userMessages.add(item);
            }
        }
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("model", player.getModelName());
        payload.put("max_tokens", boundedMaxTokens(player.getMaxTokens()));
        payload.put("temperature", normalizeTemperature(player.getTemperature()));
        payload.put("system", system.toString());
        payload.put("messages", userMessages);
        return payload;
    }

    private String extractContent(JsonNode root, boolean anthropic) {
        if (root == null) return null;
        if (anthropic) {
            JsonNode content = root.path("content");
            if (content.isArray()) {
                StringBuilder result = new StringBuilder();
                for (JsonNode item : content) if ("text".equals(item.path("type").asText())) result.append(item.path("text").asText(""));
                return result.toString();
            }
            return content.asText(null);
        }
        return root.path("choices").path(0).path("message").path("content").asText(null);
    }

    private String endpoint(String base, boolean anthropic) {
        String value = base.trim().replaceAll("/+$", "");
        if (anthropic) return value.endsWith("/messages") ? value : (value.endsWith("/v1") ? value + "/messages" : value + "/v1/messages");
        if (value.endsWith("/chat/completions")) return value;
        if (value.endsWith("/v1")) return value + "/chat/completions";
        return value + "/v1/chat/completions";
    }

    private boolean isRoomCreator(Long roomId, Long userId) {
        if (roomId == null || userId == null) return false;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM game_room WHERE id = ? AND creator_id = ?", Integer.class, roomId, userId);
        return count != null && count > 0;
    }

    private boolean isAiSeat(Long roomId, Long gamePlayerId, Long aiPlayerId) {
        if (roomId == null || gamePlayerId == null || aiPlayerId == null) return false;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM game_player WHERE id = ? AND room_id = ? AND ai_player_id = ? AND status = 1", Integer.class, gamePlayerId, roomId, aiPlayerId);
        return count != null && count > 0;
    }

    void validateRequest(AiModelChatRequest request) {
        if (request == null || request.getRoomId() == null || request.getGamePlayerId() == null || request.getAiPlayerId() == null) throw new IllegalArgumentException("模型调用参数不完整");
        List<AiModelChatRequest.Message> messages = request.getMessages();
        if (messages == null || messages.isEmpty() || messages.size() > MAX_MESSAGES) throw new IllegalArgumentException("消息数量超出限制");
        int total = 0;
        for (AiModelChatRequest.Message message : messages) {
            if (message == null || message.getRole() == null || !Arrays.asList("system", "user", "assistant").contains(message.getRole()) || message.getContent() == null || message.getContent().length() > MAX_MESSAGE_CHARS) throw new IllegalArgumentException("模型消息格式无效或长度超限");
            total += message.getContent().length();
        }
        if (total > MAX_TOTAL_CHARS) throw new IllegalArgumentException("模型上下文总长度超出限制");
    }

    private double normalizeTemperature(Double value) {
        if (value == null) return 0.7;
        double normalized = value > 2 ? value / 10.0 : value;
        return Math.max(0, Math.min(2, normalized));
    }

    private int boundedMaxTokens(Integer value) { return Math.max(1, Math.min(2200, value == null ? 1000 : value)); }
    private boolean isAnthropic(String type) { return type != null && ("anthropic".equalsIgnoreCase(type) || "claude".equalsIgnoreCase(type)); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
