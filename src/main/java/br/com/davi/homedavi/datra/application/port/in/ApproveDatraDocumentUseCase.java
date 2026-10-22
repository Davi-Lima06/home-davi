package br.com.davi.homedavi.datra.application.port.in;

import br.com.davi.homedavi.datra.domain.DatraDocument;
import java.util.UUID;

public interface ApproveDatraDocumentUseCase {
  DatraDocument approve(UUID documentId);
}
