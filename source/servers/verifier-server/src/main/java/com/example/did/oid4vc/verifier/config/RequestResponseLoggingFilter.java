/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.did.oid4vc.verifier.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isStaticResource(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        // Log request
        logRequest(cachedRequest);

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logResponse(cachedRequest, cachedResponse, duration);
            cachedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(CachedBodyHttpServletRequest request) {
        String headers = Collections.list(request.getHeaderNames()).stream()
                .map(name -> name + "=" + request.getHeader(name))
                .collect(Collectors.joining(", "));

        String body = new String(request.getCachedBody(), StandardCharsets.UTF_8);

        StringBuilder sb = new StringBuilder();
        sb.append("\n>>> ").append(request.getMethod()).append(" ").append(request.getRequestURI());
        if (request.getQueryString() != null) {
            sb.append("?").append(request.getQueryString());
        }
        sb.append("\n  Headers: {").append(headers).append("}");
        if (!body.isEmpty()) {
            sb.append("\n  Body: ").append(body);
        }

        log.debug(sb.toString());
    }

    private void logResponse(CachedBodyHttpServletRequest request, ContentCachingResponseWrapper response, long duration) {
        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        StringBuilder sb = new StringBuilder();
        sb.append("\n<<< ").append(response.getStatus()).append(" ")
                .append(request.getMethod()).append(" ").append(request.getRequestURI())
                .append(" [").append(duration).append("ms]");
        if (!body.isEmpty()) {
            sb.append("\n  Body: ").append(body);
        }

        log.debug(sb.toString());
    }

    private boolean isStaticResource(String uri) {
        return uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
                || uri.startsWith("/webjars/") || uri.startsWith("/favicon.ico")
                || uri.startsWith("/swagger-ui") || uri.startsWith("/api-docs");
    }
}
