package com.project.suporte.ai.service;

import com.project.suporte.ai.dto.CommandStreamEventDTO;
import com.project.suporte.ai.support.ProcessLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CommandStreamingService {

    private final ProcessLauncher processLauncher;

    public CommandStreamingService(ProcessLauncher processLauncher) {
        this.processLauncher = processLauncher;
    }

    public void stream(SseEmitter emitter, String operation, String target, List<String> command) {
        AtomicBoolean completed = new AtomicBoolean(false);

        try {
            Process process = processLauncher.start(command);
            registerCallbacks(emitter, process, operation, target, completed);
            safeSend(emitter, "started", operation, target, "Execução iniciada.", null, false);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!completed.get() && (line = reader.readLine()) != null) {
                    safeSend(emitter, "output", operation, target, line, null, false);
                }
            }

            if (completed.get()) {
                return;
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                safeSend(emitter, "completed", operation, target, "Execução concluída.", exitCode, true);
            } else {
                safeSend(emitter, "error", operation, target, "O comando terminou com falha.", exitCode, true);
            }

            completed.set(true);
            emitter.complete();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            finishWithError(emitter, completed, operation, target, "Execução interrompida.");
        } catch (Exception exception) {
            finishWithError(emitter, completed, operation, target, "Falha ao executar o comando: " + exception.getMessage());
        }
    }

    private void registerCallbacks(
            SseEmitter emitter,
            Process process,
            String operation,
            String target,
            AtomicBoolean completed
    ) {
        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                destroyProcess(process);
                safeSend(emitter, "timeout", operation, target, "Tempo limite excedido.", null, true);
                emitter.complete();
            }
        });

        emitter.onCompletion(() -> {
            completed.set(true);
            destroyProcess(process);
        });

        emitter.onError(error -> {
            if (completed.compareAndSet(false, true)) {
                destroyProcess(process);
            }
        });
    }

    private void finishWithError(
            SseEmitter emitter,
            AtomicBoolean completed,
            String operation,
            String target,
            String message
    ) {
        if (completed.compareAndSet(false, true)) {
            safeSend(emitter, "error", operation, target, message, null, true);
            emitter.complete();
        }
    }

    private void destroyProcess(Process process) {
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private void safeSend(
            SseEmitter emitter,
            String type,
            String operation,
            String target,
            String message,
            Integer exitCode,
            boolean finished
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(new CommandStreamEventDTO(type, operation, target, message, exitCode, finished, Instant.now())));
        } catch (Exception ignored) {
            emitter.complete();
        }
    }
}
