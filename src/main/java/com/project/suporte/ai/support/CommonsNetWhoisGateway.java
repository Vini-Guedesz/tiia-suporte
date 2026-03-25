package com.project.suporte.ai.support;

import com.project.suporte.ai.config.DiagnosticsProperties;
import org.apache.commons.net.whois.WhoisClient;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CommonsNetWhoisGateway implements WhoisGateway {

    private final DiagnosticsProperties properties;

    public CommonsNetWhoisGateway(DiagnosticsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String query(String server, String value) throws IOException {
        WhoisClient client = new WhoisClient();
        client.setDefaultTimeout(properties.getWhois().getConnectTimeoutMs());
        client.setConnectTimeout(properties.getWhois().getConnectTimeoutMs());

        try {
            client.connect(server);
            client.setSoTimeout(properties.getWhois().getReadTimeoutMs());
            return client.query(value);
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
        }
    }
}
