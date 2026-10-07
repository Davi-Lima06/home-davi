package br.com.davi.homedavi.finance.adapter.in.integration.mcp;

import br.com.davi.homedavi.finance.application.port.in.integration.ListAccountTransactionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListBankConnectionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListConnectedAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListCreditCardBillsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListInvestmentsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListLoansUseCase;
import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import br.com.davi.homedavi.finance.domain.integration.SyncedInvestment;
import br.com.davi.homedavi.finance.domain.integration.SyncedItem;
import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import br.com.davi.homedavi.finance.domain.integration.TransactionPage;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Tools MCP do tópico finance que consultam o Pluggy ao vivo. São leituras diretas na API; para o
 * retrato já sincronizado no banco, use as tools de snapshot/contas do {@link FinanceMcpTools}.
 */
@Component
public class PluggyQueryMcpTools {
  private static final Logger log = LoggerFactory.getLogger(PluggyQueryMcpTools.class);

  private final ListConnectedAccountsUseCase listAccounts;
  private final ListAccountTransactionsUseCase listTransactions;
  private final ListCreditCardBillsUseCase listBills;
  private final ListBankConnectionsUseCase listItems;
  private final ListInvestmentsUseCase listInvestments;
  private final ListLoansUseCase listLoans;

  public PluggyQueryMcpTools(
      ListConnectedAccountsUseCase listAccounts,
      ListAccountTransactionsUseCase listTransactions,
      ListCreditCardBillsUseCase listBills,
      ListBankConnectionsUseCase listItems,
      ListInvestmentsUseCase listInvestments,
      ListLoansUseCase listLoans) {
    this.listAccounts = listAccounts;
    this.listTransactions = listTransactions;
    this.listBills = listBills;
    this.listItems = listItems;
    this.listInvestments = listInvestments;
    this.listLoans = listLoans;
  }

  @Tool(
      description =
          "Lista no Pluggy as contas bancárias e cartões de uma conexão (item), pelo itemId. É a"
              + " base para as demais consultas: use o accountId retornado para saldo, transações e"
              + " faturas.")
  public List<SyncedAccount> listConnectedAccounts(
      @ToolParam(description = "Identificador (UUID) da conexão (item) na Pluggy") String itemId) {
    log.info("MCP tool listConnectedAccounts called: itemId={}", itemId);
    return listAccounts.listAccounts(UUID.fromString(itemId));
  }

  @Tool(
      description =
          "Lista no Pluggy (API v2) uma página de transações de uma conta, com paginação por"
              + " cursor. Informe o nextCursor da resposta anterior para avançar; omita no primeiro"
              + " acesso.")
  public TransactionPage listAccountTransactions(
      @ToolParam(description = "Identificador (UUID) da conta na Pluggy") String accountId,
      @ToolParam(required = false, description = "Cursor da próxima página (nextCursor anterior)")
          String cursor,
      @ToolParam(required = false, description = "Tamanho da página (1 a 500; padrão 100)")
          Integer pageSize) {
    log.info("MCP tool listAccountTransactions called: accountId={}", accountId);
    return listTransactions.listTransactions(UUID.fromString(accountId), cursor, pageSize);
  }

  @Tool(
      description =
          "Lista no Pluggy as faturas de um cartão de crédito, pelo accountId do cartão.")
  public List<SyncedBill> listCreditCardBills(
      @ToolParam(description = "Identificador (UUID) da conta de cartão na Pluggy") String accountId) {
    log.info("MCP tool listCreditCardBills called: accountId={}", accountId);
    return listBills.listBills(UUID.fromString(accountId));
  }

  @Tool(
      description =
          "Lista no Pluggy (API v2) as conexões bancárias (items) e seus estados. Útil para"
              + " descobrir conexões desatualizadas ou com falha. Endpoint opt-in: pode exigir"
              + " habilitação junto à Pluggy.")
  public List<SyncedItem> listBankConnections() {
    log.info("MCP tool listBankConnections called");
    return listItems.listItems();
  }

  @Tool(description = "Lista no Pluggy os investimentos de uma conexão (item), pelo itemId.")
  public List<SyncedInvestment> listInvestments(
      @ToolParam(description = "Identificador (UUID) da conexão (item) na Pluggy") String itemId) {
    log.info("MCP tool listInvestments called: itemId={}", itemId);
    return listInvestments.listInvestments(UUID.fromString(itemId));
  }

  @Tool(description = "Lista no Pluggy os empréstimos de uma conexão (item), pelo itemId.")
  public List<SyncedLoan> listLoans(
      @ToolParam(description = "Identificador (UUID) da conexão (item) na Pluggy") String itemId) {
    log.info("MCP tool listLoans called: itemId={}", itemId);
    return listLoans.listLoans(UUID.fromString(itemId));
  }
}
