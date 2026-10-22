package br.com.davi.homedavi.datra.application.service;

import br.com.davi.homedavi.datra.application.port.in.ApproveDatraDocumentUseCase;
import br.com.davi.homedavi.datra.application.port.in.SearchApprovedDatraKnowledgeUseCase;
import br.com.davi.homedavi.datra.application.port.in.SubmitDatraDocumentUseCase;
import br.com.davi.homedavi.datra.application.port.out.DatraDocumentRepository;
import br.com.davi.homedavi.datra.domain.DatraDocument;
import br.com.davi.homedavi.datra.domain.DatraDocumentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps unapproved material out of every retrieval path exposed to the AI. */
@Service
@Transactional
public class DatraKnowledgeService
    implements SubmitDatraDocumentUseCase, ApproveDatraDocumentUseCase, SearchApprovedDatraKnowledgeUseCase {
  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 20;

  private final DatraDocumentRepository repository;

  public DatraKnowledgeService(DatraDocumentRepository repository) {
    this.repository = repository;
  }

  @Override
  public DatraDocument submit(SubmitDatraDocumentCommand command) {
    Instant now = Instant.now();
    return repository.save(
        new DatraDocument(
            UUID.randomUUID(),
            command.title().trim(),
            command.content().trim(),
            blankToNull(command.sourceUrl()),
            blankToNull(command.sourceReference()),
            blankToNull(command.tags()),
            DatraDocumentStatus.PENDING_APPROVAL,
            null,
            now,
            now));
  }

  @Override
  public DatraDocument approve(UUID documentId) {
    DatraDocument document =
        repository.findById(documentId).orElseThrow(() -> new DatraDocumentNotFoundException(documentId));
    if (document.status() == DatraDocumentStatus.ARCHIVED) {
      throw new IllegalStateException("Documentos arquivados não podem ser aprovados");
    }
    if (document.status() == DatraDocumentStatus.APPROVED) {
      return document;
    }
    Instant now = Instant.now();
    return repository.save(
        new DatraDocument(
            document.id(),
            document.title(),
            document.content(),
            document.sourceUrl(),
            document.sourceReference(),
            document.tags(),
            DatraDocumentStatus.APPROVED,
            now,
            document.createdAt(),
            now));
  }

  @Override
  @Transactional(readOnly = true)
  public List<DatraDocument> search(String query, int limit) {
    return repository.findApproved(requireText(query, "query"), normalizeLimit(limit));
  }

  @Override
  @Transactional(readOnly = true)
  public DatraDocument getApproved(UUID documentId) {
    DatraDocument document =
        repository.findById(documentId).orElseThrow(() -> new DatraDocumentNotFoundException(documentId));
    if (document.status() != DatraDocumentStatus.APPROVED) {
      throw new DatraDocumentNotFoundException(documentId);
    }
    return document;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DatraDocument> listApproved(int limit) {
    return repository.findApproved(null, normalizeLimit(limit));
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static int normalizeLimit(int limit) {
    return limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
  }
}
