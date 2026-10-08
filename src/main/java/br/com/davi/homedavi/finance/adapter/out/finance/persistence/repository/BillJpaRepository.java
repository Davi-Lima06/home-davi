package br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.BillEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillJpaRepository extends JpaRepository<BillEntity, UUID> {}
