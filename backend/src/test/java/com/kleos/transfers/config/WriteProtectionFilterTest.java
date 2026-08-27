package com.kleos.transfers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WriteProtectionFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StringWriter responseBody;
    private PrintWriter writer;

    private void stubResponseWriter() throws Exception {
        responseBody = new StringWriter();
        writer = new PrintWriter(responseBody);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void allowsPublicPredictionCreateWithoutKey() throws Exception {
        WriteProtectionFilter filter = new WriteProtectionFilter(true, "secret-key");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/predictions");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void blocksBulkImportWithoutKey() throws Exception {
        stubResponseWriter();
        WriteProtectionFilter filter = new WriteProtectionFilter(true, "secret-key");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/players/bulk");
        when(request.getHeader(WriteProtectionFilter.INGEST_KEY_HEADER)).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(filterChain, never()).doFilter(any(), any());
        writer.flush();
        assertThat(responseBody.toString()).contains("ingest credentials");
    }

    @Test
    void allowsBulkImportWithMatchingKey() throws Exception {
        WriteProtectionFilter filter = new WriteProtectionFilter(true, "secret-key");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/players/bulk");
        when(request.getHeader(WriteProtectionFilter.INGEST_KEY_HEADER)).thenReturn("secret-key");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blocksEvaluationWritesWithoutKey() throws Exception {
        stubResponseWriter();
        WriteProtectionFilter filter = new WriteProtectionFilter(true, "secret-key");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/predictions/550e8400-e29b-41d4-a716-446655440000/evaluate");
        when(request.getHeader(WriteProtectionFilter.INGEST_KEY_HEADER)).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void blocksAllWritesWhenKeyUnsetInProd() throws Exception {
        stubResponseWriter();
        WriteProtectionFilter filter = new WriteProtectionFilter(true, "");
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/v1/players/550e8400-e29b-41d4-a716-446655440000");

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        writer.flush();
        assertThat(responseBody.toString()).contains("disabled on this deployment");
    }

    @Test
    void disabledFilterSkipsMutations() throws Exception {
        WriteProtectionFilter filter = new WriteProtectionFilter(false, "");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }
}
