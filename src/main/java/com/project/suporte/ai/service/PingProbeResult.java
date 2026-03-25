package com.project.suporte.ai.service;

record PingProbeResult(
        boolean successful,
        Double latencyMs,
        String message,
        int exitCode
) {
}
