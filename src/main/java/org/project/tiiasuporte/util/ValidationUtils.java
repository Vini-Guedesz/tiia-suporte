package org.project.tiiasuporte.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final String IPV4_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final String IPV6_REGEX = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|^([0-9a-fA-F]{1,4}:){1,7}:$|^([0-9a-fA-F]{1,4}:)([0-9a-fA-F]{1,4}:){0,5}(:[0-9a-fA-F]{1,4}){1,6}$";
    private static final String HOSTNAME_REGEX = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9])$";

    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);
    private static final Pattern IPV6_PATTERN = Pattern.compile(IPV6_REGEX);
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEX);

    /**
     * Valida se uma string é um endereço IP válido (IPv4 ou IPv6)
     * Usa InetAddress para validação mais robusta
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        // Primeiro tenta com regex para performance
        if (IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches()) {
            return true;
        }

        // Fallback: valida com InetAddress (suporta formatos IPv6 complexos)
        try {
            InetAddress addr = InetAddress.getByName(ip);
            // Verifica se o endereço parseado é igual ao original (evita DNS lookup)
            return addr.getHostAddress().equals(ip) || isValidIpv6Variant(ip, addr);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Verifica variantes válidas de IPv6 (com zeros comprimidos, etc)
     */
    private static boolean isValidIpv6Variant(String original, InetAddress addr) {
        String normalized = addr.getHostAddress();
        // Remove scope id se presente (ex: %eth0)
        if (normalized.contains("%")) {
            normalized = normalized.substring(0, normalized.indexOf("%"));
        }
        if (original.contains("%")) {
            original = original.substring(0, original.indexOf("%"));
        }
        return normalized.replace(":", "").equalsIgnoreCase(original.replace(":", ""));
    }

    /**
     * Valida se uma string é um hostname válido
     */
    public static boolean isValidHostname(String hostname) {
        return hostname != null && HOSTNAME_PATTERN.matcher(hostname).matches();
    }

    /**
     * Valida se uma string é um endereço IP (IPv4/IPv6) ou hostname válido
     */
    public static boolean isValidIpOrHostname(String input) {
        return isValidIpAddress(input) || isValidHostname(input);
    }

    /**
     * Valida se uma string é especificamente um IPv4
     */
    public static boolean isValidIpv4(String ip) {
        return ip != null && IPV4_PATTERN.matcher(ip).matches();
    }

    /**
     * Valida se uma string é especificamente um IPv6
     */
    public static boolean isValidIpv6(String ip) {
        if (ip == null) return false;
        if (IPV6_PATTERN.matcher(ip).matches()) return true;

        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.getHostAddress().contains(":");
        } catch (UnknownHostException e) {
            return false;
        }
    }
}