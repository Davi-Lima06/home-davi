package br.com.davi.homedavi.datra.application.port.in.messages;

import com.fasterxml.jackson.databind.JsonNode;

public interface ReceiveSupabaseMessageWebhookUseCase {
  Receipt receive(JsonNode payload);

  record Receipt(long sourceMessageId, boolean created) {}
}
