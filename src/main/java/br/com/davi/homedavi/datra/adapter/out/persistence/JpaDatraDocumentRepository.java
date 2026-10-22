package br.com.davi.homedavi.datra.adapter.out.persistence;

import br.com.davi.homedavi.datra.adapter.out.persistence.entity.DatraDocumentEntity;
import br.com.davi.homedavi.datra.adapter.out.persistence.repository.DatraDocumentJpaRepository;
import br.com.davi.homedavi.datra.application.port.out.DatraDocumentRepository;
import br.com.davi.homedavi.datra.domain.DatraDocument;
import br.com.davi.homedavi.datra.domain.DatraDocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDatraDocumentRepository implements DatraDocumentRepository {
  private final DatraDocumentJpaRepository repository;

  public JpaDatraDocumentRepository(DatraDocumentJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public DatraDocument save(DatraDocument document) {
    return toDomain(repository.save(toEntity(document)));
  }

  @Override
  public Optional<DatraDocument> findById(UUID documentId) {
    return repository.findById(documentId).map(JpaDatraDocumentRepository::toDomain);
  }

  @Override
  public List<DatraDocument> findApproved(String query, int limit) {
    return repository.findApproved(DatraDocumentStatus.APPROVED, query, PageRequest.of(0, limit)).stream()
        .map(JpaDatraDocumentRepository::toDomain)
        .toList();
  }

  private static DatraDocumentEntity toEntity(DatraDocument document) {
    return new DatraDocumentEntity(
        document.id(),
        document.title(),
        document.content(),
        document.sourceUrl(),
        document.sourceReference(),
        document.tags(),
        document.status(),
        document.approvedAt(),
        document.createdAt(),
        document.updatedAt());
  }

  private static DatraDocument toDomain(DatraDocumentEntity entity) {
    return new DatraDocument(
        entity.getId(),
        entity.getTitle(),
        entity.getContent(),
        entity.getSourceUrl(),
        entity.getSourceReference(),
        entity.getTags(),
        entity.getStatus(),
        entity.getApprovedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
