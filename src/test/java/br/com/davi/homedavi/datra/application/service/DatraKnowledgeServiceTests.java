package br.com.davi.homedavi.datra.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.davi.homedavi.datra.application.port.in.SubmitDatraDocumentUseCase.SubmitDatraDocumentCommand;
import br.com.davi.homedavi.datra.application.port.out.DatraDocumentRepository;
import br.com.davi.homedavi.datra.domain.DatraDocument;
import br.com.davi.homedavi.datra.domain.DatraDocumentStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DatraKnowledgeServiceTests {
  @Test
  void submittedDocumentIsInvisibleUntilApproved() {
    var repository = new InMemoryDatraDocumentRepository();
    var service = new DatraKnowledgeService(repository);

    DatraDocument draft =
        service.submit(new SubmitDatraDocumentCommand("DAS MEI", "Conteúdo aprovado depois", null, null, "mei,das"));

    assertEquals(DatraDocumentStatus.PENDING_APPROVAL, draft.status());
    assertEquals(List.of(), service.search("DAS", 10));
    assertThrows(DatraDocumentNotFoundException.class, () -> service.getApproved(draft.id()));
  }

  @Test
  void approvedDocumentCanBeRetrieved() {
    var repository = new InMemoryDatraDocumentRepository();
    var service = new DatraKnowledgeService(repository);
    DatraDocument draft =
        service.submit(new SubmitDatraDocumentCommand("Emissão no WhatsApp", "Procedimento oficial", null, "manual", "nota fiscal"));

    DatraDocument approved = service.approve(draft.id());

    assertEquals(DatraDocumentStatus.APPROVED, approved.status());
    assertEquals(List.of(approved), service.search("WhatsApp", 10));
    assertEquals(approved, service.getApproved(approved.id()));
  }

  private static class InMemoryDatraDocumentRepository implements DatraDocumentRepository {
    private final List<DatraDocument> documents = new ArrayList<>();

    @Override
    public DatraDocument save(DatraDocument document) {
      documents.removeIf(existing -> existing.id().equals(document.id()));
      documents.add(document);
      return document;
    }

    @Override
    public Optional<DatraDocument> findById(UUID documentId) {
      return documents.stream().filter(document -> document.id().equals(documentId)).findFirst();
    }

    @Override
    public List<DatraDocument> findApproved(String query, int limit) {
      String normalizedQuery = query == null ? null : query.toLowerCase();
      return documents.stream()
          .filter(document -> document.status() == DatraDocumentStatus.APPROVED)
          .filter(
              document ->
                  normalizedQuery == null
                      || (document.title() + " " + document.content() + " " + document.tags())
                          .toLowerCase()
                          .contains(normalizedQuery))
          .sorted(Comparator.comparing(DatraDocument::approvedAt).reversed())
          .limit(limit)
          .toList();
    }
  }
}
