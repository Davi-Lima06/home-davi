package br.com.davi.homedavi.datra.application.port.out.messages;

import br.com.davi.homedavi.datra.domain.messages.DatraMessage;
import java.util.Optional;

public interface DatraMessageRepository {
  Optional<DatraMessage> findBySourceMessageId(long sourceMessageId);

  DatraMessage save(DatraMessage message);
}
