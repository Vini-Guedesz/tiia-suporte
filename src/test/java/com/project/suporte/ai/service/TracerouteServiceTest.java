package com.project.suporte.ai.service;

import com.project.suporte.ai.support.TargetValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TracerouteServiceTest {

    @Test
    void shouldBuildCommandAndDelegateStreaming() {
        TargetValidator validator = mock(TargetValidator.class);
        CommandStreamingService streamingService = mock(CommandStreamingService.class);
        TracerouteService tracerouteService = new TracerouteService(validator, streamingService);

        when(validator.normalizeTarget("1.1.1.1")).thenReturn("1.1.1.1");

        tracerouteService.executeTraceroute(new SseEmitter(), "1.1.1.1");

        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> expected = isWindows
                ? List.of("tracert", "1.1.1.1")
                : List.of("traceroute", "1.1.1.1");

        verify(streamingService).stream(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("traceroute"), org.mockito.ArgumentMatchers.eq("1.1.1.1"), org.mockito.ArgumentMatchers.eq(expected));
    }
}
