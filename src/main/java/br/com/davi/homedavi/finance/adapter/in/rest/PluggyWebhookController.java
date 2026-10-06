package br.com.davi.homedavi.finance.adapter.in.rest;

import br.com.davi.homedavi.finance.application.port.in.ReceivePluggyWebhookUseCase;
import br.com.davi.homedavi.finance.application.service.InvalidPluggyWebhookException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Boundary for Pluggy events. Translation to financial commands belongs here, never in the domain.
 */
@RestController
@RequestMapping("/api/v1/webhooks/pluggy")
public class PluggyWebhookController {
  private static final Logger log = LoggerFactory.getLogger(PluggyWebhookController.class);

  private final String webhookSecret;
  private final ReceivePluggyWebhookUseCase receiveWebhook;

  public PluggyWebhookController(
      @Value("${pluggy.webhook.secret}") String webhookSecret,
      ReceivePluggyWebhookUseCase receiveWebhook) {
    this.webhookSecret = webhookSecret;
    this.receiveWebhook = receiveWebhook;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void receive(
          @RequestHeader(name = "X-Webhook-Secret", required = false) String suppliedSecret,
          @RequestBody JsonNode payload) {
    if (suppliedSecret == null
            || !MessageDigest.isEqual(
            webhookSecret.getBytes(StandardCharsets.UTF_8),
            suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
      throw new InvalidWebhookSecretException();
    }
    receiveWebhook.receive(payload);
  }

  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  static class InvalidWebhookSecretException extends RuntimeException {}

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(InvalidPluggyWebhookException.class)
  void invalidPayload(InvalidPluggyWebhookException exception) {
    log.warn("[1/7] Pluggy webhook rejected: {}", exception.getMessage());
  }
}
