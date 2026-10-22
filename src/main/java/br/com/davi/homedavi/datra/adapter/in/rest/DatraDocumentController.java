package br.com.davi.homedavi.datra.adapter.in.rest;

import br.com.davi.homedavi.datra.application.port.in.ApproveDatraDocumentUseCase;
import br.com.davi.homedavi.datra.application.port.in.SearchApprovedDatraKnowledgeUseCase;
import br.com.davi.homedavi.datra.application.port.in.SubmitDatraDocumentUseCase;
import br.com.davi.homedavi.datra.domain.DatraDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administrative API. Submitted documents remain invisible to MCP until explicitly approved. */
@RestController
@RequestMapping("/api/v1/datra/documents")
public class DatraDocumentController {
  private final SubmitDatraDocumentUseCase submit;
  private final ApproveDatraDocumentUseCase approve;
  private final SearchApprovedDatraKnowledgeUseCase approvedKnowledge;

  public DatraDocumentController(
      SubmitDatraDocumentUseCase submit,
      ApproveDatraDocumentUseCase approve,
      SearchApprovedDatraKnowledgeUseCase approvedKnowledge) {
    this.submit = submit;
    this.approve = approve;
    this.approvedKnowledge = approvedKnowledge;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DatraDocument submit(@Valid @RequestBody SubmitDocumentRequest request) {
    return submit.submit(
        new SubmitDatraDocumentUseCase.SubmitDatraDocumentCommand(
            request.title(), request.content(), request.sourceUrl(), request.sourceReference(), request.tags()));
  }

  @PostMapping("/{documentId}/approve")
  public DatraDocument approve(@PathVariable UUID documentId) {
    return approve.approve(documentId);
  }

  /** Inspection endpoint: never returns drafts or archived documents. */
  @GetMapping
  public List<DatraDocument> listApproved(@RequestParam(defaultValue = "10") int limit) {
    return approvedKnowledge.listApproved(limit);
  }

  public record SubmitDocumentRequest(
      @NotBlank String title,
      @NotBlank String content,
      String sourceUrl,
      String sourceReference,
      String tags) {}
}
