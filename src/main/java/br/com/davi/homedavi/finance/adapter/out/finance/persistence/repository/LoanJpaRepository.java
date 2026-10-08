package br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.LoanEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanJpaRepository extends JpaRepository<LoanEntity, UUID> {}
