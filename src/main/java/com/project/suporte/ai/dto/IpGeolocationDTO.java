package com.project.suporte.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IpGeolocationDTO(
    String query,
    String status,
    String message,
    String country,
    String countryCode,
    String region,
    String regionName,
    String city,
    String zip,
    double lat,
    double lon,
    String timezone,
    String isp,
    String org,
    String as
) {}
