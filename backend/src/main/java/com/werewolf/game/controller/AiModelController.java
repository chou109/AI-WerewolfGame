package com.werewolf.game.controller;

import com.werewolf.game.llm.AiModelChatRequest;
import com.werewolf.game.llm.AiDecisionRequest;
import com.werewolf.game.llm.AiModelTransientRequest;
import com.werewolf.game.llm.AiModelGatewayService;
import com.werewolf.game.util.MapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/ai/model")
public class AiModelController {
    @Autowired
    private AiModelGatewayService gatewayService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody AiModelChatRequest request, HttpServletRequest httpRequest) {
        Long userId = parseUserId(httpRequest.getAttribute("userId"));
        try {
            String content = gatewayService.complete(userId, request);
            return MapUtil.of("code", 200, "data", MapUtil.of("content", content));
        } catch (SecurityException exception) {
            return MapUtil.of("code", 403, "message", exception.getMessage());
        } catch (AiModelGatewayService.RateLimitException exception) {
            return MapUtil.of("code", 429, "message", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return MapUtil.of("code", 400, "message", exception.getMessage());
        } catch (IllegalStateException exception) {
            return MapUtil.of("code", 502, "message", exception.getMessage());
        }
    }

    @PostMapping("/decision")
    public Map<String, Object> decision(@RequestBody AiDecisionRequest request, HttpServletRequest httpRequest) {
        Long userId = parseUserId(httpRequest.getAttribute("userId"));
        try {
            String content = gatewayService.completeDecision(userId, request);
            return MapUtil.of("code", 200, "data", MapUtil.of("content", content));
        } catch (SecurityException exception) {
            return MapUtil.of("code", 403, "message", exception.getMessage());
        } catch (AiModelGatewayService.RateLimitException exception) {
            return MapUtil.of("code", 429, "message", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return MapUtil.of("code", 400, "message", exception.getMessage());
        } catch (IllegalStateException exception) {
            return MapUtil.of("code", 502, "message", exception.getMessage());
        }
    }


    @PostMapping("/models")
    public Map<String, Object> models(@RequestBody AiModelTransientRequest request, HttpServletRequest httpRequest) {
        Long userId = parseUserId(httpRequest.getAttribute("userId"));
        try {
            return MapUtil.of("code", 200, "data", gatewayService.listModels(userId, request));
        } catch (AiModelGatewayService.RateLimitException exception) {
            return MapUtil.of("code", 429, "message", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return MapUtil.of("code", 400, "message", exception.getMessage());
        } catch (IllegalStateException exception) {
            return MapUtil.of("code", 502, "message", exception.getMessage());
        }
    }

    @PostMapping("/chat-test")
    public Map<String, Object> chatTest(@RequestBody AiModelTransientRequest request, HttpServletRequest httpRequest) {
        Long userId = parseUserId(httpRequest.getAttribute("userId"));
        try {
            String content = gatewayService.completeTransient(userId, request);
            return MapUtil.of("code", 200, "data", MapUtil.of("content", content));
        } catch (AiModelGatewayService.RateLimitException exception) {
            return MapUtil.of("code", 429, "message", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return MapUtil.of("code", 400, "message", exception.getMessage());
        } catch (IllegalStateException exception) {
            return MapUtil.of("code", 502, "message", exception.getMessage());
        }
    }

    private Long parseUserId(Object value) {
        try { return value == null ? null : Long.valueOf(value.toString()); }
        catch (NumberFormatException exception) { return null; }
    }
}
