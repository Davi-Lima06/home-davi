package br.com.davi.homedavi.datra.application.port.out;

import br.com.davi.homedavi.datra.domain.DatraDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatraDocumentRepository {
  DatraDocument save(DatraDocument document);

  Optional<DatraDocument> findById(UUID documentId);

  List<DatraDocument> findApproved(String query, int limit);
}
