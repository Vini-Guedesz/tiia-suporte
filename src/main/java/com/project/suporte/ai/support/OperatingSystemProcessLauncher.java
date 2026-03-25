package com.project.suporte.ai.support;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class OperatingSystemProcessLauncher implements ProcessLauncher {

    @Override
    public Process start(List<String> command) throws IOException {
        return new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
    }
}
