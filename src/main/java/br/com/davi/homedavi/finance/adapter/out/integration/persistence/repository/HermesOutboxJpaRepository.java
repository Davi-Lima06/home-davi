package br.com.davi.homedavi.finance.adapter.out.integration.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.HermesOutboxEntity;
import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.HermesOutboxStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HermesOutboxJpaRepository extends JpaRepository<HermesOutboxEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select o from HermesOutboxEntity o where o.status in :statuses and o.availableAt <= :now"
          + " order by o.createdAt asc")
  List<HermesOutboxEntity> findReadyForDispatch(
      @Param("now") Instant now,
      @Param("statuses") List<HermesOutboxStatus> statuses,
      Pageable pageable);

  default List<HermesOutboxEntity> findReadyForDispatch(Instant now, Pageable pageable) {
    return findReadyForDispatch(
        now,
        List.of(HermesOutboxStatus.PENDING, HermesOutboxStatus.FAILED, HermesOutboxStatus.SENDING),
        pageable);
  }
}
