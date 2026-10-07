package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.FinancialSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Leituras locais necessárias para montar um snapshot coerente da última sincronização. */
public interface FinancialSnapshotDataRepository {
  Optional<SyncInfo> findLatestSync();

  Optional<SyncInfo> findLatestSuccessfulSync();

  Optional<SyncInfo> findPreviousSuccessfulSync(Instant before);

  List<FinancialSnapshot.Transaction> findTransactionsSyncedBetween(Instant after, Instant through);

  List<FinancialSnapshot.Transaction> findPendingTransactions();

  record SyncInfo(String status, Instant completedAt, String errorMessage) {}
}
