package br.com.davi.homedavi.finance.application.service.integration;

import br.com.davi.homedavi.finance.application.port.in.integration.ReceivePluggyWebhookUseCase;
import br.com.davi.homedavi.finance.application.port.out.integration.PluggyWebhookInbox;
import br.com.davi.homedavi.finance.domain.integration.PluggyWebhookEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class PluggyWebhookService implements ReceivePluggyWebhookUseCase {
  private static final Logger log = LoggerFactory.getLogger(PluggyWebhookService.class);

  private final PluggyWebhookInbox inbox;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher events;

  public PluggyWebhookService(
      PluggyWebhookInbox inbox, ObjectMapper objectMapper, ApplicationEventPublisher events) {
    this.inbox = inbox;
    this.objectMapper = objectMapper;
    this.events = events;
  }

  @Override
  public Receipt receive(JsonNode payload) {
    var event =
        new PluggyWebhookEvent(
            requiredUuid(payload, "eventId"),
            requiredText(payload, "event"),
            optionalUuid(payload, "itemId"),
            optionalUuid(payload, "accountId"),
            optionalUuid(payload, "clientId"),
            optionalText(payload, "clientUserId"),
            optionalText(payload, "triggeredBy"),
            json(payload.path("transactionIds")),
            json(payload));
    log.info(
        "[1/7] Pluggy webhook received: eventId={}, type={}, itemId={}, accountId={}",
        event.eventId(),
        event.eventType(),
        event.itemId(),
        event.accountId());
    boolean inserted = inbox.persist(event);
    if (inserted) {
      log.info("[1/7] Pluggy webhook stored in inbox: eventId={}", event.eventId());
      events.publishEvent(new PluggyWebhookStoredEvent(event.eventId()));
    } else {
      log.info(
          "[1/7] Pluggy webhook already stored, ignoring duplicate: eventId={}", event.eventId());
    }
    return new Receipt(inserted, !inserted);
  }

  private UUID requiredUuid(JsonNode payload, String field) {
    return UUID.fromString(requiredText(payload, field));
  }

  private UUID optionalUuid(JsonNode payload, String field) {
    String value = optionalText(payload, field);
    return value == null ? null : UUID.fromString(value);
  }

  private String requiredText(JsonNode payload, String field) {
    String value = optionalText(payload, field);
    if (value == null) throw new InvalidPluggyWebhookException("Missing required field: " + field);
    return value;
  }

  private String optionalText(JsonNode payload, String field) {
    JsonNode value = payload.get(field);
    return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
  }

  private String json(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(
          node.isMissingNode() ? objectMapper.createArrayNode() : node);
    } catch (JsonProcessingException exception) {
      throw new InvalidPluggyWebhookException("Could not serialize payload");
    }
  }
}
