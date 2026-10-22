package br.com.davi.homedavi.datra.adapter.in.mcp;

import br.com.davi.homedavi.datra.application.port.in.SearchApprovedDatraKnowledgeUseCase;
import br.com.davi.homedavi.datra.domain.DatraDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Retrieval tools for Hermes' Datra profile. Only approved sources are reachable here. */
@Component
public class DatraMcpTools {
  private final SearchApprovedDatraKnowledgeUseCase knowledge;

  public DatraMcpTools(SearchApprovedDatraKnowledgeUseCase knowledge) {
    this.knowledge = knowledge;
  }

  @Tool(
      description =
          "Pesquisa a base de conhecimento aprovada da Datra. Use para responder sobre emissão de"
              + " notas fiscais via WhatsApp, DAS/MEI, produto e processos internos. Retorna apenas"
              + " documentos que foram aprovados por uma pessoa; cite o título e a fonte ao responder.")
  public List<DatraDocument> searchDatraKnowledge(
      @ToolParam(description = "Pergunta ou termos para pesquisar na base Datra") String query,
      @ToolParam(required = false, description = "Máximo de documentos, de 1 a 20; padrão 10")
          Integer limit) {
    return knowledge.search(query, limit == null ? 10 : limit);
  }

  @Tool(
      description =
          "Lê um documento específico já aprovado na base Datra. Não use documentos não aprovados"
              + " como fonte de informação.")
  public DatraDocument readDatraDocument(
      @ToolParam(description = "UUID retornado por searchDatraKnowledge") String documentId) {
    return knowledge.getApproved(UUID.fromString(documentId));
  }

  @Tool(
      description =
          "Lista os documentos aprovados mais recentemente na base de conhecimento Datra. Útil para"
              + " descobrir as fontes disponíveis antes de uma pergunta ampla.")
  public List<DatraDocument> listApprovedDatraDocuments(
      @ToolParam(required = false, description = "Máximo de documentos, de 1 a 20; padrão 10")
          Integer limit) {
    return knowledge.listApproved(limit == null ? 10 : limit);
  }
}
