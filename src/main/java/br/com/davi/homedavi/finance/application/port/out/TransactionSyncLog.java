package br.com.davi.homedavi.finance.application.port.out;

import java.util.UUID;

public interface TransactionSyncLog {
  long startSync(UUID webhookEventId, UUID itemId);

  void completeSync(long syncId);

  void failSync(long syncId, String error);
}
