package br.com.davi.homedavi.finance.application.service;

import br.com.davi.homedavi.finance.application.port.in.ProcessPluggyWebhookUseCase;
import br.com.davi.homedavi.finance.application.port.out.FinanceDataRepository;
import br.com.davi.homedavi.finance.application.port.out.FinancialDataProvider;
import br.com.davi.homedavi.finance.application.port.out.HermesNotificationOutbox;
import br.com.davi.homedavi.finance.application.port.out.TransactionSyncLog;
import br.com.davi.homedavi.finance.application.port.out.WebhookEventQueue;
import br.com.davi.homedavi.finance.domain.HermesOutboxMessage;
import br.com.davi.homedavi.finance.domain.PluggyWebhookEvent;
import br.com.davi.homedavi.finance.domain.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.TransactionChanges;
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
  private final FinanceDataRepository financeData;
  private final TransactionSyncLog syncLog;
  private final HermesNotificationOutbox hermesOutbox;
  private final ObjectMapper objectMapper;

  public PluggyWebhookProcessingService(
      WebhookEventQueue eventQueue,
      FinancialDataProvider dataProvider,
      FinanceDataRepository financeData,
      TransactionSyncLog syncLog,
      HermesNotificationOutbox hermesOutbox,
      ObjectMapper objectMapper) {
    this.eventQueue = eventQueue;
    this.dataProvider = dataProvider;
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
              null, event.eventId(), event.eventType(), summary(event, changes), 0));
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
  private String summary(PluggyWebhookEvent event, TransactionChanges changes) {
    var summary = objectMapper.createObjectNode();
    summary.put("processedAt", Instant.now().toString());
    summary.put("itemId", event.itemId() == null ? null : event.itemId().toString());
    summary.put("accountId", event.accountId() == null ? null : event.accountId().toString());
    summary.put("triggeredBy", event.triggeredBy());
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
