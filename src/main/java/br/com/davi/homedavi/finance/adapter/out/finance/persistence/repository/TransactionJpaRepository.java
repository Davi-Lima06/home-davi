package br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.TransactionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
  List<TransactionEntity> findByAccountIdOrderByOccurredAtDesc(UUID accountId);

  List<TransactionEntity> findByUpdatedAtAfterAndUpdatedAtBeforeOrderByUpdatedAtAsc(
      java.time.Instant after, java.time.Instant before);

  List<TransactionEntity> findByUpdatedAtBeforeOrderByUpdatedAtAsc(java.time.Instant before);

  List<TransactionEntity> findByPendingTrueOrderByOccurredAtDesc();
}
