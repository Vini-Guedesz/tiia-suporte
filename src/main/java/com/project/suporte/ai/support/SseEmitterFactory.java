package com.project.suporte.ai.support;

import com.project.suporte.ai.config.DiagnosticsProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterFactory {

    private final DiagnosticsProperties properties;

    public SseEmitterFactory(DiagnosticsProperties properties) {
        this.properties = properties;
    }

    public SseEmitter create() {
        return new SseEmitter(properties.getSse().getTimeoutMs());
    }

    public SseEmitter createContinuous() {
        return new SseEmitter(0L);
    }
}
