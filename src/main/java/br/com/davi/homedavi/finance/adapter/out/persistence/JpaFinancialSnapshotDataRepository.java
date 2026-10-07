package br.com.davi.homedavi.finance.adapter.out.persistence;

import br.com.davi.homedavi.finance.adapter.out.persistence.entity.TransactionEntity;
import br.com.davi.homedavi.finance.adapter.out.persistence.repository.TransactionJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.persistence.repository.TransactionSyncLogJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.FinancialSnapshotDataRepository;
import br.com.davi.homedavi.finance.domain.FinancialSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaFinancialSnapshotDataRepository implements FinancialSnapshotDataRepository {
  private final TransactionSyncLogJpaRepository syncLogs;
  private final TransactionJpaRepository transactions;

  public JpaFinancialSnapshotDataRepository(
      TransactionSyncLogJpaRepository syncLogs, TransactionJpaRepository transactions) {
    this.syncLogs = syncLogs;
    this.transactions = transactions;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SyncInfo> findLatestSync() {
    return syncLogs.findFirstByOrderByStartedAtDesc(PageRequest.of(0, 1)).stream()
        .findFirst()
        .map(
            sync ->
                new SyncInfo(
                    sync.getStatus().name(),
                    sync.getCompletedAt(),
                    sync.getErrorMessage()));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SyncInfo> findLatestSuccessfulSync() {
    return syncLogs
        .findByStatusOrderByCompletedAtDesc(
            br.com.davi.homedavi.finance.adapter.out.persistence.entity.TransactionSyncStatus
                .COMPLETED,
            PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .map(sync -> new SyncInfo(sync.getStatus().name(), sync.getCompletedAt(), null));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SyncInfo> findPreviousSuccessfulSync(Instant before) {
    return syncLogs
        .findByStatusAndCompletedAtBeforeOrderByCompletedAtDesc(
            br.com.davi.homedavi.finance.adapter.out.persistence.entity.TransactionSyncStatus
                .COMPLETED,
            before,
            PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .map(sync -> new SyncInfo(sync.getStatus().name(), sync.getCompletedAt(), null));
  }

  @Override
  @Transactional(readOnly = true)
  public List<FinancialSnapshot.Transaction> findTransactionsSyncedBetween(
      Instant after, Instant through) {
    var found =
        after == null
            ? transactions.findByUpdatedAtBeforeOrderByUpdatedAtAsc(through)
            : transactions.findByUpdatedAtAfterAndUpdatedAtBeforeOrderByUpdatedAtAsc(after, through);
    return found.stream()
        .map(JpaFinancialSnapshotDataRepository::toSnapshotTransaction)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<FinancialSnapshot.Transaction> findPendingTransactions() {
    return transactions.findByPendingTrueOrderByOccurredAtDesc().stream()
        .map(JpaFinancialSnapshotDataRepository::toSnapshotTransaction)
        .toList();
  }

  private static FinancialSnapshot.Transaction toSnapshotTransaction(TransactionEntity entity) {
    return new FinancialSnapshot.Transaction(
        entity.getId(),
        entity.getAccount().getId(),
        entity.getDescription(),
        entity.getAmount(),
        entity.getCurrencyCode(),
        entity.getTransactionType().name(),
        entity.getStatus(),
        entity.getOccurredAt(),
        entity.getUpdatedAt());
  }
}
