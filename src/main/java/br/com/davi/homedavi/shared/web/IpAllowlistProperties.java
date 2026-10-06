package br.com.davi.homedavi.shared.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security.ip-allowlist")
public class IpAllowlistProperties {
    /**
     * Liga/desliga a allowlist. O loopback é sempre liberado, mesmo com ela ligada.
     */
    private boolean enabled = true;
    /**
     * Só use true se houver um proxy reverso confiável à frente; X-Forwarded-For é falsificável.
     */
    private boolean trustForwardedFor = false;
    /**
     * IPs ou faixas CIDR liberados, ex.: 52.67.145.81 ou 192.168.2.0/24.
     */
    private List<String> entries = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustForwardedFor() {
        return trustForwardedFor;
    }

    public void setTrustForwardedFor(boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    public List<String> getEntries() {
        return entries;
    }

    public void setEntries(List<String> entries) {
        this.entries = entries;
    }
}
