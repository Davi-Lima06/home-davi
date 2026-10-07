package br.com.davi.homedavi.finance.application.port.in.integration;

import com.fasterxml.jackson.databind.JsonNode;

public interface ReceivePluggyWebhookUseCase {
  Receipt receive(JsonNode payload);

  record Receipt(boolean accepted, boolean duplicate) {}
}
