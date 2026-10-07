package br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.CategoryEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.CategoryKind;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {
  Optional<CategoryEntity> findByNameAndKind(String name, CategoryKind kind);
}
