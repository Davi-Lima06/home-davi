package br.com.davi.homedavi.datra.application.port.in;

import br.com.davi.homedavi.datra.domain.DatraDocument;
import java.util.List;
import java.util.UUID;

public interface SearchApprovedDatraKnowledgeUseCase {
  List<DatraDocument> search(String query, int limit);

  DatraDocument getApproved(UUID documentId);

  List<DatraDocument> listApproved(int limit);
}
