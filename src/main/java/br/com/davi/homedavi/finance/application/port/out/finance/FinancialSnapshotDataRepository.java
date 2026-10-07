package br.com.davi.homedavi.finance.application.port.out.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Leituras locais necessárias para montar um snapshot coerente da última sincronização. */
public interface FinancialSnapshotDataRepository {
  /** itemIds já sincronizados no banco; fonte dos itens para investimentos e empréstimos. */
  List<UUID> findActiveItemIds();

  Optional<SyncInfo> findLatestSync();

  Optional<SyncInfo> findLatestSuccessfulSync();

  Optional<SyncInfo> findPreviousSuccessfulSync(Instant before);

  List<FinancialSnapshot.Transaction> findTransactionsSyncedBetween(Instant after, Instant through);

  List<FinancialSnapshot.Transaction> findPendingTransactions();

  record SyncInfo(String status, Instant completedAt, String errorMessage) {}
}
