package br.com.davi.homedavi.shared.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {
    /**
     * Liga/desliga o rate limit.
     */
    private boolean enabled = true;
    /**
     * Máximo de requisições por IP dentro da janela.
     */
    private int capacity = 100;
    /**
     * Janela de reabastecimento do bucket (ex.: PT1M = 1 minuto).
     */
    private Duration window = Duration.ofMinutes(1);
    /**
     * Limite de IPs rastreados em memória; evita crescimento sem limite sob abuso.
     */
    private int maxTrackedIps = 100_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public int getMaxTrackedIps() {
        return maxTrackedIps;
    }

    public void setMaxTrackedIps(int maxTrackedIps) {
        this.maxTrackedIps = maxTrackedIps;
    }
}
