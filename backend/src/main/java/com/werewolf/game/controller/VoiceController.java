package com.werewolf.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.werewolf.game.service.VoiceCacheService;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

/**
 * Cloud speech proxy. API keys are accepted through a request header for one-off
 * browser configuration, or can be supplied by server environment variables.
 */
@RestController
@RequestMapping("/voice")
public class VoiceController {

    @Value("${voice.cloud.openai.api-key:}")
    private String openAiApiKey;

    @Value("${voice.cloud.openai.base-url:https://api.openai.com/v1/audio/speech}")
    private String openAiBaseUrl;

    @Value("${voice.cloud.azure.api-key:}")
    private String azureApiKey;

    @Value("${voice.cloud.azure.base-url:}")
    private String azureBaseUrl;

    @Value("${voice.cloud.rate-limit-per-minute:120}")
    private int rateLimitPerMinute;

    private static final Set<String> BLOCKED_HOST_SUFFIXES = new HashSet<>(Arrays.asList(
            ".local", ".internal", ".localhost", ".lan", ".home", ".test", ".example"
    ));

    @Autowired(required = false)
    private VoiceCacheService voiceCacheService;

    private final ConcurrentMap<String, long[]> rateBuckets = new ConcurrentHashMap<>();

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> data = new HashMap<>();
        data.put("openaiConfigured", hasText(openAiApiKey));
        data.put("azureConfigured", hasText(azureApiKey) && hasText(azureBaseUrl));
        data.put("proxyPath", "/api/voice/synthesize");
        data.put("cacheEnabled", voiceCacheService != null);
        return response(200, data);
    }

    @PostMapping(value = "/synthesize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> synthesize(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Voice-Api-Key", required = false) String requestApiKey,
            HttpServletRequest httpRequest) {
        checkRateLimit(httpRequest);
        String text = stringValue(request.get("text"));
        if (!hasText(text)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "语音文本不能为空");
        }
        if (text.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单次语音文本不能超过4000字");
        }

        String provider = stringValue(request.get("provider"), "openai").toLowerCase();
        if (!"openai".equals(provider) && !"azure".equals(provider) && !"custom".equals(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的云端语音服务");
        }
        int timeout = clamp(intValue(request.get("timeout"), 30000), 3000, 120000);
        String apiKey = hasText(requestApiKey) ? requestApiKey.trim() : stringValue(request.get("apiKey"));
        String endpoint;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity;

        try {
            if ("azure".equals(provider)) {
                endpoint = firstText(stringValue(request.get("apiBaseUrl")), azureBaseUrl);
                apiKey = firstText(apiKey, azureApiKey);
                if (!hasText(endpoint) || !hasText(apiKey)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Azure 需要填写 Speech Endpoint 和 API Key");
                }
                if (endpoint.contains("tts.speech.microsoft.com")) {
                    headers.setContentType(MediaType.parseMediaType("application/ssml+xml"));
                    headers.set("Ocp-Apim-Subscription-Key", apiKey);
                    headers.set("X-Microsoft-OutputFormat", azureOutputFormat(stringValue(request.get("responseFormat"), "mp3")));
                    entity = new HttpEntity<>(azureSsml(text,
                            stringValue(request.get("voice"), "zh-CN-XiaoxiaoNeural"),
                            stringValue(request.get("language"), "zh-CN"),
                            numberValue(request.get("speed"), 1.0)), headers);
                } else {
                    endpoint = ensureSpeechPath(endpoint);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
                    headers.set("api-key", apiKey);
                    entity = new HttpEntity<>(openAiBody(request, text), headers);
                }
            } else {
                endpoint = firstText(stringValue(request.get("apiBaseUrl")), "openai".equals(provider) ? openAiBaseUrl : "");
                if (!hasText(endpoint)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "自定义云端引擎需要填写 API 地址");
                }
                endpoint = ensureSpeechPath(endpoint);
                apiKey = firstText(apiKey, "openai".equals(provider) ? openAiApiKey : "");
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
                if (hasText(apiKey)) headers.setBearerAuth(apiKey);
                entity = new HttpEntity<>(openAiBody(request, text), headers);
            }

            validateEndpoint(endpoint);

            String cacheKey = cacheKey(provider, endpoint, request, text);
            if (voiceCacheService != null) {
                byte[] cached = voiceCacheService.findAudio(cacheKey);
                if (cached != null && cached.length > 0) {
                    HttpHeaders cacheHeaders = new HttpHeaders();
                    cacheHeaders.setContentType(audioType(stringValue(request.get("responseFormat"), "mp3")));
                    cacheHeaders.setCacheControl("no-store");
                    return new ResponseEntity<>(cached, cacheHeaders, HttpStatus.OK);
                }
            }

            RestTemplate restTemplate = restTemplate(timeout);
            ResponseEntity<byte[]> upstream = restTemplate.exchange(endpoint, HttpMethod.POST, entity, byte[].class);
            if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null || upstream.getBody().length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "云端语音没有返回音频");
            }
            HttpHeaders responseHeaders = new HttpHeaders();
            MediaType contentType = upstream.getHeaders().getContentType();
            responseHeaders.setContentType(contentType == null ? audioType(stringValue(request.get("responseFormat"), "mp3")) : contentType);
            responseHeaders.setCacheControl("no-store");
            if (voiceCacheService != null) {
                voiceCacheService.saveAudio(cacheKey, provider, stringValue(request.get("voice"), "alloy"),
                        stringValue(request.get("responseFormat"), "mp3"), upstream.getBody());
            }
            return new ResponseEntity<>(upstream.getBody(), responseHeaders, HttpStatus.OK);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "云端语音请求失败：" + exception.getMessage(), exception);
        }
    }

    private String cacheKey(String provider, String endpoint, Map<String, Object> request, String text) {
        StringBuilder raw = new StringBuilder();
        raw.append(provider).append('|')
                .append(endpoint).append('|')
                .append(stringValue(request.get("model"), "gpt-4o-mini-tts")).append('|')
                .append(stringValue(request.get("voice"), "alloy")).append('|')
                .append(stringValue(request.get("language"), "zh-CN")).append('|')
                .append(stringValue(request.get("responseFormat"), "mp3")).append('|')
                .append(String.format(java.util.Locale.US, "%.4f", numberValue(request.get("speed"), 1.0))).append('|')
                .append(text.trim());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            return String.valueOf(raw.toString().hashCode());
        }
    }

    private Map<String, Object> openAiBody(Map<String, Object> request, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", stringValue(request.get("model"), "gpt-4o-mini-tts"));
        body.put("voice", stringValue(request.get("voice"), "alloy"));
        body.put("input", text);
        body.put("response_format", stringValue(request.get("responseFormat"), "mp3"));
        body.put("speed", clampDouble(numberValue(request.get("speed"), 1.0), 0.25, 4.0));
        return body;
    }

    private String ensureSpeechPath(String baseUrl) {
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        int queryIndex = normalized.indexOf('?');
        String path = queryIndex >= 0 ? normalized.substring(0, queryIndex).replaceAll("/+$", "") : normalized;
        String query = queryIndex >= 0 ? normalized.substring(queryIndex) : "";
        if (path.endsWith("/audio/speech")) return path + query;
        return path + "/audio/speech" + query;
    }

    private void validateEndpoint(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "云端语音 API 地址必须是 HTTP 或 HTTPS 地址");
            }
            String host = uri.getHost();
            if (!hasText(host)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "云端语音 API 地址缺少主机名");
            }
            if (isBlockedHost(host)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "云端语音 API 地址不允许指向本机、内网或保留地址");
            }
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "云端语音 API 地址格式不正确", exception);
        }
    }

    private boolean isBlockedHost(String host) {
        String normalized = host.replace("[", "").replace("]", "").toLowerCase();
        if ("localhost".equals(normalized)) {
            return true;
        }
        for (String suffix : BLOCKED_HOST_SUFFIXES) {
            if (normalized.endsWith(suffix)) {
                return true;
            }
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalized);
            for (InetAddress address : addresses) {
                if (isReservedAddress(address)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // 解析失败留给连接阶段报错
        }
        return false;
    }

    private boolean isReservedAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes != null && bytes.length == 16) {
            // fc00::/7 唯一本地地址
            if ((bytes[0] & 0xFE) == 0xFC) {
                return true;
            }
            // fe80::/10 链路本地（双保险）
            if ((bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xC0) == 0x80) {
                return true;
            }
        }
        return false;
    }

    private void checkRateLimit(HttpServletRequest httpRequest) {
        if (rateLimitPerMinute <= 0 || httpRequest == null) {
            return;
        }
        String clientKey = httpRequest.getRemoteAddr();
        long now = System.currentTimeMillis();
        long[] bucket = rateBuckets.get(clientKey);
        if (bucket == null) {
            bucket = new long[]{now, 0};
            long[] existing = rateBuckets.putIfAbsent(clientKey, bucket);
            if (existing != null) {
                bucket = existing;
            }
        }
        synchronized (bucket) {
            if (now - bucket[0] >= 60_000L) {
                bucket[0] = now;
                bucket[1] = 0;
            }
            bucket[1]++;
            if (bucket[1] > rateLimitPerMinute) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "语音请求过于频繁，请稍后再试");
            }
        }
        if (rateBuckets.size() > 10000) {
            rateBuckets.clear();
        }
    }

    private String azureSsml(String text, String voice, String language, double speed) {
        double percent = Math.round((clampDouble(speed, 0.5, 2.0) - 1.0) * 100.0);
        String rate = (percent >= 0 ? "+" : "") + String.format(java.util.Locale.US, "%.0f%%", percent);
        return "<speak version=\"1.0\" xml:lang=\"" + escapeXml(language) + "\"><voice name=\""
                + escapeXml(voice) + "\"><prosody rate=\"" + rate + "\">" + escapeXml(text)
                + "</prosody></voice></speak>";
    }

    private String azureOutputFormat(String responseFormat) {
        if ("wav".equalsIgnoreCase(responseFormat)) return "riff-24khz-16bit-mono-pcm";
        if ("ogg".equalsIgnoreCase(responseFormat)) return "ogg-24khz-16bit-mono-opus";
        return "audio-24khz-160kbitrate-mono-mp3";
    }

    private MediaType audioType(String responseFormat) {
        if ("wav".equalsIgnoreCase(responseFormat)) return MediaType.parseMediaType("audio/wav");
        if ("ogg".equalsIgnoreCase(responseFormat) || "opus".equalsIgnoreCase(responseFormat)) return MediaType.parseMediaType("audio/ogg");
        return MediaType.parseMediaType("audio/mpeg");
    }

    private RestTemplate restTemplate(int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    private Map<String, Object> response(int code, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("data", data);
        return result;
    }

    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String stringValue(Object value, String fallback) { String text = stringValue(value); return hasText(text) ? text : fallback; }
    private String firstText(String value, String fallback) { return hasText(value) ? value : fallback; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private int intValue(Object value, int fallback) { try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return fallback; } }
    private double numberValue(Object value, double fallback) { try { return value == null ? fallback : Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException e) { return fallback; } }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private double clampDouble(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private String escapeXml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
}
