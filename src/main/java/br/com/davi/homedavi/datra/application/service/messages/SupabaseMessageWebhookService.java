package br.com.davi.homedavi.datra.application.service.messages;

import br.com.davi.homedavi.datra.application.port.in.messages.ReceiveSupabaseMessageWebhookUseCase;
import br.com.davi.homedavi.datra.application.port.out.messages.DatraMessageRepository;
import br.com.davi.homedavi.datra.domain.messages.DatraMessage;
import br.com.davi.homedavi.datra.domain.messages.MessageDirection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates the Supabase Database Webhook envelope before it reaches the local database. */
@Service
@Transactional
public class SupabaseMessageWebhookService implements ReceiveSupabaseMessageWebhookUseCase {
  private static final String EXPECTED_SCHEMA = "public";
  private static final String EXPECTED_TABLE = "logs_mensagens";
  private static final int MAX_CONTENT_LENGTH = 10_000;
  private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

  private final DatraMessageRepository repository;
  private final ObjectMapper objectMapper;

  public SupabaseMessageWebhookService(DatraMessageRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  public Receipt receive(JsonNode payload) {
    requireObject(payload, "payload");
    String eventType = requiredText(payload, "type").toUpperCase(Locale.ROOT);
    if (!eventType.equals("INSERT") && !eventType.equals("UPDATE")) {
      throw new InvalidSupabaseMessageWebhookException("Only INSERT and UPDATE events are accepted");
    }
    requireEquals(payload, "schema", EXPECTED_SCHEMA);
    requireEquals(payload, "table", EXPECTED_TABLE);

    JsonNode record = payload.get("record");
    requireObject(record, "record");
    long sourceMessageId = requiredPositiveLong(record, "id");
    String phone = requiredText(record, "telefone");
    if (!PHONE_PATTERN.matcher(phone).matches()) {
      throw new InvalidSupabaseMessageWebhookException("telefone must contain 10 to 15 digits");
    }
    String content = requiredText(record, "conteudo");
    if (content.length() > MAX_CONTENT_LENGTH) {
      throw new InvalidSupabaseMessageWebhookException("conteudo exceeds 10000 characters");
    }
    MessageDirection direction = requiredDirection(record);
    Instant occurredAt = parseTimestamp(requiredText(record, "data_hora"));
    Instant now = Instant.now();
    boolean created = repository.findBySourceMessageId(sourceMessageId).isEmpty();

    repository.save(
        new DatraMessage(
            sourceMessageId,
            phone,
            direction,
            content,
            occurredAt,
            eventType,
            json(payload),
            now,
            now));
    return new Receipt(sourceMessageId, created);
  }

  private static void requireEquals(JsonNode payload, String field, String expected) {
    if (!expected.equals(requiredText(payload, field))) {
      throw new InvalidSupabaseMessageWebhookException(field + " must be " + expected);
    }
  }

  private static void requireObject(JsonNode node, String field) {
    if (node == null || !node.isObject()) {
      throw new InvalidSupabaseMessageWebhookException(field + " must be a JSON object");
    }
  }

  private static long requiredPositiveLong(JsonNode record, String field) {
    JsonNode value = record.get(field);
    if (value == null || !value.canConvertToLong() || value.asLong() <= 0) {
      throw new InvalidSupabaseMessageWebhookException(field + " must be a positive integer");
    }
    return value.asLong();
  }

  private static String requiredText(JsonNode record, String field) {
    JsonNode value = record.get(field);
    if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
      throw new InvalidSupabaseMessageWebhookException(field + " is required");
    }
    return value.asText().trim();
  }

  private static MessageDirection requiredDirection(JsonNode record) {
    try {
      return MessageDirection.valueOf(requiredText(record, "direcao").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new InvalidSupabaseMessageWebhookException("direcao must be ENTRADA or SAIDA");
    }
  }

  private static Instant parseTimestamp(String value) {
    String normalized = value.replace(' ', 'T');
    if (normalized.matches(".*[+-][0-9]{2}$")) {
      normalized += ":00";
    }
    try {
      return OffsetDateTime.parse(normalized).toInstant();
    } catch (RuntimeException exception) {
      throw new InvalidSupabaseMessageWebhookException("data_hora must include a valid UTC offset");
    }
  }

  private String json(JsonNode payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new InvalidSupabaseMessageWebhookException("Could not serialize webhook payload");
    }
  }
}
