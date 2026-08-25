package com.kleos.transfers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void allowsRequestsUnderLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 3);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/v1/clubs"), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        verify(chain, org.mockito.Mockito.times(3)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void blocksWhenLimitExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 2);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request("/api/v1/clubs"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("/api/v1/clubs"), new MockHttpServletResponse(), chain);

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/clubs"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("Rate limit exceeded");
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        verify(chain, org.mockito.Mockito.times(2)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void skipsHealthEndpoint() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/v1/health"), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        verify(chain, org.mockito.Mockito.times(3)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void disabledDoesNotFilter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false, 1);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/v1/clubs"), response, chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void prefersForwardedForClientIp() {
        MockHttpServletRequest request = request("/api/v1/clubs");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        assertThat(RateLimitFilter.resolveClientIp(request)).isEqualTo("203.0.113.10");
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("198.51.100.20");
        return request;
    }
}
