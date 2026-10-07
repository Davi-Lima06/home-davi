package br.com.davi.homedavi.finance.adapter.out.integration.persistence;

import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.TransactionSyncLogEntity;
import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.TransactionSyncStatus;
import br.com.davi.homedavi.finance.adapter.out.integration.persistence.repository.TransactionSyncLogJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.integration.TransactionSyncLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaTransactionSyncLog implements TransactionSyncLog {
  private final TransactionSyncLogJpaRepository syncLogRepository;

  public JpaTransactionSyncLog(TransactionSyncLogJpaRepository syncLogRepository) {
    this.syncLogRepository = syncLogRepository;
  }

  @Override
  @Transactional
  public long startSync(UUID webhookEventId, UUID itemId) {
    return syncLogRepository.save(new TransactionSyncLogEntity(webhookEventId, itemId)).getId();
  }

  @Override
  @Transactional
  public void completeSync(long syncId) {
    finish(syncId, TransactionSyncStatus.COMPLETED, null);
  }

  @Override
  @Transactional
  public void failSync(long syncId, String error) {
    finish(syncId, TransactionSyncStatus.FAILED, error);
  }

  private void finish(long syncId, TransactionSyncStatus status, String error) {
    var entity =
        syncLogRepository
            .findById(syncId)
            .orElseThrow(() -> new IllegalStateException("Sync log not found: " + syncId));
    entity.setStatus(status);
    entity.setCompletedAt(Instant.now());
    entity.setErrorMessage(error);
  }
}
