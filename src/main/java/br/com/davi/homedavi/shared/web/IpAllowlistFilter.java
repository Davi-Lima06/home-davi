package br.com.davi.homedavi.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** Bloqueia (403) requisições de IPs fora da allowlist. O loopback é sempre permitido. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IpAllowlistFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(IpAllowlistFilter.class);

    private final IpAllowlistProperties properties;

    public IpAllowlistFilter(IpAllowlistProperties properties) { this.properties = properties; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) { filterChain.doFilter(request, response); return; }

        String clientIp = WebClientIp.resolve(request, properties.isTrustForwardedFor());
        InetAddress address;
        try {
            address = InetAddress.getByName(clientIp);
        } catch (UnknownHostException exception) {
            log.warn("Blocked request with unresolvable client IP '{}': {} {}", clientIp, request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (address.isLoopbackAddress() || isAllowed(address)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Blocked request from non-allowlisted IP {}: {} {}", clientIp, request.getMethod(), request.getRequestURI());
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean isAllowed(InetAddress address) {
        for (String entry : properties.getEntries()) {
            if (matches(entry.trim(), address)) return true;
        }
        return false;
    }

    /** Casa um IP exato ou uma faixa CIDR (IPv4/IPv6) com o endereço do cliente. */
    private static boolean matches(String entry, InetAddress address) {
        try {
            if (!entry.contains("/")) return InetAddress.getByName(entry).equals(address);
            String[] parts = entry.split("/", 2);
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            byte[] target = address.getAddress();
            if (network.length != target.length) return false; // IPv4 vs IPv6
            int prefix = Integer.parseInt(parts[1]);
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) if (network[i] != target[i]) return false;
            if (remainingBits > 0) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((network[fullBytes] & mask) != (target[fullBytes] & mask)) return false;
            }
            return true;
        } catch (RuntimeException | UnknownHostException exception) {
            log.warn("Ignoring invalid allowlist entry '{}'", entry);
            return false;
        }
    }
}
