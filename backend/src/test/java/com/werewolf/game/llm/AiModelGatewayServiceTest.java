package com.werewolf.game.llm;

import com.werewolf.game.mapper.AiPlayerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiModelGatewayServiceTest {
    private JdbcTemplate jdbcTemplate;
    private AiPlayerMapper aiPlayerMapper;
    private RestTemplate restTemplate;
    private AiModelGatewayService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        aiPlayerMapper = mock(AiPlayerMapper.class);
        restTemplate = mock(RestTemplate.class);
        service = new AiModelGatewayService(jdbcTemplate, aiPlayerMapper, restTemplate);
    }

    @Test
    void allowsConfiguredProviderHostsOverHttps() {
        assertDoesNotThrow(() -> AiModelGatewayService.validateProviderUrl("https://api.openai.com/v1", false));
        assertDoesNotThrow(() -> AiModelGatewayService.validateProviderUrl("https://api.deepseek.com/v1", false));
        assertDoesNotThrow(() -> AiModelGatewayService.validateProviderUrl("https://api.anthropic.com/v1", true));
        assertDoesNotThrow(() -> AiModelGatewayService.validateProviderUrl("https://api-inference.modelscope.cn/v1", false));
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("https://api.anthropic.com/v1", false));
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("https://api.openai.com/v1/messages", true));
    }

    @Test
    void rejectsHttpPrivateCustomAndMalformedDestinations() {
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("http://api.openai.com/v1", false));
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("https://127.0.0.1/v1", false));
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("https://attacker.example/v1", false));
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("https://api.openai.com:8443/v1", false));
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("https://user@api.openai.com/v1", false));
        assertThrows(IllegalArgumentException.class, () -> AiModelGatewayService.validateProviderUrl("https://api.openai.com/v1/../../internal", false));
    }

    @Test
    void rejectsMalformedRequestsBeforeDatabaseOrProviderAccess() {
        AiModelChatRequest request = new AiModelChatRequest();
        request.setRoomId(1L);
        request.setGamePlayerId(2L);
        request.setAiPlayerId(3L);
        request.setMessages(Collections.singletonList(message("tool", "not allowed")));

        assertThrows(IllegalArgumentException.class, () -> service.complete(5L, request));
        verify(jdbcTemplate, never()).queryForObject(anyString(), any(Class.class), any(), any());
        verify(aiPlayerMapper, never()).selectById(anyLong());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void rejectsNonOwnerBeforeLoadingTheAiCredential() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);
        AiModelChatRequest request = validRequest();

        assertThrows(SecurityException.class, () -> service.complete(5L, request));
        verify(aiPlayerMapper, never()).selectById(anyLong());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void transientProbeRejectsUnapprovedHostBeforeNetworkAccess() {
        AiModelTransientRequest request = new AiModelTransientRequest();
        request.setApiKey("temporary-test-key");
        request.setApiBaseUrl("https://127.0.0.1/v1");
        request.setModelType("openai");

        assertThrows(IllegalArgumentException.class, () -> service.listModels(5L, request));
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void transientChatRejectsMissingKeyAndDoesNotCallProvider() {
        AiModelTransientRequest request = new AiModelTransientRequest();
        request.setApiBaseUrl("https://api.openai.com/v1");
        request.setModelType("openai");
        request.setModelName("gpt-test");
        request.setMessages(Collections.singletonList(message("user", "hello")));

        assertThrows(IllegalArgumentException.class, () -> service.completeTransient(5L, request));
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void acceptsOnlyBoundedMessageLists() {
        AiModelChatRequest request = validRequest();
        assertDoesNotThrow(() -> service.validateRequest(request));
        StringBuilder oversized = new StringBuilder();
        for (int i = 0; i < 12001; i++) oversized.append('a');
        request.setMessages(Arrays.asList(message("user", oversized.toString())));
        assertThrows(IllegalArgumentException.class, () -> service.validateRequest(request));
    }

    private AiModelChatRequest validRequest() {
        AiModelChatRequest request = new AiModelChatRequest();
        request.setRoomId(1L);
        request.setGamePlayerId(2L);
        request.setAiPlayerId(3L);
        request.setMessages(Collections.singletonList(message("user", "hello")));
        return request;
    }

    private AiModelChatRequest.Message message(String role, String content) {
        AiModelChatRequest.Message message = new AiModelChatRequest.Message();
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
