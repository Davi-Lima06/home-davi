package br.com.davi.homedavi.shared.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolve o IP do cliente. Por padrão usa o remoteAddr; opcionalmente confia no X-Forwarded-For.
 */
final class WebClientIp {
    private WebClientIp() {
    }

    static String resolve(HttpServletRequest request, boolean trustForwardedFor) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
