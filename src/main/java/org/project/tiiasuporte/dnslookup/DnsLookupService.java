package org.project.tiiasuporte.dnslookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.project.tiiasuporte.util.ValidationUtils;

import jakarta.annotation.PreDestroy;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.*;
import java.util.concurrent.*;

@Service
public class DnsLookupService {

    private static final Logger logger = LoggerFactory.getLogger(DnsLookupService.class);
    private static final String[] DNS_RECORD_TYPES = {"A", "AAAA", "MX", "NS", "TXT", "CNAME", "SOA"};
    private static final int DNS_TIMEOUT_MS = 5000; // 5 seconds timeout

    private final DirContext dirContext;
    private final ExecutorService dnsExecutor;

    public DnsLookupService() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(DNS_TIMEOUT_MS));
        env.put("com.sun.jndi.dns.timeout.retries", "2");
        this.dirContext = new InitialDirContext(env);
        this.dnsExecutor = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r);
            t.setName("dns-lookup-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }

    // Constructor for testing purposes
    public DnsLookupService(DirContext dirContext) {
        this.dirContext = dirContext;
        this.dnsExecutor = Executors.newFixedThreadPool(10);
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
        if (dnsExecutor != null) {
            dnsExecutor.shutdownNow();
            logger.info("DNS ExecutorService encerrado");
        }
    }

    public CompletableFuture<Map<String, Object>> dnsLookup(String host) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> result = new LinkedHashMap<>();

            if (!ValidationUtils.isValidHostname(host)) {
                logger.warn("Tentativa de DNS lookup com host inválido: {}", host);
                result.put("error", "Nome de host inválido.");
                return result;
            }

            try {
                // Execute DNS query with timeout
                Future<Attributes> attrsFuture = dnsExecutor.submit(() ->
                    dirContext.getAttributes(host, DNS_RECORD_TYPES)
                );

                Attributes attrs;
                try {
                    attrs = attrsFuture.get(DNS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    attrsFuture.cancel(true);
                    logger.warn("DNS lookup timeout para {}", host);
                    result.put("error", "Timeout ao realizar DNS lookup");
                    return result;
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Erro ao executar DNS lookup para {}: {}", host, e.getMessage());
                    result.put("error", "Erro ao realizar DNS lookup: " + e.getMessage());
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    return result;
                }

                // Process attributes efficiently
                NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
                try {
                    while (allAttrs.hasMore()) {
                        Attribute attr = allAttrs.next();
                        List<String> values = new ArrayList<>();
                        NamingEnumeration<?> attrValues = attr.getAll();
                        try {
                            while (attrValues.hasMore()) {
                                Object value = attrValues.next();
                                if (value != null) {
                                    values.add(value.toString());
                                }
                            }
                        } finally {
                            attrValues.close();
                        }
                        if (!values.isEmpty()) {
                            result.put(attr.getID(), values);
                        }
                    }
                } finally {
                    allAttrs.close();
                }

                long duration = System.currentTimeMillis() - startTime;
                result.put("queryTime", duration + "ms");
                logger.info("DNS Lookup para {} concluído em {}ms com {} registros", host, duration, result.size() - 1);

            } catch (NamingException e) {
                logger.error("Erro ao realizar DNS lookup para {}: {}", host, e.getMessage(), e);
                result.put("error", "Erro ao realizar DNS lookup: " + e.getMessage());
            }
            return result;
        }, dnsExecutor);
    }
}
