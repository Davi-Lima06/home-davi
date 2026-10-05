package br.com.davi.homedavi.finance.domain;

import java.util.UUID;

/** Mensagem persistida antes de qualquer chamada HTTP ao Hermes. */
public record HermesOutboxMessage(
    Long id, UUID webhookEventId, String pluggyEventType, String summaryJson, int attempts) {}
