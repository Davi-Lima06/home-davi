package br.com.davi.homedavi.finance.application.service;

import java.util.UUID;

/** Publicado depois que o webhook foi gravado (e commitado) no inbox. */
public record PluggyWebhookStoredEvent(UUID eventId) {}
