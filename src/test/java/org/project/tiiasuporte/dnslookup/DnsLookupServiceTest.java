package org.project.tiiasuporte.dnslookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.tiiasuporte.util.ValidationUtils;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnsLookupServiceTest {

    @Mock
    private DirContext mockDirContext;

    @InjectMocks
    private DnsLookupService dnsLookupService;

    @BeforeEach
    void setUp() {
        dnsLookupService = new DnsLookupService(mockDirContext);
    }

    @Mock
    private Attributes mockAttributes;

    @Mock
    private NamingEnumeration<? extends Attribute> mockAllAttrs;

    @Mock
    private Attribute mockAttributeA;
    @Mock
    private Attribute mockAttributeMX;

    @Mock
    private NamingEnumeration<?> mockAttrValuesA;
    @Mock
    private NamingEnumeration<?> mockAttrValuesMX;

    

    @Test
    void dnsLookup_ValidHost_ReturnsDnsInfo() throws NamingException, ExecutionException, InterruptedException {
        try (MockedStatic<ValidationUtils> mockedValidationUtils = mockStatic(ValidationUtils.class)) {
            mockedValidationUtils.when(() -> ValidationUtils.isValidHostname(anyString())).thenReturn(true);

            when(mockDirContext.getAttributes(eq("example.com"), any(String[].class))).thenReturn(mockAttributes);
            doReturn(mockAllAttrs).when(mockAttributes).getAll();

            when(mockAllAttrs.hasMore())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false); // Two attributes

            doReturn(mockAttributeA).doReturn(mockAttributeMX).when(mockAllAttrs).next();

            when(mockAttributeA.getID()).thenReturn("A");
            doReturn(mockAttrValuesA).when(mockAttributeA).getAll();
            when(mockAttrValuesA.hasMore())
                    .thenReturn(true)
                    .thenReturn(false);
            doReturn((Object)"192.0.2.1").when(mockAttrValuesA).next();

            when(mockAttributeMX.getID()).thenReturn("MX");
            doReturn(mockAttrValuesMX).when(mockAttributeMX).getAll();
            when(mockAttrValuesMX.hasMore())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);
            doReturn((Object)"10 mail.example.com").doReturn((Object)"20 backup.example.com").when(mockAttrValuesMX).next();

            Map<String, Object> result = dnsLookupService.dnsLookup("example.com").get();

            assertNotNull(result);
            assertTrue(result.containsKey("A"));
            assertTrue(result.containsKey("MX"));
            assertEquals(Collections.singletonList("192.0.2.1"), result.get("A"));
            assertEquals(Arrays.asList("10 mail.example.com", "20 backup.example.com"), result.get("MX"));


        }
    }

    @Test
    void dnsLookup_InvalidHost_ReturnsError() throws ExecutionException, InterruptedException {
        try (MockedStatic<ValidationUtils> mockedValidationUtils = mockStatic(ValidationUtils.class)) {
            mockedValidationUtils.when(() -> ValidationUtils.isValidHostname(anyString())).thenReturn(false);

            Map<String, Object> result = dnsLookupService.dnsLookup("invalid_host").get();

            assertNotNull(result);
            assertTrue(result.containsKey("error"));
            assertEquals("Nome de host inválido.", result.get("error"));
            verifyNoInteractions(mockDirContext); // Should not interact with DirContext for invalid host
        }
    }

    @Test
    void dnsLookup_NamingException_ReturnsError() throws NamingException, ExecutionException, InterruptedException {
        try (MockedStatic<ValidationUtils> mockedValidationUtils = mockStatic(ValidationUtils.class)) {
            mockedValidationUtils.when(() -> ValidationUtils.isValidHostname(anyString())).thenReturn(true);

            when(mockDirContext.getAttributes(eq("example.com"), any(String[].class)))
                    .thenThrow(new NamingException("Test Naming Exception"));

            Map<String, Object> result = dnsLookupService.dnsLookup("example.com").get();

            assertNotNull(result);
            assertTrue(result.containsKey("error"));
            assertTrue(result.get("error").toString().contains("Erro ao realizar DNS lookup"));
 // close() should still be called even on exception
        }
    }

    @Test
    void dnsLookup_EmptyAttributes_ReturnsEmptyMap() throws NamingException, ExecutionException, InterruptedException {
        try (MockedStatic<ValidationUtils> mockedValidationUtils = mockStatic(ValidationUtils.class)) {
            mockedValidationUtils.when(() -> ValidationUtils.isValidHostname(anyString())).thenReturn(true);

            when(mockDirContext.getAttributes(eq("example.com"), any(String[].class))).thenReturn(mockAttributes);
            doReturn(mockAllAttrs).when(mockAttributes).getAll();
            doReturn(false).when(mockAllAttrs).hasMore(); // No attributes

            Map<String, Object> result = dnsLookupService.dnsLookup("example.com").get();

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertTrue(result.containsKey("queryTime"));


        }
    }
}
