package br.com.davi.homedavi.datra.application.port.in;

import br.com.davi.homedavi.datra.domain.DatraDocument;

public interface SubmitDatraDocumentUseCase {
  DatraDocument submit(SubmitDatraDocumentCommand command);

  record SubmitDatraDocumentCommand(
      String title, String content, String sourceUrl, String sourceReference, String tags) {}
}
