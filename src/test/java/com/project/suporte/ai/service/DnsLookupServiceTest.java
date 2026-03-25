package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.DnsLookupResponseDTO;
import com.project.suporte.ai.support.TargetValidator;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DnsLookupServiceTest {

    @Test
    void shouldResolveAndCacheDnsLookup() throws Exception {
        TargetValidator validator = mock(TargetValidator.class);
        DnsLookupService service = new DnsLookupService(validator, new DiagnosticsProperties());

        when(validator.normalizeTarget("openai.com")).thenReturn("openai.com");
        when(validator.resolveAddresses("openai.com")).thenReturn(List.of(
                InetAddress.getByName("172.64.154.211"),
                InetAddress.getByName("104.18.33.45"),
                InetAddress.getByName("104.18.33.45")
        ));

        DnsLookupResponseDTO first = service.lookup("openai.com");
        DnsLookupResponseDTO second = service.lookup("openai.com");

        assertEquals(List.of("104.18.33.45", "172.64.154.211"), first.ipAddresses());
        assertEquals(first, second);
        verify(validator, times(1)).resolveAddresses("openai.com");
    }
}
