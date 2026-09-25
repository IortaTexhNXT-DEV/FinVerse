package com.iortatechnxt.brokerverse.integration.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.events.service.EventsProperties;
import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.OutboxEventPublisher;
import com.iortatechnxt.brokerverse.events.service.OutboxRelay;
import com.iortatechnxt.brokerverse.events.service.OutboxStore;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.NewOutboxRow;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Integration events of modules that publish no in-process event for the fact: clients registered
 * or changed ({@code bibs.crm.client-changed.v1}) and receipts issued ({@code
 * bibs.cashiering.receipt-issued.v1}). A Hibernate listener sees every insert and update of those
 * entities, whatever the write path, and writes the outbox row through the same connection just
 * before the transaction commits (after the final flush), so the row commits, or rolls back, with
 * the change.
 *
 * <p>Payloads carry identifiers, status and classification only; personal data (TIN, contact
 * details, birth date) stays in the application.
 */
@Component
public class EntityChangeCapture implements SmartInitializingSingleton {

  private final EntityManagerFactory entityManagerFactory;
  private final OutboxEventPublisher publisher;
  private final OutboxStore store;
  private final OutboxRelay relay;
  private final EventsProperties properties;

  /**
   * Creates the capture.
   *
   * @param entityManagerFactory JPA (Hibernate) entity manager factory
   * @param publisher builds the outbox rows
   * @param store outbox store
   * @param relay outbox relay (woken after commit)
   * @param properties Kafka switch
   */
  public EntityChangeCapture(
      EntityManagerFactory entityManagerFactory,
      OutboxEventPublisher publisher,
      OutboxStore store,
      OutboxRelay relay,
      EventsProperties properties) {
    this.entityManagerFactory = entityManagerFactory;
    this.publisher = publisher;
    this.store = store;
    this.relay = relay;
    this.properties = properties;
  }

  @Override
  public void afterSingletonsInstantiated() {
    EventListenerRegistry registry =
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getEventEngine()
            .getListenerRegistry();
    Listener listener = new Listener();
    registry.appendListeners(EventType.POST_INSERT, listener);
    registry.appendListeners(EventType.POST_UPDATE, listener);
  }

  /**
   * The event of a new entity, if it is one of the captured types.
   *
   * @param entity inserted entity
   * @return event, null for other entities
   */
  static IntegrationEvent inserted(Object entity) {
    if (entity instanceof Client client) {
      return clientEvent(client, IntegrationTopics.TYPE_CLIENT_REGISTERED, List.of());
    }
    if (entity instanceof Receipt receipt) {
      return receiptEvent(receipt);
    }
    return null;
  }

  /**
   * The event of a changed entity, if it is one of the captured types.
   *
   * @param entity updated entity
   * @param changedFields names of the changed properties
   * @return event, null for other entities
   */
  static IntegrationEvent updated(Object entity, List<String> changedFields) {
    if (entity instanceof Client client) {
      return clientEvent(client, IntegrationTopics.TYPE_CLIENT_CHANGED, changedFields);
    }
    return null;
  }

  private static IntegrationEvent clientEvent(Client c, String type, List<String> changed) {
    return new IntegrationEvent(
        IntegrationTopics.CLIENT_CHANGED,
        type,
        String.valueOf(c.getId()),
        c.getCompanyId(),
        new ClientPayload(
            c.getId(),
            c.getProspectCode(),
            c.getClientCode(),
            c.getClientType() == null ? null : c.getClientType().name(),
            c.getDisplayName(),
            c.getStatus() == null ? null : c.getStatus().name(),
            c.getKycStatus() == null ? null : c.getKycStatus().name(),
            c.getMarketSegment(),
            c.getPartyCode(),
            changed));
  }

  private static IntegrationEvent receiptEvent(Receipt r) {
    return new IntegrationEvent(
        IntegrationTopics.RECEIPT_ISSUED,
        IntegrationTopics.TYPE_RECEIPT_ISSUED,
        r.getReceiptNo() == null ? String.valueOf(r.getId()) : r.getReceiptNo(),
        r.getCompanyId(),
        new ReceiptPayload(
            r.getId(),
            r.getReceiptNo(),
            r.getKind() == null ? null : r.getKind().name(),
            r.getReceiptClass(),
            r.getReceiptDate(),
            r.getPayorCode(),
            r.getCurrency(),
            r.getAmount(),
            r.getMode() == null ? null : r.getMode().name(),
            r.getSourceModule(),
            r.getSourceRef()));
  }

  private void capture(EventSource session, IntegrationEvent event) {
    NewOutboxRow row = publisher.row(event);
    session
        .getActionQueue()
        .registerProcess(
            (BeforeTransactionCompletionProcess)
                s ->
                    s.doWork(
                        connection ->
                            store.insert(
                                new JdbcTemplate(new SingleConnectionDataSource(connection, true)),
                                row)));
    if (properties.enabled() && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              relay.requestDrain();
            }
          });
    }
  }

  /** Hibernate listener of the captured entity types. */
  private final class Listener implements PostInsertEventListener, PostUpdateEventListener {

    @Override
    public void onPostInsert(PostInsertEvent event) {
      IntegrationEvent integration = inserted(event.getEntity());
      if (integration != null) {
        capture(event.getSession(), integration);
      }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
      if (!(event.getEntity() instanceof Client)) {
        return;
      }
      List<String> changed = new ArrayList<>();
      String[] names = event.getPersister().getPropertyNames();
      int[] dirty = event.getDirtyProperties();
      if (dirty != null) {
        for (int index : dirty) {
          changed.add(names[index]);
        }
      }
      capture(event.getSession(), updated(event.getEntity(), changed));
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
      return false;
    }
  }

  /**
   * Payload of {@code crm.client.registered} / {@code crm.client.changed}.
   *
   * @param clientId client id
   * @param prospectCode prospect code
   * @param clientCode client code (after confirmation)
   * @param clientType INDIVIDUAL or CORPORATE
   * @param displayName display name
   * @param status client status
   * @param kycStatus KYC status
   * @param marketSegment market segment
   * @param partyCode party of the client
   * @param changedFields properties changed (empty for a new client)
   */
  public record ClientPayload(
      Long clientId,
      String prospectCode,
      String clientCode,
      String clientType,
      String displayName,
      String status,
      String kycStatus,
      String marketSegment,
      String partyCode,
      List<String> changedFields) {}

  /**
   * Payload of {@code cashiering.receipt.issued}.
   *
   * @param receiptId receipt id
   * @param receiptNo receipt number
   * @param kind AR or OR
   * @param receiptClass receipt class
   * @param receiptDate receipt date
   * @param payorCode payor
   * @param currency currency
   * @param amount amount
   * @param mode payment mode
   * @param sourceModule source module
   * @param sourceRef source reference
   */
  public record ReceiptPayload(
      Long receiptId,
      String receiptNo,
      String kind,
      String receiptClass,
      LocalDate receiptDate,
      String payorCode,
      String currency,
      BigDecimal amount,
      String mode,
      String sourceModule,
      String sourceRef) {}
}
