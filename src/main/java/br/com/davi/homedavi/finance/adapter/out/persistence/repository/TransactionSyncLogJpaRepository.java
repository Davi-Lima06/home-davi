package br.com.davi.homedavi.finance.adapter.out.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.persistence.entity.TransactionSyncLogEntity;
import br.com.davi.homedavi.finance.adapter.out.persistence.entity.TransactionSyncStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionSyncLogJpaRepository
    extends JpaRepository<TransactionSyncLogEntity, Long> {
  List<TransactionSyncLogEntity> findFirstByOrderByStartedAtDesc(Pageable pageable);

  List<TransactionSyncLogEntity> findByStatusOrderByCompletedAtDesc(
      TransactionSyncStatus status, Pageable pageable);

  List<TransactionSyncLogEntity> findByStatusAndCompletedAtBeforeOrderByCompletedAtDesc(
      TransactionSyncStatus status, java.time.Instant completedAt, Pageable pageable);
}
