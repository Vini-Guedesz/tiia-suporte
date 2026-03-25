package com.project.suporte.ai.service;

import com.project.suporte.ai.support.TargetValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PingServiceTest {

    @Test
    void shouldBuildCommandAndDelegateStreaming() {
        TargetValidator validator = mock(TargetValidator.class);
        CommandStreamingService streamingService = mock(CommandStreamingService.class);
        PingService pingService = new PingService(validator, streamingService);

        when(validator.normalizeTarget("openai.com")).thenReturn("openai.com");

        pingService.executePing(new SseEmitter(), "openai.com", 3);

        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> expected = isWindows
                ? List.of("ping", "-n", "3", "openai.com")
                : List.of("ping", "-c", "3", "openai.com");

        verify(streamingService).stream(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("ping"), org.mockito.ArgumentMatchers.eq("openai.com"), org.mockito.ArgumentMatchers.eq(expected));
    }
}
