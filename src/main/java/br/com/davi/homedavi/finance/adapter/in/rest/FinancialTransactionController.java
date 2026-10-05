package br.com.davi.homedavi.finance.adapter.in.rest;

import br.com.davi.homedavi.finance.application.port.in.ListFinancialTransactionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.RegisterFinancialTransactionUseCase;
import br.com.davi.homedavi.finance.domain.FinancialTransaction;
import br.com.davi.homedavi.finance.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/financial-control/transactions")
public class FinancialTransactionController {
  private final RegisterFinancialTransactionUseCase register;
  private final ListFinancialTransactionsUseCase list;

  public FinancialTransactionController(
      RegisterFinancialTransactionUseCase register, ListFinancialTransactionsUseCase list) {
    this.register = register;
    this.list = list;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FinancialTransaction create(@Valid @RequestBody CreateTransactionRequest request) {
    return register.register(
        new RegisterFinancialTransactionUseCase.RegisterFinancialTransactionCommand(
            request.accountId(),
            request.description(),
            request.amount(),
            request.currency(),
            request.type(),
            request.occurredAt(),
            request.externalId()));
  }

  @GetMapping
  public List<FinancialTransaction> list(@RequestParam String accountId) {
    return list.listByAccount(accountId);
  }

  public record CreateTransactionRequest(
      @NotBlank String accountId,
      @NotBlank String description,
      @NotNull @Positive BigDecimal amount,
      String currency,
      @NotNull TransactionType type,
      Instant occurredAt,
      String externalId) {}
}
