package com.example.bff.common.web;

import com.example.bff.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiResponseTimingAdvice implements ResponseBodyAdvice<ApiResponse<?>> {

    @Override
    public boolean supports(MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public ApiResponse<?> beforeBodyWrite(
            ApiResponse<?> body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response
    ) {

        if (body == null) {
            return null;
        }

        long durationMs = 0L;

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

            Object attr = httpServletRequest.getAttribute(RequestTimingFilter.ATTR_START_TIME);

            if (attr instanceof Long startNanos) {
                long elapsedNanos = System.nanoTime() - startNanos;
                durationMs = elapsedNanos / 1_000_000L;
            }
        }

        return new ApiResponse<>(
                body.timestamp() != null ? body.timestamp() : Instant.now(),
                body.requestId(),
                body.status(),
                body.message(),
                body.data(),
                body.error(),
                durationMs
        );
    }
}