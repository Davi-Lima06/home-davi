package br.com.davi.homedavi.datra.adapter.out.persistence.repository;

import br.com.davi.homedavi.datra.adapter.out.persistence.entity.DatraDocumentEntity;
import br.com.davi.homedavi.datra.domain.DatraDocumentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatraDocumentJpaRepository extends JpaRepository<DatraDocumentEntity, UUID> {
  @Query(
      """
      select d from DatraDocumentEntity d
      where d.status = :status
        and (:query is null or lower(d.title) like lower(concat('%', :query, '%'))
             or lower(d.content) like lower(concat('%', :query, '%'))
             or lower(coalesce(d.tags, '')) like lower(concat('%', :query, '%')))
      order by d.approvedAt desc, d.updatedAt desc
      """)
  List<DatraDocumentEntity> findApproved(
      @Param("status") DatraDocumentStatus status, @Param("query") String query, Pageable pageable);
}
