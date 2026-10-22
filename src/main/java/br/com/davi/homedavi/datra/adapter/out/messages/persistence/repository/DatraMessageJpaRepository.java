package br.com.davi.homedavi.datra.adapter.out.messages.persistence.repository;

import br.com.davi.homedavi.datra.adapter.out.messages.persistence.entity.DatraMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatraMessageJpaRepository extends JpaRepository<DatraMessageEntity, Long> {}
