package br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.AccountEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
  List<AccountEntity> findAllByOrderByNameAsc();

  @Query("select distinct a.itemId from AccountEntity a")
  List<UUID> findDistinctItemIds();
}
