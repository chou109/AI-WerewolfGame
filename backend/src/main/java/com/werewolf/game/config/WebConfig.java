package com.werewolf.game.config;

import com.werewolf.game.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web interceptor registration: protect business endpoints, allow login/register and docs.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login", "/user/register", "/error",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        "/swagger-resources/**", "/favicon.ico");
    }
}