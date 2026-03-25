package com.project.suporte.ai.support;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface AddressResolver {
    InetAddress[] resolveAll(String host) throws UnknownHostException;
}
