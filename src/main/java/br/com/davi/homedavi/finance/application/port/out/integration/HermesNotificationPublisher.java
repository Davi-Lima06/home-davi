package br.com.davi.homedavi.finance.application.port.out.integration;

import br.com.davi.homedavi.finance.domain.integration.HermesOutboxMessage;

/** Porta HTTP para o receptor de webhooks do Hermes. */
public interface HermesNotificationPublisher {
  void publish(HermesOutboxMessage message);
}
