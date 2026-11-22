package ru.spb.transit.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Пишет в STDOUT по одной строке на каждый HTTP-запрос.
 * Нужен для проверки нефункциональных требований в рантайме:
 * время ответа, код ответа, пользователь и идентификатор запроса.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);

        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String user = authentication != null && authentication.isAuthenticated()
                    ? authentication.getName()
                    : "anonymous";

            if (response.getStatus() >= 500) {
                log.error("http method={} path={} status={} durationMs={} user={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), durationMs, user);
            } else if (response.getStatus() >= 400) {
                log.warn("http method={} path={} status={} durationMs={} user={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), durationMs, user);
            } else {
                log.info("http method={} path={} status={} durationMs={} user={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), durationMs, user);
            }
            MDC.remove(REQUEST_ID);
        }
    }
}
