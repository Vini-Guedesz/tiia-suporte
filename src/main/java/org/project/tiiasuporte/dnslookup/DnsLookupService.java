package org.project.tiiasuporte.dnslookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.project.tiiasuporte.util.ValidationUtils;

import javax.annotation.PreDestroy;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Service
public class DnsLookupService {

    private static final Logger logger = LoggerFactory.getLogger(DnsLookupService.class);

    private final DirContext dirContext;

    public DnsLookupService() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        this.dirContext = new InitialDirContext(env);
    }

    // Constructor for testing purposes
    public DnsLookupService(DirContext dirContext) {
        this.dirContext = dirContext;
    }

    @PreDestroy
    public void cleanup() {
        if (dirContext != null) {
            try {
                dirContext.close();
                logger.info("DirContext fechado com sucesso");
            } catch (NamingException e) {
                logger.error("Erro ao fechar DirContext: {}", e.getMessage(), e);
            }
        }
    }

    public CompletableFuture<Map<String, Object>> dnsLookup(String host) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new LinkedHashMap<>();

            if (!ValidationUtils.isValidHostname(host)) {
                logger.warn("Tentativa de DNS lookup com host inválido: {}", host);
                result.put("error", "Nome de host inválido.");
                return result;
            }

            try {
                Attributes attrs = dirContext.getAttributes(host, new String[]{"A", "AAAA", "MX", "NS", "TXT", "CNAME"});

                NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
                try {
                    while (allAttrs.hasMore()) {
                        Attribute attr = allAttrs.next();
                        List<String> values = new ArrayList<>();
                        NamingEnumeration<?> attrValues = attr.getAll();
                        try {
                            while (attrValues.hasMore()) {
                                values.add(attrValues.next().toString());
                            }
                        } finally {
                            attrValues.close();
                        }
                        result.put(attr.getID(), values);
                    }
                } finally {
                    allAttrs.close();
                }
                logger.info("DNS Lookup para {} concluído: {}", host, result);
            } catch (NamingException e) {
                logger.error("Erro ao realizar DNS lookup para {}: {}", host, e.getMessage(), e);
                result.put("error", "Erro ao realizar DNS lookup: " + e.getMessage());
            }
            return result;
        });
    }
}
