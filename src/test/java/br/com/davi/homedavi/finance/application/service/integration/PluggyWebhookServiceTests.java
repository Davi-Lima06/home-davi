package br.com.davi.homedavi.finance.application.service.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.davi.homedavi.finance.application.port.out.integration.PluggyWebhookInbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PluggyWebhookServiceTests {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void persistsNewEventAndMarksRepeatedEventAsDuplicate() throws Exception {
    PluggyWebhookInbox inbox = event -> false;
    var service = new PluggyWebhookService(inbox, mapper, published -> {});
    var payload =
        mapper.readTree(
            """
            {"event":"item/updated","eventId":"9a60b622-4b3b-4509-a836-79e65d36aef6",
             "itemId":"e5fc4528-471a-4b31-bdc2-8665b174b377","clientId":"76bf3bf6-b579-478b-b45a-47f9ca6629f7",
             "clientUserId":null,"triggeredBy":"USER"}
            """);
    var receipt = service.receive(payload);
    assertFalse(receipt.accepted());
    assertTrue(receipt.duplicate());
  }
}
