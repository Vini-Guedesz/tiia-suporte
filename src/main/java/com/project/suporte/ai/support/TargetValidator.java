package com.project.suporte.ai.support;

import com.project.suporte.ai.exceptions.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TargetValidator {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"
    );
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)(?!-)([A-Za-z0-9-]{1,63})(\\.(?!-)[A-Za-z0-9-]{1,63})*(?<!-)$"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile("^[0-9A-Fa-f:]+$");

    private final AddressResolver addressResolver;

    public TargetValidator(AddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    public String normalizeTarget(String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_target", "Informe um host, IP ou URL válido.");
        }

        String extracted = extractHost(rawTarget.trim());
        String ascii = IDN.toASCII(extracted, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);

        if (!isIpv4(ascii) && !isIpv6(ascii) && !isHostname(ascii)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_target", "Informe um host, IP ou URL válido.");
        }

        return ascii;
    }

    public String normalizeDomain(String rawDomain) {
        String target = normalizeTarget(rawDomain);
        if (isIpLiteral(target) || !target.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_domain", "Informe um domínio válido.");
        }
        return target;
    }

    public List<InetAddress> resolveAddresses(String target) {
        try {
            return Arrays.asList(addressResolver.resolveAll(target));
        } catch (UnknownHostException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "target_not_found", "Não foi possível resolver o alvo informado.", exception);
        }
    }

    public InetAddress resolvePublicAddress(String rawTarget) {
        String target = normalizeTarget(rawTarget);
        return resolveAddresses(target).stream()
                .filter(address -> !isRestricted(address))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "public_ip_required",
                        "A geolocalização requer um IP público ou domínio resolvível para IP público."
                ));
    }

    public String normalizePortScanTarget(String rawTarget) {
        String target = normalizeTarget(rawTarget);

        if ("localhost".equalsIgnoreCase(target)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "restricted_target", "Port scan em localhost ou redes privadas não é permitido.");
        }

        boolean hasRestrictedAddress = resolveAddresses(target).stream().anyMatch(this::isRestricted);
        if (hasRestrictedAddress) {
            throw new ApiException(HttpStatus.FORBIDDEN, "restricted_target", "Port scan em localhost ou redes privadas não é permitido.");
        }

        return target;
    }

    private String extractHost(String rawTarget) {
        try {
            String candidate = rawTarget.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")
                    ? rawTarget
                    : "http://" + rawTarget;

            URI uri = new URI(candidate);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                host = uri.getPath();
            }
            if (host == null || host.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_target", "Informe um host, IP ou URL válido.");
            }
            return host.replace("[", "").replace("]", "");
        } catch (URISyntaxException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_target", "Informe um host, IP ou URL válido.", exception);
        }
    }

    private boolean isHostname(String value) {
        return HOSTNAME_PATTERN.matcher(value).matches();
    }

    private boolean isIpv4(String value) {
        return IPV4_PATTERN.matcher(value).matches();
    }

    private boolean isIpv6(String value) {
        return value.contains(":") && IPV6_PATTERN.matcher(value).matches();
    }

    private boolean isIpLiteral(String value) {
        return isIpv4(value) || isIpv6(value);
    }

    private boolean isRestricted(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isCarrierGradeNat(address)
                || isUniqueLocalIpv6(address);
    }

    private boolean isCarrierGradeNat(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 4
                && Byte.toUnsignedInt(bytes[0]) == 100
                && Byte.toUnsignedInt(bytes[1]) >= 64
                && Byte.toUnsignedInt(bytes[1]) <= 127;
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        int firstByte = Byte.toUnsignedInt(address.getAddress()[0]);
        return (firstByte & 0xFE) == 0xFC;
    }
}
