package com.iortatechnxt.brokerverse.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReleased;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.events.service.EventHousekeepingJob;
import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher;
import com.iortatechnxt.brokerverse.events.service.OutboxRelayJob;
import com.iortatechnxt.brokerverse.events.service.OutboxStore;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.OutboxEntry;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.OutboxFilter;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.OutboxStatus;
import com.iortatechnxt.brokerverse.integration.service.BookingEventAdapter;
import com.iortatechnxt.brokerverse.integration.service.IntegrationTopics;
import com.iortatechnxt.brokerverse.integration.service.OperationsEventAdapter;
import com.iortatechnxt.brokerverse.integration.service.WorkflowAndCatalogEventAdapter;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration events with Kafka disabled (test profile): every adapter writes its outbox row in the
 * business transaction, recorded as delivered in-process (LOCAL); the relay job marks leftovers
 * LOCAL; the support API refuses Kafka actions.
 */
@IntegrationTest
class IntegrationEventsLocalIT {

  private static final String ADMIN = "admin";

  @Autowired private ApplicationEventPublisher events;
  @Autowired private BookingEventAdapter booking;
  @Autowired private OperationsEventAdapter operations;
  @Autowired private WorkflowAndCatalogEventAdapter workflowAndCatalog;
  @Autowired private IntegrationEventPublisher publisher;
  @Autowired private OutboxStore outbox;
  @Autowired private OutboxRelayJob relayJob;
  @Autowired private EventHousekeepingJob housekeeping;
  @Autowired private MessageService messages;
  @Autowired private ClientService clients;
  @Autowired private ClientRepository clientRepository;
  @Autowired private TestData data;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;
  @Autowired private Api api;

  @Test
  void everyAdapterWritesItsEventInTheBusinessTransaction() {
    String ref = UUID.randomUUID().toString().substring(0, 8);
    Long company = data.company().getId();
    String batchId = String.valueOf(900_000_000L + Math.abs(ref.hashCode() % 1_000_000));
    tx.executeWithoutResult(
        s -> {
          // Adapters called directly: the real listeners of these events must not see test data.
          booking.on(invoiceBooked("BI-" + ref));
          operations.on(
              new OpsLedgerEvents.InvoiceMovementPosted(
                  company,
                  "INV-" + ref,
                  MovementType.APPLIED,
                  "CASHIERING",
                  "OR-" + ref,
                  Map.of(LedgerComponent.values()[0], new BigDecimal("100.00"))));
          operations.on(
              new OpsLedgerEvents.InvoiceMovementPosted(
                  company, "INV-" + ref, MovementType.BOOKED, "BOOKING", "X", Map.of()));
          operations.on(
              new OpsLedgerEvents.DisbursementStatusChanged(
                  company,
                  "PR-" + ref,
                  DisbursementRequest.Type.values()[0],
                  "TEST",
                  "REF-" + ref,
                  DisbursementRequest.Status.values()[0],
                  null,
                  null));
          workflowAndCatalog.on(
              new WorkCaseTransitioned(
                  1L, "OPS_REMITTANCE", "RemittanceBatch", batchId, "A", "B", "go", null, null));
          workflowAndCatalog.on(
              new WorkCaseTransitioned(1L, "OTHER", "Other", "1", "A", "B", "go", null, null));
          workflowAndCatalog.on(
              new ProductVersionReleased("PV-" + ref, 2, LocalDate.of(2026, 1, 1), null, "tsu"));
          // Through the event bus: the BEFORE_COMMIT listener joins the transaction.
          events.publishEvent(new OpsLedgerEvents.CollectionFeedReady(company, "FEED-" + ref));
        });
    assertThat(only("BI-" + ref).topic()).isEqualTo(IntegrationTopics.INVOICE_BOOKED);
    OutboxEntry applied = only("INV-" + ref);
    assertThat(applied.type()).isEqualTo(IntegrationTopics.TYPE_PAYMENT_APPLIED);
    assertThat(applied.companyCode()).isNotBlank();
    assertThat(applied.payload()).contains("OR-" + ref).contains("100.00");
    assertThat(only("PR-" + ref).topic()).isEqualTo(IntegrationTopics.DISBURSEMENT_STATUS);
    assertThat(only("FEED-" + ref).topic()).isEqualTo(IntegrationTopics.COLLECTION_FEED_READY);
    assertThat(only(batchId).topic()).isEqualTo(IntegrationTopics.REMITTANCE_BATCH_STATUS);
    assertThat(only("PV-" + ref).topic()).isEqualTo(IntegrationTopics.PRODUCT_VERSION_RELEASED);
    assertThat(only("PV-" + ref).status()).isEqualTo(OutboxStatus.LOCAL);
  }

  @Test
  void aRolledBackTransactionPublishesNothing() {
    String key = "ROLLBACK-" + UUID.randomUUID();
    tx.executeWithoutResult(
        s -> {
          events.publishEvent(new OpsLedgerEvents.CollectionFeedReady(1L, key));
          s.setRollbackOnly();
        });
    assertThat(rows(key)).isEmpty();
  }

  @Test
  void queuedEmailsAndClientChangesAreRecorded() {
    Long messageId =
        as.run(
                "proc",
                () ->
                    messages.queueEmail(
                        new OutboundEmail(
                            null,
                            "TEST",
                            List.of("someone@example.ph"),
                            null,
                            "Subject",
                            "Body",
                            null,
                            null,
                            null)))
            .messageId();
    assertThat(only(String.valueOf(messageId)).topic())
        .isEqualTo(IntegrationTopics.NOTIFICATION_REQUESTED);

    String name = "Outbox Test Corp " + UUID.randomUUID().toString().substring(0, 8);
    Client client =
        as.run(
            "ao",
            () ->
                clients.createProspect(
                    data.company().getId(),
                    new ClientDetails(
                        ClientType.CORPORATE,
                        new PersonName(null, null, null, null, name),
                        null,
                        null,
                        null,
                        null,
                        false,
                        null)));
    String key = String.valueOf(client.getId());
    assertThat(rows(key))
        .extracting(OutboxEntry::type)
        .contains(IntegrationTopics.TYPE_CLIENT_REGISTERED);
    tx.executeWithoutResult(
        s ->
            clientRepository
                .findById(client.getId())
                .orElseThrow()
                .setKycStatus(KycStatus.EXPIRED));
    OutboxEntry changed =
        rows(key).stream()
            .filter(r -> r.type().equals(IntegrationTopics.TYPE_CLIENT_CHANGED))
            .findFirst()
            .orElseThrow();
    assertThat(changed.payload()).contains("kycStatus").doesNotContain("\"tin\"");
    assertThat(changed.companyId()).isEqualTo(data.company().getId());
  }

  @Test
  void theRelayJobMarksLeftoversLocalAndHousekeepingPurges() {
    String key = "LEFTOVER-" + UUID.randomUUID();
    outbox.insert(
        new OutboxStore.NewOutboxRow(
            UUID.randomUUID(),
            IntegrationTopics.COLLECTION_FEED_READY,
            IntegrationTopics.TYPE_COLLECTION_FEED_READY,
            key,
            null,
            "c",
            Instant.now(),
            "{}",
            OutboxStatus.PENDING,
            "SYSTEM"));
    assertThat(relayJob.name()).isEqualTo("EVENT_OUTBOX_RELAY");
    assertThat(relayJob.cron()).isNotBlank();
    assertThat(relayJob.description()).isNotBlank();
    assertThat(relayJob.execute(LocalDate.now()).itemsProcessed()).isPositive();
    assertThat(only(key).status()).isEqualTo(OutboxStatus.LOCAL);
    assertThat(housekeeping.name()).isEqualTo(EventHousekeepingJob.JOB_NAME);
    assertThat(housekeeping.description()).isNotBlank();
    assertThat(housekeeping.cron()).isNotBlank();
    assertThat(housekeeping.execute(LocalDate.now()).message()).contains("outbox");
  }

  @Test
  void undeclaredTopicsAreRefused() {
    assertThatThrownBy(
            () ->
                publisher.publish(
                    new IntegrationEvent("bibs.nowhere.nothing.v1", "t", "k", null, Map.of())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void theSupportApiShowsEventsAndRefusesKafkaActionsWhenKafkaIsDisabled() throws Exception {
    String key = "API-" + UUID.randomUUID();
    tx.executeWithoutResult(
        s -> events.publishEvent(new OpsLedgerEvents.CollectionFeedReady(1L, key)));
    long id = only(key).id();
    api.doGet(ADMIN, "/api/v1/admin/events/topics")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(9))
        .andExpect(jsonPath("$[0].kafkaEnabled").value(false));
    api.doGet(ADMIN, "/api/v1/admin/events/outbox?status=LOCAL&key=" + key)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].status").value("LOCAL"));
    api.doGet(ADMIN, "/api/v1/admin/events/archive?key=" + key)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
    api.doGet(ADMIN, "/api/v1/admin/events/dead-letters").andExpect(status().isOk());
    api.doPost(ADMIN, "/api/v1/admin/events/outbox/" + id + "/retry", null)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("KAFKA_DISABLED"));
    api.doPost(ADMIN, "/api/v1/admin/events/dead-letters/1/retry", null)
        .andExpect(status().isUnprocessableEntity());
    api.doGet("accountant", "/api/v1/admin/events/topics").andExpect(status().isForbidden());
  }

  private List<OutboxEntry> rows(String key) {
    return outbox.search(new OutboxFilter(null, null, key), 50, 0);
  }

  private OutboxEntry only(String key) {
    List<OutboxEntry> rows = rows(key);
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }

  private static InvoiceBooked invoiceBooked(String invoiceNo) {
    return new InvoiceBooked(
        invoiceNo,
        "ARN-1",
        null,
        "CL-1",
        List.of(new InvoiceBooked.Share("INS-1", new BigDecimal("100"))),
        "PHP",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2027, 1, 1),
        "MC",
        "CBG",
        "ao",
        "SU1",
        "CC1",
        Map.of(),
        BigDecimal.TEN,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        false,
        false,
        false,
        InvoiceKind.values()[0],
        "POL-1",
        1,
        "MOTOR",
        null,
        null);
  }
}
