package com.project.suporte.ai.service;

import com.project.suporte.ai.dto.PingMonitorEventDTO;
import com.project.suporte.ai.support.ProcessLauncher;
import com.project.suporte.ai.support.TargetValidator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PingMonitorServiceTest {

    @Test
    void shouldExtractLatencyFromLocalizedOutput() {
        TargetValidator validator = mock(TargetValidator.class);
        ProcessLauncher launcher = mock(ProcessLauncher.class);
        PingMonitorService service = new PingMonitorService(validator, launcher);

        Double latency = service.extractLatencyMs(List.of("Resposta de 8.8.8.8: bytes=32 tempo=23ms TTL=117"));

        assertEquals(23.0, latency);
    }

    @Test
    void shouldBuildProbeResultFromPingOutput() throws Exception {
        TargetValidator validator = mock(TargetValidator.class);
        ProcessLauncher launcher = mock(ProcessLauncher.class);
        PingMonitorService service = new PingMonitorService(validator, launcher);

        when(launcher.start(anyList())).thenReturn(fakeProcess("""
                Disparando 8.8.8.8 com 32 bytes de dados:
                Resposta de 8.8.8.8: bytes=32 tempo=19ms TTL=117
                """, 0));

        PingProbeResult result = service.probeOnce("8.8.8.8", 2000);

        assertTrue(result.successful());
        assertEquals(19.0, result.latencyMs());
        assertTrue(result.message().contains("Ping OK"));
    }

    @Test
    void shouldAggregateOutagesAndPacketLoss() {
        PingMonitorAccumulator accumulator = new PingMonitorAccumulator("cliente.exemplo.com.br");

        PingMonitorEventDTO first = accumulator.sample(new PingProbeResult(true, 20.0, "Ping OK em 20 ms.", 0));
        PingMonitorEventDTO second = accumulator.sample(new PingProbeResult(false, null, "Sem resposta do alvo.", 1));
        PingMonitorEventDTO third = accumulator.sample(new PingProbeResult(false, null, "Sem resposta do alvo.", 1));

        assertEquals("ONLINE", first.status());
        assertFalse(second.connected());
        assertEquals(1, second.outages());
        assertEquals(1, third.outages());
        assertEquals(66.67, third.packetLossPercent());
        assertEquals(2, third.consecutiveFailures());
    }

    private static Process fakeProcess(String output, int exitCode) {
        return new Process() {
            @Override
            public OutputStream getOutputStream() {
                return OutputStream.nullOutputStream();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public InputStream getErrorStream() {
                return InputStream.nullInputStream();
            }

            @Override
            public int waitFor() {
                return exitCode;
            }

            @Override
            public int exitValue() {
                return exitCode;
            }

            @Override
            public void destroy() {
            }

            @Override
            public Process destroyForcibly() {
                return this;
            }

            @Override
            public boolean isAlive() {
                return false;
            }
        };
    }
}
