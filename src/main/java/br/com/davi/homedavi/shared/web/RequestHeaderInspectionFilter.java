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
import java.util.List;
import java.util.Set;

/**
 * Equivalente, no Spring, a um ContainerRequestFilter (@Provider) do Quarkus: roda antes de a requisição
 * chegar aos controllers. Aqui ela apenas inspeciona (loga) os headers e segue o fluxo.
 *
 * <p>Para interromper a requisição (como um filtro que aborta no Quarkus), escreva na response e
 * <b>não</b> chame {@code filterChain.doFilter(...)} — veja o exemplo comentado em {@link #doFilterInternal}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestHeaderInspectionFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestHeaderInspectionFilter.class);

    /**
     * Headers cujo valor não deve ir para o log (segredos, assinaturas, credenciais).
     */
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "x-api-key",
            "x-webhook-signature", "x-webhook-signature-v2", "x-webhook-timestamp");

    private static String headerValue(String name, HttpServletRequest request) {
        if (SENSITIVE_HEADERS.contains(name.toLowerCase())) return "***";
        return String.join(", ", java.util.Collections.list(request.getHeaders(name)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
        for (String name : List.copyOf(java.util.Collections.list(request.getHeaderNames()))) {
            log.info("  header {} = {}", name, headerValue(name, request));
        }

        // Para abortar antes de chegar na aplicação (ex.: header obrigatório ausente), faça algo como:
        // if (request.getHeader("X-Required-Header") == null) {
        //     response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Required-Header ausente");
        //     return; // não chama doFilter: a requisição não segue para os controllers
        // }

        filterChain.doFilter(request, response);
    }
}
