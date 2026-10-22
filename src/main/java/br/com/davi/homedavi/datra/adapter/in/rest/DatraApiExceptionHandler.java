package br.com.davi.homedavi.datra.adapter.in.rest;

import br.com.davi.homedavi.datra.application.service.DatraDocumentNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = DatraDocumentController.class)
class DatraApiExceptionHandler {
  @ExceptionHandler(DatraDocumentNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  Map<String, String> documentNotFound(DatraDocumentNotFoundException exception) {
    return Map.of("message", exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> invalidRequest(IllegalArgumentException exception) {
    return Map.of("message", exception.getMessage());
  }
}
