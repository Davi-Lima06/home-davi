package br.com.davi.homedavi.shared.web;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpBearerTokenFilterTests {

  private static MockHttpServletRequest request(String path) {
    return new MockHttpServletRequest("POST", path);
  }

  @Test
  void allowsMcpRequestWithMatchingBearerToken() throws ServletException, IOException {
    var request = request("/mcp");
    request.addHeader("Authorization", "Bearer test-secret");
    var chain = new MockFilterChain();
    var response = new MockHttpServletResponse();

    new McpBearerTokenFilter("test-secret").doFilter(request, response, chain);

    assertNotNull(chain.getRequest());
    assertEquals(200, response.getStatus());
  }

  @Test
  void rejectsMcpRequestWithoutToken() throws ServletException, IOException {
    var chain = new MockFilterChain();
    var response = new MockHttpServletResponse();

    new McpBearerTokenFilter("test-secret").doFilter(request("/mcp"), response, chain);

    assertNull(chain.getRequest());
    assertEquals(401, response.getStatus());
    assertEquals("Bearer realm=\"home-davi-mcp\"", response.getHeader("WWW-Authenticate"));
  }

  @Test
  void rejectsMcpRequestWhenTokenIsNotConfigured() throws ServletException, IOException {
    var chain = new MockFilterChain();
    var response = new MockHttpServletResponse();

    new McpBearerTokenFilter("").doFilter(request("/mcp"), response, chain);

    assertNull(chain.getRequest());
    assertEquals(503, response.getStatus());
  }

  @Test
  void doesNotRequireMcpTokenForOtherRoutes() throws ServletException, IOException {
    var chain = new MockFilterChain();
    var response = new MockHttpServletResponse();

    new McpBearerTokenFilter("").doFilter(request("/actuator/health"), response, chain);

    assertNotNull(chain.getRequest());
    assertEquals(200, response.getStatus());
  }
}
