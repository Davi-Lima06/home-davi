package br.com.davi.homedavi.datra.application.service;

import java.util.UUID;

public class DatraDocumentNotFoundException extends RuntimeException {
  public DatraDocumentNotFoundException(UUID documentId) {
    super("Documento Datra não encontrado ou ainda não aprovado: " + documentId);
  }
}
