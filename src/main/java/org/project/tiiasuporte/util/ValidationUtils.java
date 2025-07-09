package org.project.tiiasuporte.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final String IP_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final String HOSTNAME_REGEX = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9])$";

    private static final Pattern IP_PATTERN = Pattern.compile(IP_REGEX);
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEX);

    public static boolean isValidIpAddress(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidHostname(String hostname) {
        return hostname != null && HOSTNAME_PATTERN.matcher(hostname).matches();
    }

    public static boolean isValidIpOrHostname(String input) {
        return isValidIpAddress(input) || isValidHostname(input);
    }
}