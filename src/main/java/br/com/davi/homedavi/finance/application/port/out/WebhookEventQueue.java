package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.PluggyWebhookEvent;
import java.util.Optional;
import java.util.UUID;

/** Leitura e baixa dos eventos do inbox que ainda não foram refletidos no schema finance. */
public interface WebhookEventQueue {
  /**
   * Passa o evento de RECEIVED/FAILED para PROCESSING; vazio se ele não está pendente ou outro
   * processo já o pegou.
   */
  Optional<PluggyWebhookEvent> claimPendingEvent(UUID eventId);

  void markProcessed(UUID eventId);

  void markFailed(UUID eventId, String error);
}
