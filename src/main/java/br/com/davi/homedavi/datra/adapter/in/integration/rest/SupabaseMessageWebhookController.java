package br.com.davi.homedavi.datra.adapter.in.integration.rest;

import br.com.davi.homedavi.datra.application.port.in.messages.ReceiveSupabaseMessageWebhookUseCase;
import br.com.davi.homedavi.datra.application.service.messages.InvalidSupabaseMessageWebhookException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP boundary for the Supabase Database Webhook configured on public.logs_mensagens. */
@RestController
@RequestMapping("/api/v1/webhooks/supabase/messages")
public class SupabaseMessageWebhookController {
  private static final Logger log = LoggerFactory.getLogger(SupabaseMessageWebhookController.class);

  private final byte[] expectedSecret;
  private final ReceiveSupabaseMessageWebhookUseCase receiveWebhook;

  public SupabaseMessageWebhookController(
      @Value("${supabase.webhook.messages.secret:}") String webhookSecret,
      ReceiveSupabaseMessageWebhookUseCase receiveWebhook) {
    this.expectedSecret = webhookSecret == null ? new byte[0] : webhookSecret.getBytes(StandardCharsets.UTF_8);
    this.receiveWebhook = receiveWebhook;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ReceiveSupabaseMessageWebhookUseCase.Receipt receive(
      @RequestHeader(name = "X-Webhook-Secret", required = false) String suppliedSecret,
      @RequestBody JsonNode payload,
      HttpServletRequest request) {
    requireJson(request.getContentType());
    requireValidSecret(suppliedSecret);
    return receiveWebhook.receive(payload);
  }

  private void requireJson(String contentType) {
    try {
      if (contentType == null || !MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)) {
        throw new InvalidSupabaseMessageWebhookException("Content-Type must be application/json");
      }
    } catch (IllegalArgumentException exception) {
      throw new InvalidSupabaseMessageWebhookException("Invalid Content-Type");
    }
  }

  private void requireValidSecret(String suppliedSecret) {
    if (expectedSecret.length == 0
        || suppliedSecret == null
        || !MessageDigest.isEqual(expectedSecret, suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
      throw new InvalidSupabaseMessageWebhookUnauthorizedException();
    }
  }

  @ExceptionHandler(InvalidSupabaseMessageWebhookException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> invalidPayload(InvalidSupabaseMessageWebhookException exception) {
    log.warn("Supabase messages webhook rejected: {}", exception.getMessage());
    return Map.of("message", exception.getMessage());
  }

  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  static class InvalidSupabaseMessageWebhookUnauthorizedException extends RuntimeException {}
}
