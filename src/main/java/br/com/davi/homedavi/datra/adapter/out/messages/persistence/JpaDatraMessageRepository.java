package br.com.davi.homedavi.datra.adapter.out.messages.persistence;

import br.com.davi.homedavi.datra.adapter.out.messages.persistence.entity.DatraMessageEntity;
import br.com.davi.homedavi.datra.adapter.out.messages.persistence.repository.DatraMessageJpaRepository;
import br.com.davi.homedavi.datra.application.port.out.messages.DatraMessageRepository;
import br.com.davi.homedavi.datra.domain.messages.DatraMessage;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDatraMessageRepository implements DatraMessageRepository {
  private final DatraMessageJpaRepository repository;

  public JpaDatraMessageRepository(DatraMessageJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<DatraMessage> findBySourceMessageId(long sourceMessageId) {
    return repository.findById(sourceMessageId).map(JpaDatraMessageRepository::toDomain);
  }

  @Override
  public DatraMessage save(DatraMessage message) {
    return toDomain(repository.save(toEntity(message)));
  }

  private static DatraMessageEntity toEntity(DatraMessage message) {
    return new DatraMessageEntity(
        message.sourceMessageId(),
        message.phone(),
        message.direction(),
        message.content(),
        message.occurredAt(),
        message.sourceEventType(),
        message.rawPayload(),
        message.receivedAt(),
        message.updatedAt());
  }

  private static DatraMessage toDomain(DatraMessageEntity entity) {
    return new DatraMessage(
        entity.getSourceMessageId(),
        entity.getPhone(),
        entity.getDirection(),
        entity.getContent(),
        entity.getOccurredAt(),
        entity.getSourceEventType(),
        entity.getRawPayload(),
        entity.getReceivedAt(),
        entity.getUpdatedAt());
  }
}
