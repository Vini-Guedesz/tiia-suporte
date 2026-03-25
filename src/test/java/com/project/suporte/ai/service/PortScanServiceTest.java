package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.PortScanRequestDTO;
import com.project.suporte.ai.dto.PortScanResponseDTO;
import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.support.PortProbe;
import com.project.suporte.ai.support.TargetValidator;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortScanServiceTest {

    @Test
    void shouldReturnOpenPortsOnly() {
        TargetValidator validator = mock(TargetValidator.class);
        PortProbe portProbe = mock(PortProbe.class);
        Executor directExecutor = Runnable::run;
        PortScanService service = new PortScanService(validator, portProbe, directExecutor, new DiagnosticsProperties());

        when(validator.normalizePortScanTarget("scanme.nmap.org")).thenReturn("scanme.nmap.org");
        when(portProbe.isOpen(argThat(address -> matches(address, "scanme.nmap.org", 80)), eq(800))).thenReturn(true);
        when(portProbe.isOpen(argThat(address -> matches(address, "scanme.nmap.org", 443)), eq(800))).thenReturn(false);

        PortScanResponseDTO response = service.scanPorts(new PortScanRequestDTO("scanme.nmap.org", List.of(443, 80, 80), 800));

        assertEquals("scanme.nmap.org", response.host());
        assertEquals(List.of(80), response.openPorts());
    }

    @Test
    void shouldRejectTooManyPorts() {
        TargetValidator validator = mock(TargetValidator.class);
        PortProbe portProbe = mock(PortProbe.class);
        Executor directExecutor = Runnable::run;
        PortScanService service = new PortScanService(validator, portProbe, directExecutor, new DiagnosticsProperties());

        when(validator.normalizePortScanTarget("scanme.nmap.org")).thenReturn("scanme.nmap.org");

        List<Integer> manyPorts = IntStream.rangeClosed(1, 65).boxed().toList();
        assertThrows(ApiException.class, () -> service.scanPorts(new PortScanRequestDTO("scanme.nmap.org", manyPorts, 800)));
    }

    private static boolean matches(InetSocketAddress address, String host, int port) {
        return address != null && address.getHostString().equals(host) && address.getPort() == port;
    }
}
