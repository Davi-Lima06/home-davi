package br.com.davi.homedavi.finance.adapter.in.integration.event;

import br.com.davi.homedavi.finance.application.port.in.integration.ProcessPluggyWebhookUseCase;
import br.com.davi.homedavi.finance.application.service.integration.PluggyWebhookStoredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Processa fora da thread HTTP para que o Pluggy receba o 202 sem esperar a API dele mesmo. */
@Component
public class PluggyWebhookStoredListener {
  private static final Logger log = LoggerFactory.getLogger(PluggyWebhookStoredListener.class);

  private final ProcessPluggyWebhookUseCase processWebhook;

  public PluggyWebhookStoredListener(ProcessPluggyWebhookUseCase processWebhook) {
    this.processWebhook = processWebhook;
  }

  @Async
  @EventListener
  public void onWebhookStored(PluggyWebhookStoredEvent event) {
    log.debug("Async processing started for Pluggy webhook: eventId={}", event.eventId());
    processWebhook.process(event.eventId());
  }
}
