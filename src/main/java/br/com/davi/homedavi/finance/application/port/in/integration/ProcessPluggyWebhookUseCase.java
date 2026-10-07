package br.com.davi.homedavi.finance.application.port.in.integration;

import java.util.UUID;

public interface ProcessPluggyWebhookUseCase {
  void process(UUID eventId);
}
