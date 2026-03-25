package com.project.suporte.ai.support;

import com.project.suporte.ai.exceptions.ApiException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TargetValidatorTest {

    @Test
    void shouldNormalizeHostFromUrl() {
        AddressResolver resolver = mock(AddressResolver.class);
        TargetValidator validator = new TargetValidator(resolver);

        assertEquals("openai.com", validator.normalizeTarget("https://OpenAI.com/docs"));
    }

    @Test
    void shouldRejectIpAsDomain() {
        AddressResolver resolver = mock(AddressResolver.class);
        TargetValidator validator = new TargetValidator(resolver);

        assertThrows(ApiException.class, () -> validator.normalizeDomain("8.8.8.8"));
    }

    @Test
    void shouldRejectLocalhostForPortScan() {
        AddressResolver resolver = mock(AddressResolver.class);
        TargetValidator validator = new TargetValidator(resolver);

        assertThrows(ApiException.class, () -> validator.normalizePortScanTarget("localhost"));
    }

    @Test
    void shouldRequirePublicAddressForGeolocation() throws Exception {
        AddressResolver resolver = mock(AddressResolver.class);
        TargetValidator validator = new TargetValidator(resolver);
        when(resolver.resolveAll("internal.local")).thenReturn(new InetAddress[]{InetAddress.getByName("127.0.0.1")});

        assertThrows(ApiException.class, () -> validator.resolvePublicAddress("internal.local"));
    }
}
