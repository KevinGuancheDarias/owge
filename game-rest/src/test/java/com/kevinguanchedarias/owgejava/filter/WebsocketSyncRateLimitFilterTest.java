package com.kevinguanchedarias.owgejava.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class WebsocketSyncRateLimitFilterTest {
    private static final String SYNC_URI = "/game_api/game/websocket-sync";
    private static final String HEADER = "X-OWGE-RMT-IP";

    private WebsocketSyncRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new WebsocketSyncRateLimitFilter();
        ReflectionTestUtils.setField(filter, "maxPerMinute", 2);
        ReflectionTestUtils.setField(filter, "proxyTrustedHeader", HEADER);
    }

    @Test
    void allows_requests_up_to_the_limit_then_blocks() throws Exception {
        var ip = "1.2.3.4";

        var first = runOnce(SYNC_URI, ip);
        var second = runOnce(SYNC_URI, ip);
        var third = runOnce(SYNC_URI, ip);

        assertThat(first.passedThrough).isTrue();
        assertThat(first.status).isEqualTo(HttpStatus.OK.value());
        assertThat(second.passedThrough).isTrue();
        assertThat(third.passedThrough).isFalse();
        assertThat(third.status).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void limit_is_per_ip() throws Exception {
        runOnce(SYNC_URI, "1.1.1.1");
        runOnce(SYNC_URI, "1.1.1.1");
        var blocked = runOnce(SYNC_URI, "1.1.1.1");
        var otherIp = runOnce(SYNC_URI, "2.2.2.2");

        assertThat(blocked.passedThrough).isFalse();
        assertThat(otherIp.passedThrough).isTrue();
    }

    @Test
    void does_not_limit_other_endpoints() throws Exception {
        Result last = null;
        for (var i = 0; i < 5; i++) {
            last = runOnce("/game_api/open/websocket-sync/rule_change", "1.2.3.4");
        }
        assertThat(last.passedThrough).isTrue();
    }

    @Test
    void disabled_when_limit_is_zero() throws Exception {
        ReflectionTestUtils.setField(filter, "maxPerMinute", 0);
        Result last = null;
        for (var i = 0; i < 5; i++) {
            last = runOnce(SYNC_URI, "1.2.3.4");
        }
        assertThat(last.passedThrough).isTrue();
    }

    private Result runOnce(String uri, String ip) throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", uri);
        if (ip != null) {
            request.addHeader(HEADER, ip);
        }
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Result(chain.getRequest() != null, response.getStatus());
    }

    private record Result(boolean passedThrough, int status) {
    }
}
