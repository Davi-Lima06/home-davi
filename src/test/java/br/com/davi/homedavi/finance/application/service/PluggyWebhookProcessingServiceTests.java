package br.com.davi.homedavi.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.davi.homedavi.finance.application.port.out.FinancialDataProvider;
import br.com.davi.homedavi.finance.application.port.out.HermesNotificationOutbox;
import br.com.davi.homedavi.finance.application.port.out.TransactionSyncLog;
import br.com.davi.homedavi.finance.application.port.out.WebhookEventQueue;
import br.com.davi.homedavi.finance.domain.HermesOutboxMessage;
import br.com.davi.homedavi.finance.domain.PluggyWebhookEvent;
import br.com.davi.homedavi.finance.domain.SyncedAccount;
import br.com.davi.homedavi.finance.domain.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.TransactionChanges;
import br.com.davi.homedavi.finance.domain.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
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
  private final FakeSyncLog syncLog = new FakeSyncLog();
  private final FakeHermesOutbox hermesOutbox = new FakeHermesOutbox();
  private final List<Instant> requestedCreatedAtFrom = new ArrayList<>();
  private boolean providerDown;

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
    assertTrue(syncLog.outcomes.isEmpty());
    assertNull(queue.failed.get(ACCOUNT));
  }

  private PluggyWebhookProcessingService service() {
    FinancialDataProvider provider =
        new FinancialDataProvider() {
          @Override
          public SyncedAccount fetchAccount(UUID accountId) {
            if (providerDown) throw new IllegalStateException("Pluggy unavailable");
            return new SyncedAccount(
                accountId,
                "Conta",
                "BANK",
                "CHECKING_ACCOUNT",
                null,
                "BRL",
                BigDecimal.TEN,
                BigDecimal.TEN,
                null,
                null,
                "{}");
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
    return new PluggyWebhookProcessingService(
        queue, provider, saved::add, syncLog, hermesOutbox, new ObjectMapper());
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
