package br.com.davi.homedavi.finance.adapter.out.integration.pluggy;

import br.com.davi.homedavi.finance.application.port.out.integration.FinancialDataProvider;
import br.com.davi.homedavi.finance.application.port.out.integration.FinancialQueryProvider;
import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import br.com.davi.homedavi.finance.domain.integration.SyncedInvestment;
import br.com.davi.homedavi.finance.domain.integration.SyncedItem;
import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import br.com.davi.homedavi.finance.domain.integration.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.integration.TransactionPage;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * Traduz a API REST do Pluggy (https://docs.pluggy.ai) para o modelo do domínio.
 */
@Component
public class PluggyApiClient implements FinancialDataProvider, FinancialQueryProvider {
    private static final Logger log = LoggerFactory.getLogger(PluggyApiClient.class);

    // A apiKey do Pluggy expira em 2h; renova com folga.
    private static final Duration API_KEY_TTL = Duration.ofMinutes(110);
    private static final int PAGE_SIZE = 500;

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private String apiKey;
    private Instant apiKeyExpiresAt = Instant.EPOCH;

    public PluggyApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${pluggy.api.base-url}") String baseUrl,
            @Value("${pluggy.api.client-id}") String clientId,
            @Value("${pluggy.api.client-secret}") String clientSecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private static SyncedAccount toAccount(JsonNode node) {
        JsonNode credit = node.path("creditData");
        boolean isCredit = "CREDIT".equals(text(node, "type"));
        return new SyncedAccount(
                UUID.fromString(node.get("id").asText()),
                text(node, "name"),
                text(node, "type"),
                text(node, "subtype"),
                mask(text(node, "number")),
                text(node, "currencyCode"),
                decimal(node, "balance"),
                isCredit ? decimal(credit, "availableCreditLimit") : decimal(node, "balance"),
                decimal(credit, "creditLimit"),
                text(credit, "status"),
                node.toString());
    }

    private static SyncedTransaction toTransaction(JsonNode node) {
        String status = text(node, "status");
        String description = text(node, "description");
        return new SyncedTransaction(
                UUID.fromString(node.get("id").asText()),
                UUID.fromString(node.get("accountId").asText()),
                description == null ? text(node, "descriptionRaw") : description,
                text(node.path("merchant"), "name"),
                text(node, "category"),
                decimal(node, "amount"),
                text(node, "currencyCode"),
                "CREDIT".equals(text(node, "type")) ? TransactionType.INCOME : TransactionType.EXPENSE,
                status,
                OffsetDateTime.parse(node.get("date").asText()).toInstant(),
                "PENDING".equals(status),
                node.toString());
    }

    private static SyncedBill toBill(UUID accountId, JsonNode node) {
        return new SyncedBill(
                UUID.fromString(node.get("id").asText()),
                accountId,
                date(node, "dueDate"),
                decimal(node, "totalAmount"),
                decimal(node, "minimumPayment"),
                text(node, "totalAmountCurrencyCode"),
                node.toString());
    }

    // --- FinancialQueryProvider: leituras ao vivo para as tools MCP ---

    private static SyncedItem toItem(JsonNode node) {
        String updated = text(node, "lastUpdatedAt");
        return new SyncedItem(
                UUID.fromString(node.get("id").asText()),
                text(node.path("connector"), "name"),
                text(node, "status"),
                text(node, "executionStatus"),
                updated == null ? null : OffsetDateTime.parse(updated).toInstant(),
                node.toString());
    }

    private static SyncedInvestment toInvestment(JsonNode node) {
        return new SyncedInvestment(
                UUID.fromString(node.get("id").asText()),
                text(node, "name"),
                text(node, "type"),
                decimal(node, "balance"),
                text(node, "currencyCode"),
                node.toString());
    }

    private static SyncedLoan toLoan(JsonNode node) {
        return new SyncedLoan(
                UUID.fromString(node.get("id").asText()),
                text(node, "contractNumber"),
                decimal(node, "outstandingBalance"),
                text(node, "currencyCode"),
                node.toString());
    }

    private static LocalDate date(JsonNode node, String field) {
        String value = text(node, field);
        // Pluggy manda datas como YYYY-MM-DD ou ISO com horário; cobre os dois.
        return value == null ? null : LocalDate.parse(value.substring(0, 10));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    private static String mask(String number) {
        if (number == null) return null;
        return number.length() <= 4 ? number : "****" + number.substring(number.length() - 4);
    }

    @Override
    public SyncedAccount fetchAccount(UUID accountId) {
        return toAccount(get("/accounts/{id}", accountId));
    }

    @Override
    public SyncedTransaction fetchTransaction(UUID transactionId) {
        return toTransaction(get("/transactions/{id}", transactionId));
    }

    @Override
    public List<SyncedTransaction> fetchCreatedTransactions(UUID accountId, Instant createdAtFrom) {
        return fetchAllPages(
                uri -> {
                    uri.path("/transactions")
                            .queryParam("accountId", accountId)
                            .queryParam("pageSize", PAGE_SIZE);
                    return createdAtFrom == null ? uri : uri.queryParam("createdAtFrom", createdAtFrom);
                })
                .stream()
                .map(PluggyApiClient::toTransaction)
                .toList();
    }

    @Override
    public List<SyncedAccount> fetchAccounts(UUID itemId) {
        return fetchAllPages(uri -> uri.path("/accounts").queryParam("itemId", itemId)).stream()
                .map(PluggyApiClient::toAccount)
                .toList();
    }

    @Override
    public TransactionPage fetchTransactionsPage(UUID accountId, String cursor, int pageSize) {
        JsonNode body =
                restClient
                        .get()
                        .uri(
                                uri -> {
                                    // A API v2 não aceita pageSize (rejeita com 400); a paginação é só por cursor.
                                    uri.path("/v2/transactions").queryParam("accountId", accountId);
                                    return cursor == null || cursor.isBlank()
                                            ? uri.build()
                                            : uri.queryParam("cursor", cursor).build();
                                })
                        .header("X-API-KEY", apiKey())
                        .retrieve()
                        .body(JsonNode.class);
        if (body == null) throw new IllegalStateException("Pluggy returned an empty body for /v2/transactions");
        // v2 usa paginação por cursor; aceita results/data e nextCursor na raiz ou em page.
        JsonNode results = body.has("results") ? body.path("results") : body.path("data");
        var transactions = new ArrayList<SyncedTransaction>();
        results.forEach(node -> transactions.add(toTransaction(node)));
        String nextCursor = text(body, "nextCursor");
        if (nextCursor == null) nextCursor = text(body.path("page"), "nextCursor");
        log.debug(
                "Pluggy v2 transactions page: accountId={}, count={}, nextCursor={}",
                accountId,
                transactions.size(),
                nextCursor != null);
        return new TransactionPage(transactions, nextCursor);
    }

    @Override
    public List<SyncedBill> fetchBills(UUID accountId) {
        return fetchAllPages(uri -> uri.path("/bills").queryParam("accountId", accountId)).stream()
                .map(node -> toBill(accountId, node))
                .toList();
    }

    @Override
    public List<SyncedItem> fetchItems() {
        // /v2/items é v2 (cursor), não aceita o parâmetro `page` do fetchAllPages; busca direta.
        JsonNode body = get("/v2/items");
        JsonNode results =
                body.has("results") ? body.path("results") : body.has("data") ? body.path("data") : body;
        var items = new ArrayList<SyncedItem>();
        results.forEach(node -> items.add(toItem(node)));
        return items;
    }

    @Override
    public List<SyncedInvestment> fetchInvestments(UUID itemId) {
        return fetchAllPages(uri -> uri.path("/investments").queryParam("itemId", itemId)).stream()
                .map(PluggyApiClient::toInvestment)
                .toList();
    }

    @Override
    public List<SyncedLoan> fetchLoans(UUID itemId) {
        return fetchAllPages(uri -> uri.path("/loans").queryParam("itemId", itemId)).stream()
                .map(PluggyApiClient::toLoan)
                .toList();
    }

    private JsonNode get(String path, Object... uriVariables) {
        log.debug("[4/7] GET {} {}", path, uriVariables);
        JsonNode body =
                restClient
                        .get()
                        .uri(path, uriVariables)
                        .header("X-API-KEY", apiKey())
                        .retrieve()
                        .body(JsonNode.class);
        if (body == null) throw new IllegalStateException("Pluggy returned an empty body for " + path);
        return body;
    }

    private List<JsonNode> fetchAllPages(Function<UriBuilder, UriBuilder> query) {
        var results = new ArrayList<JsonNode>();
        int page = 1;
        int totalPages;
        do {
            int currentPage = page;
            JsonNode body =
                    restClient
                            .get()
                            .uri(uri -> query.apply(uri).queryParam("page", currentPage).build())
                            .header("X-API-KEY", apiKey())
                            .retrieve()
                            .body(JsonNode.class);
            if (body == null) break;
            body.path("results").forEach(results::add);
            totalPages = body.path("totalPages").asInt(1);
            log.debug(
                    "[4/7] Pluggy page {}/{} fetched, {} results so far",
                    currentPage,
                    totalPages,
                    results.size());
        } while (page++ < totalPages);
        return results;
    }

    private synchronized String apiKey() {
        if (apiKey != null && Instant.now().isBefore(apiKeyExpiresAt)) {
            log.debug("[3/7] Reusing Pluggy API key, valid until {}", apiKeyExpiresAt);
        } else {
            log.info("[3/7] Requesting new Pluggy API key");
            JsonNode body =
                    restClient
                            .post()
                            .uri("/auth")
                            .body(Map.of("clientId", clientId, "clientSecret", clientSecret))
                            .retrieve()
                            .body(JsonNode.class);
            if (body == null || !body.hasNonNull("apiKey"))
                throw new IllegalStateException("Pluggy auth returned no apiKey");
            apiKey = body.get("apiKey").asText();
            apiKeyExpiresAt = Instant.now().plus(API_KEY_TTL);
            log.info("[3/7] Pluggy API key generated, reusing until {}", apiKeyExpiresAt);
        }
        return apiKey;
    }
}
