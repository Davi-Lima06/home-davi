package br.com.davi.homedavi.finance.application.service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.davi.homedavi.finance.application.port.out.finance.FinanceDataRepository;
import br.com.davi.homedavi.finance.application.port.out.integration.FinancialDataProvider;
import br.com.davi.homedavi.finance.application.port.out.integration.FinancialQueryProvider;
import br.com.davi.homedavi.finance.application.port.out.integration.HermesNotificationOutbox;
import br.com.davi.homedavi.finance.application.port.out.integration.TransactionSyncLog;
import br.com.davi.homedavi.finance.application.port.out.integration.WebhookEventQueue;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import br.com.davi.homedavi.finance.domain.integration.HermesOutboxMessage;
import br.com.davi.homedavi.finance.domain.integration.PluggyWebhookEvent;
import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import br.com.davi.homedavi.finance.domain.integration.SyncedInvestment;
import br.com.davi.homedavi.finance.domain.integration.SyncedItem;
import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import br.com.davi.homedavi.finance.domain.integration.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.integration.TransactionChanges;
import br.com.davi.homedavi.finance.domain.integration.TransactionPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PluggyWebhookProcessingServiceTests {
  private static final UUID ITEM = UUID.randomUUID();
  private static final UUID ACCOUNT = UUID.randomUUID();

  private final FakeQueue queue = new FakeQueue();
  private final List<TransactionChanges> saved = new ArrayList<>();
  private final List<SyncedAccount> syncedAccounts = new ArrayList<>();
  private final List<SyncedLoan> savedLoans = new ArrayList<>();
  private final List<SyncedBill> savedBills = new ArrayList<>();
  private final Set<UUID> createOnSave = new HashSet<>();
  private List<SyncedAccount> itemAccounts =
      new ArrayList<>(List.of(account(ACCOUNT), account(UUID.randomUUID())));
  private final FakeSyncLog syncLog = new FakeSyncLog();
  private final FakeHermesOutbox hermesOutbox = new FakeHermesOutbox();
  private final List<Instant> requestedCreatedAtFrom = new ArrayList<>();
  private boolean providerDown;

  @Test
  void syncsItemAccountsOnAnyWebhookWithItemId() {
    // item/updated: contas do item são atualizadas, mas como não são novas, nada é populado.
    var event = queue.add("item/updated", null, "[]", "{}");

    service().process(event.eventId());

    assertEquals(2, syncedAccounts.size());
    assertTrue(saved.isEmpty());
    assertTrue(savedLoans.isEmpty());
    assertTrue(savedBills.isEmpty());
    assertTrue(queue.processed.contains(event.eventId()));
    assertTrue(hermesOutbox.messages.getFirst().summaryJson().contains("accountsSynced"));
  }

  @Test
  void populatesLoansTransactionsAndBillsWhenAccountIsNew() {
    UUID cardAccount = UUID.randomUUID();
    itemAccounts = new ArrayList<>(List.of(creditAccount(cardAccount)));
    createOnSave.add(cardAccount); // a conta é criada agora
    var event = queue.add("item/created", null, "[]", "{}");

    service().process(event.eventId());

    assertEquals(1, savedLoans.size(), "empréstimos do item devem ser persistidos");
    assertEquals(1, saved.size(), "transações iniciais da conta nova devem ser persistidas");
    assertEquals(cardAccount, saved.getFirst().accounts().getFirst().id());
    assertEquals(1, saved.getFirst().transactions().size());
    assertEquals(1, savedBills.size(), "fatura deve ser persistida para conta de cartão");
    assertEquals(cardAccount, savedBills.getFirst().accountId());
  }

  @Test
  void doesNotFetchBillsForNewNonCreditAccount() {
    UUID bankAccount = UUID.randomUUID();
    itemAccounts = new ArrayList<>(List.of(account(bankAccount)));
    createOnSave.add(bankAccount);
    var event = queue.add("item/created", null, "[]", "{}");

    service().process(event.eventId());

    assertEquals(1, savedLoans.size());
    assertEquals(1, saved.size());
    assertTrue(savedBills.isEmpty(), "conta não-cartão não deve consultar faturas");
  }

  @Test
  void upsertsCreatedTransactionsFromPluggyAndMarksEventProcessed() {
    var event =
        queue.add(
            "transactions/created",
            ACCOUNT,
            "[]",
            "{\"transactionsCreatedAtFrom\":\"2026-10-05T10:00:00Z\"}");

    service().process(event.eventId());

    assertEquals(List.of(Instant.parse("2026-10-05T10:00:00Z")), requestedCreatedAtFrom);
    var changes = saved.getFirst();
    assertEquals(ITEM, changes.itemId());
    assertEquals(ACCOUNT, changes.accounts().getFirst().id());
    assertEquals(1, changes.transactions().size());
    assertTrue(queue.processed.contains(event.eventId()));
    assertEquals(List.of("COMPLETED"), syncLog.outcomes);
    assertEquals(1, hermesOutbox.messages.size());
    assertTrue(hermesOutbox.messages.getFirst().summaryJson().contains("transactionsUpserted"));
  }

  @Test
  void fetchesEachUpdatedTransactionAndAppliesDeletes() {
    var updatedId = UUID.randomUUID();
    var deletedId = UUID.randomUUID();
    var updated = queue.add("transactions/updated", ACCOUNT, "[\"" + updatedId + "\"]", "{}");
    var deleted = queue.add("transactions/deleted", ACCOUNT, "[\"" + deletedId + "\"]", "{}");

    service().process(updated.eventId());
    service().process(deleted.eventId());

    assertEquals(updatedId, saved.get(0).transactions().getFirst().id());
    assertEquals(1, saved.get(0).accounts().size());
    assertEquals(List.of(deletedId), saved.get(1).deletedTransactionIds());
    assertEquals(Set.of(updated.eventId(), deleted.eventId()), queue.processed);
  }

  @Test
  void marksEventFailedWhenPluggyIsUnavailable() {
    providerDown = true;
    var event = queue.add("transactions/created", ACCOUNT, "[]", "{}");

    service().process(event.eventId());

    assertTrue(saved.isEmpty());
    assertEquals("Pluggy unavailable", queue.failed.get(event.eventId()));
    assertEquals(List.of("FAILED"), syncLog.outcomes);
    assertTrue(hermesOutbox.messages.isEmpty());
  }

  @Test
  void ignoresEventThatIsNoLongerPending() {
    service().process(UUID.randomUUID());

    assertTrue(saved.isEmpty());
    assertTrue(syncedAccounts.isEmpty());
    assertTrue(syncLog.outcomes.isEmpty());
    assertNull(queue.failed.get(ACCOUNT));
  }

  private PluggyWebhookProcessingService service() {
    FinancialDataProvider provider =
        new FinancialDataProvider() {
          @Override
          public SyncedAccount fetchAccount(UUID accountId) {
            if (providerDown) throw new IllegalStateException("Pluggy unavailable");
            return account(accountId);
          }

          @Override
          public SyncedTransaction fetchTransaction(UUID transactionId) {
            return transaction(transactionId);
          }

          @Override
          public List<SyncedTransaction> fetchCreatedTransactions(
              UUID accountId, Instant createdAtFrom) {
            if (providerDown) throw new IllegalStateException("Pluggy unavailable");
            requestedCreatedAtFrom.add(createdAtFrom);
            return List.of(transaction(UUID.randomUUID()));
          }
        };
    FinancialQueryProvider queryProvider =
        new FinancialQueryProvider() {
          @Override
          public List<SyncedAccount> fetchAccounts(UUID itemId) {
            if (providerDown) throw new IllegalStateException("Pluggy unavailable");
            return itemAccounts;
          }

          @Override
          public TransactionPage fetchTransactionsPage(UUID accountId, String cursor, int pageSize) {
            return new TransactionPage(List.of(transaction(UUID.randomUUID())), null);
          }

          @Override
          public List<SyncedBill> fetchBills(UUID accountId) {
            return List.of(
                new SyncedBill(
                    UUID.randomUUID(), accountId, LocalDate.parse("2026-11-10"),
                    new BigDecimal("1200.00"), new BigDecimal("300.00"), "BRL", "{}"));
          }

          @Override
          public List<SyncedItem> fetchItems() {
            return List.of();
          }

          @Override
          public List<SyncedInvestment> fetchInvestments(UUID itemId) {
            return List.of();
          }

          @Override
          public List<SyncedLoan> fetchLoans(UUID itemId) {
            return List.of(new SyncedLoan(UUID.randomUUID(), "CT-1", new BigDecimal("2500.00"), "BRL", "{}"));
          }
        };
    FinanceDataRepository financeData =
        new FinanceDataRepository() {
          @Override
          public void saveTransactionChanges(TransactionChanges changes) {
            saved.add(changes);
          }

          @Override
          public List<UUID> saveAccounts(UUID itemId, List<SyncedAccount> accounts) {
            syncedAccounts.addAll(accounts);
            return accounts.stream().map(SyncedAccount::id).filter(createOnSave::contains).toList();
          }

          @Override
          public void saveLoans(UUID itemId, List<SyncedLoan> loans) {
            savedLoans.addAll(loans);
          }

          @Override
          public void saveBills(List<SyncedBill> bills) {
            savedBills.addAll(bills);
          }
        };
    return new PluggyWebhookProcessingService(
        queue, provider, queryProvider, financeData, syncLog, hermesOutbox, new ObjectMapper());
  }

  private static SyncedAccount account(UUID id) {
    return new SyncedAccount(
        id, "Conta", "BANK", "CHECKING_ACCOUNT", null, "BRL", BigDecimal.TEN, BigDecimal.TEN, null,
        null, "{}");
  }

  private static SyncedAccount creditAccount(UUID id) {
    return new SyncedAccount(
        id, "Cartão", "CREDIT", "CREDIT_CARD", null, "BRL", new BigDecimal("-100.00"), null,
        new BigDecimal("5000.00"), "ACTIVE", "{}");
  }

  private static SyncedTransaction transaction(UUID id) {
    return new SyncedTransaction(
        id,
        ACCOUNT,
        "Mercado",
        null,
        "Groceries",
        new BigDecimal("-12.30"),
        "BRL",
        TransactionType.EXPENSE,
        "POSTED",
        Instant.now(),
        false,
        "{}");
  }

  private static class FakeQueue implements WebhookEventQueue {
    final Map<UUID, PluggyWebhookEvent> pending = new HashMap<>();
    final Set<UUID> processed = new HashSet<>();
    final Map<UUID, String> failed = new HashMap<>();

    PluggyWebhookEvent add(String type, UUID accountId, String transactionIds, String payload) {
      var event =
          new PluggyWebhookEvent(
              UUID.randomUUID(), type, ITEM, accountId, null, null, null, transactionIds, payload);
      pending.put(event.eventId(), event);
      return event;
    }

    @Override
    public Optional<PluggyWebhookEvent> claimPendingEvent(UUID eventId) {
      return Optional.ofNullable(pending.remove(eventId));
    }

    @Override
    public void markProcessed(UUID eventId) {
      processed.add(eventId);
    }

    @Override
    public void markFailed(UUID eventId, String error) {
      failed.put(eventId, error);
    }
  }

  private static class FakeSyncLog implements TransactionSyncLog {
    final List<String> outcomes = new ArrayList<>();
    private long sequence;

    @Override
    public long startSync(UUID webhookEventId, UUID itemId) {
      return ++sequence;
    }

    @Override
    public void completeSync(long syncId) {
      outcomes.add("COMPLETED");
    }

    @Override
    public void failSync(long syncId, String error) {
      outcomes.add("FAILED");
    }
  }

  private static class FakeHermesOutbox implements HermesNotificationOutbox {
    final List<HermesOutboxMessage> messages = new ArrayList<>();

    @Override
    public void enqueue(HermesOutboxMessage message) {
      messages.add(message);
    }

    @Override
    public List<HermesOutboxMessage> claimReady(int limit, Instant now) {
      return List.of();
    }

    @Override
    public void markSent(long outboxId, Instant sentAt) {}

    @Override
    public void markForRetry(long outboxId, Instant availableAt, String error) {}
  }
}
