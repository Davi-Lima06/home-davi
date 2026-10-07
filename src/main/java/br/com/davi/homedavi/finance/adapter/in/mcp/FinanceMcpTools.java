package br.com.davi.homedavi.finance.adapter.in.mcp;

import br.com.davi.homedavi.finance.application.port.in.GetAccountBalanceUseCase;
import br.com.davi.homedavi.finance.application.port.in.GetFinancialSnapshotUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListFinancialAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListFinancialTransactionsUseCase;
import br.com.davi.homedavi.finance.domain.AccountBalance;
import br.com.davi.homedavi.finance.domain.FinancialAccount;
import br.com.davi.homedavi.finance.domain.FinancialTransaction;
import br.com.davi.homedavi.finance.domain.FinancialSnapshot;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Ferramentas MCP do tópico finance. Cada tópico expõe suas próprias tools por um
 * ToolCallbackProvider.
 */
@Component
public class FinanceMcpTools {
  private static final Logger log = LoggerFactory.getLogger(FinanceMcpTools.class);

  private final ListFinancialTransactionsUseCase listTransactions;
  private final GetAccountBalanceUseCase getAccountBalance;
  private final ListFinancialAccountsUseCase listAccounts;
  private final GetFinancialSnapshotUseCase getFinancialSnapshot;

  public FinanceMcpTools(
      ListFinancialTransactionsUseCase listTransactions,
      GetAccountBalanceUseCase getAccountBalance,
      ListFinancialAccountsUseCase listAccounts,
      GetFinancialSnapshotUseCase getFinancialSnapshot) {
    this.listTransactions = listTransactions;
    this.getAccountBalance = getAccountBalance;
    this.listAccounts = listAccounts;
    this.getFinancialSnapshot = getFinancialSnapshot;
  }

  @Tool(
      description =
          "Retorna um snapshot financeiro consolidado em uma única chamada: status e horário da"
              + " última sincronização, contas e saldos atuais, transações sincronizadas desde a"
              + " última janela conhecida, transações pendentes, faturas de cartão em aberto,"
              + " conexões bancárias (items) e seus estados, investimentos, empréstimos e"
              + " erros/limitações. Consulte esta ferramenta primeiro para avaliar se os dados estão"
              + " completos e confiáveis; use as tools específicas para aprofundar em um recurso.")
  public FinancialSnapshot getFinancialSnapshot() {
    return getFinancialSnapshot.getFinancialSnapshot();
  }

  @Tool(
      description =
          "Lista as contas já sincronizadas no Home Davi. Use esta ferramenta primeiro para obter o"
              + " accountId antes de consultar saldo ou transações.")
  public List<FinancialAccount> listAccounts() {
    return listAccounts.listAccounts();
  }

  @Tool(description = "Lista as transações financeiras de uma conta pelo identificador da conta.")
  public List<FinancialTransaction> listTransactions(
      @ToolParam(description = "Identificador da conta") String accountId) {
    return listTransactions.listByAccount(accountId);
  }

  @Tool(
      description =
          "Busca na Pluggy o saldo atual de uma conta pelo identificador (UUID) da conta.")
  public AccountBalance getAccountBalance(
      @ToolParam(description = "Identificador (UUID) da conta na Pluggy") String accountId) {
    log.info("MCP tool getAccountBalance called: accountId={}", accountId);
    return getAccountBalance.getAccountBalance(UUID.fromString(accountId));
  }
}
