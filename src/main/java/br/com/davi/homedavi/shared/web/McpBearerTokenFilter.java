package br.com.davi.homedavi.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Requires a configured Bearer token for the Streamable HTTP MCP endpoint only. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class McpBearerTokenFilter extends OncePerRequestFilter {
  private static final String MCP_PATH = "/mcp";
  private static final String BEARER_PREFIX = "Bearer ";

  private final byte[] expectedToken;

  public McpBearerTokenFilter(@Value("${security.mcp-auth.token:}") String token) {
    this.expectedToken = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!isMcpRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (expectedToken.length == 0) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "MCP authentication is not configured");
      return;
    }

    if (!hasValidBearerToken(request.getHeader("Authorization"))) {
      response.setHeader("WWW-Authenticate", "Bearer realm=\"home-davi-mcp\"");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private static boolean isMcpRequest(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    String requestPath = request.getRequestURI().substring(contextPath.length());
    return MCP_PATH.equals(requestPath) || requestPath.startsWith(MCP_PATH + "/");
  }

  private boolean hasValidBearerToken(String authorization) {
    if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      return false;
    }
    String providedToken = authorization.substring(BEARER_PREFIX.length()).trim();
    if (providedToken.isEmpty()) {
      return false;
    }
    return MessageDigest.isEqual(expectedToken, providedToken.getBytes(StandardCharsets.UTF_8));
  }
}
