package br.com.davi.homedavi.finance.adapter.out.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.persistence.entity.TransactionSyncLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionSyncLogJpaRepository
    extends JpaRepository<TransactionSyncLogEntity, Long> {}
