package org.project.tiiasuporte.traceroute;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TracerouteHop {
    private int hopNumber;
    private String ipAddress;
    private String hostname;
    private String country;
    private String city;
    private double lat;
    private double lon;

    // Constructors
    public TracerouteHop() {
    }

    public TracerouteHop(int hopNumber, String ipAddress, String hostname) {
        this.hopNumber = hopNumber;
        this.ipAddress = ipAddress;
        this.hostname = hostname;
    }

    // Getters and Setters
    public int getHopNumber() {
        return hopNumber;
    }

    public void setHopNumber(int hopNumber) {
        this.hopNumber = hopNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
