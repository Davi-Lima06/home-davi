package br.com.davi.homedavi.finance.adapter.out.integration.hermes;

import br.com.davi.homedavi.finance.application.port.out.integration.HermesNotificationPublisher;
import br.com.davi.homedavi.finance.domain.integration.HermesOutboxMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Cliente do contrato de webhook genérico do Hermes (HMAC-SHA256 v2). */
@Component
@ConditionalOnProperty(prefix = "hermes.webhook", name = "enabled", havingValue = "true")
public class HttpHermesNotificationPublisher implements HermesNotificationPublisher {
  private final URI endpoint;
  private final String secret;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public HttpHermesNotificationPublisher(
      @Value("${hermes.webhook.url}") String url,
      @Value("${hermes.webhook.secret}") String secret,
      ObjectMapper objectMapper) {
    if (url == null || url.isBlank())
      throw new IllegalStateException(
          "HERMES_WEBHOOK_URL is required when Hermes delivery is enabled");
    if (secret == null || secret.isBlank())
      throw new IllegalStateException(
          "HERMES_WEBHOOK_SECRET is required when Hermes delivery is enabled");
    this.endpoint = URI.create(url);
    this.secret = secret;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @Override
  public void publish(HermesOutboxMessage message) {
    long timestamp = Instant.now().getEpochSecond();
    byte[] body = envelope(message).getBytes(StandardCharsets.UTF_8);
    HttpRequest request =
        HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("X-Webhook-Timestamp", Long.toString(timestamp))
            .header(
                "X-Webhook-Signature-V2",
                hmacSha256(timestamp + "." + new String(body, StandardCharsets.UTF_8)))
            // Mantém o mesmo ID nos retries: o Hermes descarta uma entrega já aceita.
            .header("X-Request-ID", message.webhookEventId().toString())
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    try {
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Hermes returned HTTP " + response.statusCode());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while delivering summary to Hermes", exception);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Could not deliver summary to Hermes", exception);
    }
  }

  private String envelope(HermesOutboxMessage message) {
    try {
      ObjectNode root = objectMapper.createObjectNode();
      root.put("type", "financial.sync.completed");
      root.put("source", "home-davi");
      root.put("eventId", message.webhookEventId().toString());
      root.put("outboxId", message.id());
      root.put("pluggyEvent", message.pluggyEventType());
      JsonNode summary = objectMapper.readTree(message.summaryJson());
      root.set("summary", summary);
      return objectMapper.writeValueAsString(root);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize Hermes notification", exception);
    }
  }

  private String hmacSha256(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return java.util.HexFormat.of()
          .formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Could not sign Hermes webhook", exception);
    }
  }
}
