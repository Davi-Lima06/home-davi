package br.com.davi.homedavi.finance.application.service;

public class InvalidPluggyWebhookException extends RuntimeException {
  public InvalidPluggyWebhookException(String message) {
    super(message);
  }
}
