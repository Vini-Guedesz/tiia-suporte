package com.project.suporte.ai.support;

import java.io.IOException;

public interface WhoisGateway {
    String query(String server, String value) throws IOException;
}
