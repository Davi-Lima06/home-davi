package br.com.davi.homedavi.shared.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebSecurityFiltersTests {

    private static MockHttpServletRequest request(String ip) {
        var request = new MockHttpServletRequest("GET", "/api/v1/webhooks/pluggy");
        request.setRemoteAddr(ip);
        return request;
    }

    // --- Allowlist ---

    private IpAllowlistFilter allowlist() {
        var properties = new IpAllowlistProperties();
        properties.setEntries(List.of("52.67.145.81", "192.168.2.0/24", "172.18.0.1"));
        return new IpAllowlistFilter(properties);
    }

    @Test
    void allowsConfiguredIpAndCidrAndLoopback() throws ServletException, IOException {
        for (String ip : List.of("52.67.145.81", "192.168.2.50", "127.0.0.1", "172.18.0.1")) {
            var chain = new MockFilterChain();
            var response = new MockHttpServletResponse();
            allowlist().doFilter(request(ip), response, chain);
            assertNotNull(chain.getRequest(), "deveria passar para a aplicação: " + ip);
            assertEquals(200, response.getStatus(), ip);
        }
    }

    @Test
    void blocksIpOutsideAllowlist() throws ServletException, IOException {
        var chain = new MockFilterChain();
        var response = new MockHttpServletResponse();
        allowlist().doFilter(request("8.8.8.8"), response, chain);
        assertNull(chain.getRequest(), "não deveria chegar na aplicação");
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsEverythingWhenDisabled() throws ServletException, IOException {
        var properties = new IpAllowlistProperties();
        properties.setEnabled(false);
        var chain = new MockFilterChain();
        var response = new MockHttpServletResponse();
        new IpAllowlistFilter(properties).doFilter(request("8.8.8.8"), response, chain);
        assertNotNull(chain.getRequest());
    }

    // --- Rate limit ---

    @Test
    void returns429AfterCapacityIsExceeded() throws ServletException, IOException {
        var properties = new RateLimitProperties();
        properties.setCapacity(2);
        properties.setWindow(Duration.ofMinutes(1));
        var filter = new RateLimitFilter(properties);

        for (int i = 1; i <= 2; i++) {
            var chain = new MockFilterChain();
            var response = new MockHttpServletResponse();
            filter.doFilter(request("52.67.145.81"), response, chain);
            assertNotNull(chain.getRequest(), "requisição " + i + " deveria passar");
        }

        var chain = new MockFilterChain();
        var response = new MockHttpServletResponse();
        filter.doFilter(request("52.67.145.81"), response, chain);
        assertNull(chain.getRequest(), "a 3ª deveria ser barrada");
        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
    }

    @Test
    void ratePerIpIsIndependent() throws ServletException, IOException {
        var properties = new RateLimitProperties();
        properties.setCapacity(1);
        properties.setWindow(Duration.ofMinutes(1));
        var filter = new RateLimitFilter(properties);

        var firstChain = new MockFilterChain();
        filter.doFilter(request("52.67.145.81"), new MockHttpServletResponse(), firstChain);
        assertNotNull(firstChain.getRequest());

        // Outro IP tem o próprio bucket, então ainda passa.
        var otherChain = new MockFilterChain();
        filter.doFilter(request("192.168.2.165"), new MockHttpServletResponse(), otherChain);
        assertNotNull(otherChain.getRequest());
    }

    @Test
    void bindsCommaSeparatedEntriesFromEnvIntoList() {
        var source = new MapConfigurationPropertySource(
                Map.of("security.ip-allowlist.entries", "52.67.145.81,192.168.2.165,192.168.2.185,172.18.0.1"));
        var properties = new Binder(source).bind("security.ip-allowlist", IpAllowlistProperties.class).get();
        assertEquals(List.of("52.67.145.81", "192.168.2.165", "192.168.2.185", "172.18.0.1"), properties.getEntries());
    }
}
