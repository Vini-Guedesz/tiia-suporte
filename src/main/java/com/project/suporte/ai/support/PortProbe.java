package com.project.suporte.ai.support;

import java.net.InetSocketAddress;

public interface PortProbe {
    boolean isOpen(InetSocketAddress address, int timeoutMs);
}
