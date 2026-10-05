package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.HermesOutboxMessage;

/** Porta HTTP para o receptor de webhooks do Hermes. */
public interface HermesNotificationPublisher {
  void publish(HermesOutboxMessage message);
}
