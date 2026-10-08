package br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.TransactionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PathVariable;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

  @Query("SELECT t FROM TransactionEntity t WHERE t.account.id  IN :ids")
  List<TransactionEntity> findAllByIds(@Param("ids") List<UUID> ids);

  List<TransactionEntity> findByUpdatedAtAfterAndUpdatedAtBeforeOrderByUpdatedAtAsc(
      java.time.Instant after, java.time.Instant before);

  List<TransactionEntity> findByUpdatedAtBeforeOrderByUpdatedAtAsc(java.time.Instant before);

  List<TransactionEntity> findByPendingTrueOrderByOccurredAtDesc();
}
