package com.kevinguanchedarias.owgejava.filter;

import com.kevinguanchedarias.owgejava.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate-limits the heavy authenticated <code>game/websocket-sync</code> endpoint per client IP.
 * <p>
 * That endpoint resolves and serializes a large slice of the user state, so a misbehaving or
 * reconnect-looping frontend calling it many times per second can overwhelm both the app and the
 * database. We cap it to {@code OWGE_WS_SYNC_RATELIMIT_PER_MINUTE} (default 10) requests per IP per
 * fixed one-minute window, replying {@code 429 Too Many Requests} beyond that. The open/* sync
 * variants are not affected: they are cached and cheap.
 *
 * @author Kevin Guanche Darias
 */
@Component
@Order(GlobalConstants.WEBSOCKET_SYNC_RATE_LIMIT_FILTER)
@Slf4j
public class WebsocketSyncRateLimitFilter extends OncePerRequestFilter {
    private static final String SYNC_PATH_SUFFIX = "game/websocket-sync";
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_IPS = 50_000;

    @Value("${OWGE_WS_SYNC_RATELIMIT_PER_MINUTE:10}")
    private int maxPerMinute;

    @Value("${OWGE_PROXY_TRUSTED_HEADER:X-OWGE-RMT-IP}")
    private String proxyTrustedHeader;

    private final Map<String, Window> windowsByIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var ip = resolveIp(request);
        if (isRateLimited(ip)) {
            log.warn("Rate-limiting websocket-sync for ip {} (over {} req/min)", ip, maxPerMinute);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Too many websocket-sync requests, slow down");
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return maxPerMinute <= 0 || !request.getRequestURI().endsWith(SYNC_PATH_SUFFIX);
    }

    private boolean isRateLimited(String ip) {
        var now = System.currentTimeMillis();
        if (windowsByIp.size() > MAX_TRACKED_IPS) {
            windowsByIp.values().removeIf(window -> now - window.startMs > WINDOW_MS);
        }
        var window = windowsByIp.compute(ip, (key, existing) ->
                (existing == null || now - existing.startMs >= WINDOW_MS) ? new Window(now) : existing
        );
        return window.count.incrementAndGet() > maxPerMinute;
    }

    private String resolveIp(HttpServletRequest request) {
        var headerIp = StringUtils.isEmpty(proxyTrustedHeader) ? null : request.getHeader(proxyTrustedHeader);
        return StringUtils.isNotBlank(headerIp) ? headerIp : request.getRemoteAddr();
    }

    private static final class Window {
        private final long startMs;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long startMs) {
            this.startMs = startMs;
        }
    }
}
