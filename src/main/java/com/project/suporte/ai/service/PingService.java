package com.project.suporte.ai.service;

import com.project.suporte.ai.support.TargetValidator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Locale;

@Service
public class PingService {

    private final TargetValidator targetValidator;
    private final CommandStreamingService commandStreamingService;

    public PingService(TargetValidator targetValidator, CommandStreamingService commandStreamingService) {
        this.targetValidator = targetValidator;
        this.commandStreamingService = commandStreamingService;
    }

    @Async("diagnosticsExecutor")
    public void executePing(SseEmitter emitter, String host, int count) {
        String target = targetValidator.normalizeTarget(host);
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> command = isWindows
                ? List.of("ping", "-n", String.valueOf(count), target)
                : List.of("ping", "-c", String.valueOf(count), target);

        commandStreamingService.stream(emitter, "ping", target, command);
    }
}
