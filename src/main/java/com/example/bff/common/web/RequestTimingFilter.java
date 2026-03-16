package com.example.bff.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    public static final String ATTR_START_TIME = "requestStartTimeNanos";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.nanoTime();
        request.setAttribute(ATTR_START_TIME, start);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // nothing else; timing is consumed by ResponseBodyAdvice
        }
    }
}

