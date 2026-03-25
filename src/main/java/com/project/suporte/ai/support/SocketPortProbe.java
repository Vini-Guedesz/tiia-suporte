package com.project.suporte.ai.support;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class SocketPortProbe implements PortProbe {

    @Override
    public boolean isOpen(InetSocketAddress address, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(address, timeoutMs);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
