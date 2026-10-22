package br.com.davi.homedavi.datra.application.service.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.davi.homedavi.datra.application.port.out.messages.DatraMessageRepository;
import br.com.davi.homedavi.datra.domain.messages.DatraMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SupabaseMessageWebhookServiceTests {
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void storesAnInsertUsingTheSupabaseMessageIdAsIdempotencyKey() throws Exception {
    var repository = new InMemoryDatraMessageRepository();
    var service = new SupabaseMessageWebhookService(repository, objectMapper);

    var receipt = service.receive(validPayload());

    assertEquals(2L, receipt.sourceMessageId());
    assertEquals(true, receipt.created());
    assertEquals("556192300340", repository.messages.get(2L).phone());
    assertEquals("2026-07-31T18:32:01.866046Z", repository.messages.get(2L).occurredAt().toString());
  }

  @Test
  void rejectsEventsForAnotherTableAndInvalidPhone() throws Exception {
    var service = new SupabaseMessageWebhookService(new InMemoryDatraMessageRepository(), objectMapper);
    var wrongTable = validPayload();
    ((com.fasterxml.jackson.databind.node.ObjectNode) wrongTable).put("table", "other_table");

    assertThrows(InvalidSupabaseMessageWebhookException.class, () -> service.receive(wrongTable));

    var invalidPhone = validPayload();
    ((com.fasterxml.jackson.databind.node.ObjectNode) invalidPhone.path("record")).put("telefone", "+556192300340");
    assertThrows(InvalidSupabaseMessageWebhookException.class, () -> service.receive(invalidPhone));
  }

  private com.fasterxml.jackson.databind.JsonNode validPayload() throws Exception {
    return objectMapper.readTree(
        """
        {
          "type":"INSERT", "table":"logs_mensagens", "schema":"public",
          "record":{"id":2,"telefone":"556192300340","direcao":"SAIDA",
          "conteudo":"Mensagem de teste","data_hora":"2026-07-31 18:32:01.866046+00"},
          "old_record":null
        }
        """);
  }

  private static class InMemoryDatraMessageRepository implements DatraMessageRepository {
    private final Map<Long, DatraMessage> messages = new HashMap<>();

    @Override
    public Optional<DatraMessage> findBySourceMessageId(long sourceMessageId) {
      return Optional.ofNullable(messages.get(sourceMessageId));
    }

    @Override
    public DatraMessage save(DatraMessage message) {
      messages.put(message.sourceMessageId(), message);
      return message;
    }
  }
}
