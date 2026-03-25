package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.WhoisResponseDTO;
import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.support.TargetValidator;
import com.project.suporte.ai.support.WhoisGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WhoisServiceTest {

    @Test
    void shouldParseKnownTldResponse() throws Exception {
        TargetValidator validator = mock(TargetValidator.class);
        WhoisGateway gateway = mock(WhoisGateway.class);
        WhoisService service = new WhoisService(validator, gateway, new DiagnosticsProperties());

        when(validator.normalizeDomain("openai.com")).thenReturn("openai.com");
        when(gateway.query("whois.verisign-grs.com", "openai.com")).thenReturn("""
                Registrar: MarkMonitor Inc.
                Creation Date: 2015-04-24T00:00:00Z
                Registry Expiry Date: 2030-04-24T00:00:00Z
                Name Server: NS1.EXAMPLE.COM
                Name Server: NS2.EXAMPLE.COM
                Domain Status: clientTransferProhibited
                """);

        WhoisResponseDTO response = service.lookup("openai.com");

        assertEquals("openai.com", response.dominio());
        assertEquals("MarkMonitor Inc.", response.registrador());
    }

    @Test
    void shouldResolveReferralServerForUnknownTld() throws Exception {
        TargetValidator validator = mock(TargetValidator.class);
        WhoisGateway gateway = mock(WhoisGateway.class);
        WhoisService service = new WhoisService(validator, gateway, new DiagnosticsProperties());

        when(validator.normalizeDomain("example.io")).thenReturn("example.io");
        when(gateway.query("whois.iana.org", "io")).thenReturn("refer: whois.nic.io");
        when(gateway.query("whois.nic.io", "example.io")).thenReturn("""
                Registrar: Example Registrar
                Creation Date: 2020-01-01T00:00:00Z
                Registry Expiry Date: 2030-01-01T00:00:00Z
                Name Server: NS1.EXAMPLE.IO
                Domain Status: ok
                """);

        WhoisResponseDTO response = service.lookup("example.io");

        assertEquals("Example Registrar", response.registrador());
    }

    @Test
    void shouldThrowWhenDomainIsNotFound() throws Exception {
        TargetValidator validator = mock(TargetValidator.class);
        WhoisGateway gateway = mock(WhoisGateway.class);
        WhoisService service = new WhoisService(validator, gateway, new DiagnosticsProperties());

        when(validator.normalizeDomain("missing.com")).thenReturn("missing.com");
        when(gateway.query("whois.verisign-grs.com", "missing.com")).thenReturn("No match for domain");

        assertThrows(ApiException.class, () -> service.lookup("missing.com"));
    }
}
