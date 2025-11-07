package org.project.tiiasuporte.domain;

public class DomainInfoResponse {
    private String status;
    private String ipAddress;
    private String whoisInfo;
    private String error;

    public DomainInfoResponse() {
    }

    public DomainInfoResponse(String status, String ipAddress, String whoisInfo, String error) {
        this.status = status;
        this.ipAddress = ipAddress;
        this.whoisInfo = whoisInfo;
        this.error = error;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getWhoisInfo() {
        return whoisInfo;
    }

    public void setWhoisInfo(String whoisInfo) {
        this.whoisInfo = whoisInfo;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
