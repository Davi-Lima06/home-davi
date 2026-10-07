package br.com.davi.homedavi.finance.application.port.out.integration;

import br.com.davi.homedavi.finance.domain.integration.PluggyWebhookEvent;

/** Persists the inbox entry and its Hermes outbox message atomically. */
public interface PluggyWebhookInbox {
  boolean persist(PluggyWebhookEvent event);
}
