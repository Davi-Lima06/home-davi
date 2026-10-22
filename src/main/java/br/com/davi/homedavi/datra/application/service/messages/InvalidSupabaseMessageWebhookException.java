package br.com.davi.homedavi.datra.application.service.messages;

public class InvalidSupabaseMessageWebhookException extends RuntimeException {
  public InvalidSupabaseMessageWebhookException(String message) {
    super(message);
  }
}
