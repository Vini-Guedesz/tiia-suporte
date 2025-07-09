package org.project.tiiasuporte.domain;

import org.apache.commons.net.whois.WhoisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.project.tiiasuporte.util.ValidationUtils;

@Service
public class DomainService {

    private final WhoisClient whoisClient;

    @Autowired
    public DomainService(WhoisClient whoisClient) {
        this.whoisClient = whoisClient;
    }

    public String getDomainInfo(String domainName) {
        if (!ValidationUtils.isValidHostname(domainName)) {
            return "Nome de domínio inválido.";
        }

        StringBuilder result = new StringBuilder();

        // Check if domain is hosted
        try {
            InetAddress address = InetAddress.getByName(domainName);
            result.append("Status: Publicado (Ativo)\n");
            result.append("Endereço IP: ").append(address.getHostAddress()).append("\n\n");
        } catch (UnknownHostException e) {
            result.append("Status: Não Publicado (Inativo ou não registrado)\n\n");
        }

        // WHOIS lookup
        try {
            String whoisServer = WhoisClient.DEFAULT_HOST;
            if (domainName.endsWith(".br")) {
                whoisServer = "whois.registro.br";
            }
            whoisClient.connect(whoisServer);
            result.append("Informações do WHOIS:\n");
            result.append(whoisClient.query(domainName));
            whoisClient.disconnect();
        } catch (IOException e) {
            result.append("Não foi possível obter informações do WHOIS.");
        }

        return result.toString();
    }
}
