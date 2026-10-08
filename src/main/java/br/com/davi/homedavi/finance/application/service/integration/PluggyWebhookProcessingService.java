package br.com.davi.homedavi.finance.application.service.integration;

import br.com.davi.homedavi.finance.application.port.in.integration.ProcessPluggyWebhookUseCase;
import br.com.davi.homedavi.finance.application.port.out.finance.FinanceDataRepository;
import br.com.davi.homedavi.finance.application.port.out.integration.FinancialDataProvider;
import br.com.davi.homedavi.finance.application.port.out.integration.FinancialQueryProvider;
import br.com.davi.homedavi.finance.application.port.out.integration.HermesNotificationOutbox;
import br.com.davi.homedavi.finance.application.port.out.integration.TransactionSyncLog;
import br.com.davi.homedavi.finance.application.port.out.integration.WebhookEventQueue;
import br.com.davi.homedavi.finance.domain.integration.HermesOutboxMessage;
import br.com.davi.homedavi.finance.domain.integration.PluggyWebhookEvent;
import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.integration.TransactionChanges;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Os webhooks do Pluggy só avisam o que mudou: os dados completos são buscados na API do Pluggy e
 * aplicados em finance.transactions.
 */
@Service
public class PluggyWebhookProcessingService implements ProcessPluggyWebhookUseCase {
  private static final Logger log = LoggerFactory.getLogger(PluggyWebhookProcessingService.class);

  private final WebhookEventQueue eventQueue;
  private final FinancialDataProvider dataProvider;
  private final FinancialQueryProvider queryProvider;
  private final FinanceDataRepository financeData;
  private final TransactionSyncLog syncLog;
  private final HermesNotificationOutbox hermesOutbox;
  private final ObjectMapper objectMapper;

  public PluggyWebhookProcessingService(
      WebhookEventQueue eventQueue,
      FinancialDataProvider dataProvider,
      FinancialQueryProvider queryProvider,
      FinanceDataRepository financeData,
      TransactionSyncLog syncLog,
      HermesNotificationOutbox hermesOutbox,
      ObjectMapper objectMapper) {
    this.eventQueue = eventQueue;
    this.dataProvider = dataProvider;
    this.queryProvider = queryProvider;
    this.financeData = financeData;
    this.syncLog = syncLog;
    this.hermesOutbox = hermesOutbox;
    this.objectMapper = objectMapper;
  }

  @Override
  public void process(UUID eventId) {
    var claimed = eventQueue.claimPendingEvent(eventId);
    if (claimed.isEmpty()) {
      log.info("[2/7] Pluggy webhook is not pending anymore, skipping: eventId={}", eventId);
      return;
    }
    var event = claimed.get();
    log.info(
        "[2/7] Pending Pluggy webhook claimed: eventId={}, type={}, itemId={}",
        eventId,
        event.eventType(),
        event.itemId());
    Long syncId =
        event.itemId() == null ? null : syncLog.startSync(event.eventId(), event.itemId());
    try {
      int syncedAccounts = syncAccounts(event);
      var changes =
          switch (event.eventType()) {
            case "transactions/created" -> createdTransactions(event);
            case "transactions/updated" -> updatedTransactions(event);
            case "transactions/deleted" -> {
              log.info("[4/7] Deleted transactions need no Pluggy call: eventId={}", eventId);
              yield new TransactionChanges(
                  requiredItemId(event), List.of(), List.of(), transactionIds(event));
            }
            default -> null;
          };
      if (changes == null) {
        log.info(
            "[5/7] Event type {} does not change finance.transactions: eventId={}",
            event.eventType(),
            eventId);
      } else {
        financeData.saveTransactionChanges(changes);
        log.info(
            "[5/7] finance updated: eventId={}, accounts={}, upsertedTransactions={},"
                + " deletedTransactions={}",
            eventId,
            changes.accounts().size(),
            changes.transactions().size(),
            changes.deletedTransactionIds().size());
      }
      eventQueue.markProcessed(eventId);
      log.info("[6/7] Pluggy webhook marked as processed: eventId={}", eventId);
      hermesOutbox.enqueue(
          new HermesOutboxMessage(
              null, event.eventId(), event.eventType(), summary(event, changes, syncedAccounts), 0));
      log.info("[7/7] Financial summary stored in Hermes outbox: eventId={}", eventId);
      if (syncId != null) syncLog.completeSync(syncId);
    } catch (RuntimeException exception) {
      log.error(
          "Could not process Pluggy webhook, marking as failed: eventId={}, type={}",
          eventId,
          event.eventType(),
          exception);
      String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
      eventQueue.markFailed(eventId, error);
      if (syncId != null) syncLog.failSync(syncId, error);
    }
  }

  /**
   * Ao receber um webhook com itemId, cria/atualiza as contas daquele item buscando-as na própria
   * API da Pluggy (GET /accounts?itemId=). Vale para qualquer tipo de evento (inclusive item/*).
   */
  private int syncAccounts(PluggyWebhookEvent event) {
    if (event.itemId() == null) {
      log.info("[3/7] Webhook sem itemId; nenhuma conta para sincronizar: eventId={}", event.eventId());
      return 0;
    }
    UUID itemId = event.itemId();
    log.info("[3/7] Item presente; buscando contas na Pluggy: itemId={}", itemId);
    var accounts = queryProvider.fetchAccounts(itemId);
    var created = financeData.saveAccounts(itemId, accounts);
    log.info(
        "[3/7] Contas criadas/atualizadas: itemId={}, total={}, novas={}",
        itemId,
        accounts.size(),
        created.size());
    if (!created.isEmpty()) populateNewAccounts(itemId, accounts, created);
    return accounts.size();
  }

  /**
   * Quando uma conta é criada agora, popula o banco com empréstimos (por item), transações (v2, por
   * conta) e faturas (por conta de cartão) daquele item/contas novas.
   */
  private void populateNewAccounts(UUID itemId, List<SyncedAccount> accounts, List<UUID> createdIds) {
    log.info("[3/7] Contas novas detectadas; populando dados: itemId={}, novas={}", itemId, createdIds.size());

    var loans = queryProvider.fetchLoans(itemId);
    financeData.saveLoans(itemId, loans);
    log.info("[3/7] Empréstimos persistidos: itemId={}, loans={}", itemId, loans.size());

    for (var account : accounts) {
      if (!createdIds.contains(account.id())) continue;

      var transactions = fetchAllTransactions(account.id());
      financeData.saveTransactionChanges(
          new TransactionChanges(itemId, List.of(account), transactions, List.of()));
      log.info(
          "[3/7] Transações iniciais persistidas: accountId={}, transactions={}",
          account.id(),
          transactions.size());

      if ("CREDIT".equalsIgnoreCase(account.type())) {
        var bills = queryProvider.fetchBills(account.id());
        financeData.saveBills(bills);
        log.info("[3/7] Faturas persistidas: accountId={}, bills={}", account.id(), bills.size());
      }
    }
  }

  /** Percorre as páginas por cursor da API v2 de transações da conta. */
  private List<SyncedTransaction> fetchAllTransactions(UUID accountId) {
    var all = new ArrayList<SyncedTransaction>();
    String cursor = null;
    int page = 0;
    do {
      var current = queryProvider.fetchTransactionsPage(accountId, cursor, 500);
      all.addAll(current.transactions());
      cursor = current.nextCursor();
    } while (cursor != null && !cursor.isBlank() && ++page < 100);
    return all;
  }

  private TransactionChanges createdTransactions(PluggyWebhookEvent event) {
    var accountId = requiredAccountId(event);
    var createdAtFrom = payload(event).path("transactionsCreatedAtFrom");
    var from = createdAtFrom.isTextual() ? Instant.parse(createdAtFrom.asText()) : null;
    if (from == null)
      log.warn(
          "[4/7] Webhook without transactionsCreatedAtFrom, fetching all transactions of account"
              + " {}",
          accountId);
    log.info(
        "[4/7] Fetching created transactions from Pluggy: accountId={}, createdAtFrom={}",
        accountId,
        from);
    var transactions = dataProvider.fetchCreatedTransactions(accountId, from);
    var account = dataProvider.fetchAccount(accountId);
    log.info(
        "[4/7] Fetched {} created transactions from Pluggy: accountId={}",
        transactions.size(),
        accountId);
    return new TransactionChanges(requiredItemId(event), List.of(account), transactions, List.of());
  }

  private TransactionChanges updatedTransactions(PluggyWebhookEvent event) {
    var ids = transactionIds(event);
    log.info(
        "[4/7] Fetching {} updated transactions from Pluggy: eventId={}",
        ids.size(),
        event.eventId());
    var transactions = ids.stream().map(dataProvider::fetchTransaction).toList();
    // A FK de finance.transactions exige a conta; ela é atualizada junto.
    var accounts =
        transactions.stream()
            .map(SyncedTransaction::accountId)
            .distinct()
            .map(dataProvider::fetchAccount)
            .toList();
    log.info(
        "[4/7] Fetched {} transactions and {} accounts from Pluggy: eventId={}",
        transactions.size(),
        accounts.size(),
        event.eventId());
    return new TransactionChanges(requiredItemId(event), accounts, transactions, List.of());
  }

  private UUID requiredItemId(PluggyWebhookEvent event) {
    if (event.itemId() == null)
      throw new IllegalStateException("Event without itemId: " + event.eventId());
    return event.itemId();
  }

  private UUID requiredAccountId(PluggyWebhookEvent event) {
    if (event.accountId() == null)
      throw new IllegalStateException("Event without accountId: " + event.eventId());
    return event.accountId();
  }

  private List<UUID> transactionIds(PluggyWebhookEvent event) {
    var ids = new ArrayList<UUID>();
    readTree(event.transactionIdsJson()).forEach(id -> ids.add(UUID.fromString(id.asText())));
    return ids;
  }

  private JsonNode payload(PluggyWebhookEvent event) {
    return readTree(event.rawPayloadJson());
  }

  /**
   * O Hermes recebe somente metadados operacionais. Para consultar valores ou descrições, ele usa o
   * MCP/API do Home Davi. Assim a memória conversacional do agente não vira uma cópia do extrato
   * bancário.
   */
  private String summary(PluggyWebhookEvent event, TransactionChanges changes, int syncedAccounts) {
    var summary = objectMapper.createObjectNode();
    summary.put("processedAt", Instant.now().toString());
    summary.put("itemId", event.itemId() == null ? null : event.itemId().toString());
    summary.put("accountId", event.accountId() == null ? null : event.accountId().toString());
    summary.put("triggeredBy", event.triggeredBy());
    summary.put("accountsSynced", syncedAccounts);
    summary.put("accountsUpdated", changes == null ? 0 : changes.accounts().size());
    summary.put("transactionsUpserted", changes == null ? 0 : changes.transactions().size());
    summary.put(
        "transactionsDeleted", changes == null ? 0 : changes.deletedTransactionIds().size());
    try {
      return objectMapper.writeValueAsString(summary);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize Hermes summary", exception);
    }
  }

  private JsonNode readTree(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Invalid stored webhook JSON", exception);
    }
  }
}
