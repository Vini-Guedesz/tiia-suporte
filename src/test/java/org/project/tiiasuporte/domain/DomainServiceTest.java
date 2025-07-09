package org.project.tiiasuporte.domain;

import org.apache.commons.net.whois.WhoisClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainServiceTest {

    @Mock
    private WhoisClient whoisClient;

    @InjectMocks
    private DomainService domainService;

    @Test
    void testGetDomainInfo_Success() throws IOException {
        String domainName = "google.com";
        String whoisResponse = "Whois data";

        when(whoisClient.query(domainName)).thenReturn(whoisResponse);

        String actualResponse = domainService.getDomainInfo(domainName);

        assertTrue(actualResponse.contains(whoisResponse));
    }

    @Test
    void testGetDomainInfo_InvalidDomain() {
        String domainName = null;
        String expectedResponse = "Nome de domínio inválido.";

        String actualResponse = domainService.getDomainInfo(domainName);

        assertEquals(expectedResponse, actualResponse);
    }
}
