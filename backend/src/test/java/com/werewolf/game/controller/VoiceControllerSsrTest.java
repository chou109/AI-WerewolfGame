package com.werewolf.game.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class VoiceControllerSsrTest {

    private static Method validateEndpointMethod() throws Exception {
        Method method = VoiceController.class.getDeclaredMethod("validateEndpoint", String.class);
        method.setAccessible(true);
        return method;
    }

    private void assertBlocked(String endpoint) throws Exception {
        Method method = validateEndpointMethod();
        try {
            method.invoke(new VoiceController(), endpoint);
            fail("应拒绝内网/本机地址: " + endpoint);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            if (!(exception.getCause() instanceof ResponseStatusException)) {
                throw exception;
            }
        }
    }

    @Test
    void rejectsLoopbackAndPrivateEndpoints() throws Exception {
        assertBlocked("http://localhost:3000");
        assertBlocked("http://127.0.0.1:3000");
        assertBlocked("http://0.0.0.0:3000");
        assertBlocked("http://10.1.2.3/audio/speech");
        assertBlocked("http://172.16.5.5/audio/speech");
        assertBlocked("http://192.168.1.10/audio/speech");
        assertBlocked("http://169.254.10.10/audio/speech");
        assertBlocked("http://[::1]:3000/audio/speech");
    }

    @Test
    void rejectsLocalSuffixHostnames() throws Exception {
        assertBlocked("http://nas.local/audio/speech");
        assertBlocked("http://api.internal/audio/speech");
        assertBlocked("https://router.home/audio/speech");
    }

    @Test
    void rejectsNonHttpSchemes() throws Exception {
        assertBlocked("ftp://example.com/audio");
        assertBlocked("file:///etc/passwd");
    }

    @Test
    void acceptsPublicHttpEndpoint() throws Exception {
        Method method = validateEndpointMethod();
        try {
            method.invoke(new VoiceController(), "https://api.openai.com/v1/audio/speech");
        } catch (java.lang.reflect.InvocationTargetException exception) {
            if (exception.getCause() instanceof ResponseStatusException) {
                fail("公开地址不应被拦截: " + exception.getCause().getMessage());
            }
            throw exception;
        }
    }
}