package com.iortatechnxt.brokerverse.integration.service;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReleased;
import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes remittance batch stage changes ({@code bibs.remittance.batch-status.v1}, from the
 * workflow transitions of {@code OPS_REMITTANCE}) and released package versions ({@code
 * bibs.catalog.product-version-released.v1}).
 */
@Component
public class WorkflowAndCatalogEventAdapter {

  private final IntegrationEventPublisher publisher;
  private final RemittanceBatchRepository batches;

  /**
   * Creates the adapter.
   *
   * @param publisher integration event publisher
   * @param batches remittance batches (number and company)
   */
  public WorkflowAndCatalogEventAdapter(
      IntegrationEventPublisher publisher, RemittanceBatchRepository batches) {
    this.publisher = publisher;
    this.batches = batches;
  }

  /**
   * A remittance batch moved to another stage.
   *
   * @param event workflow transition (other record types are ignored)
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
  public void on(WorkCaseTransitioned event) {
    if (!BatchService.ENTITY.equals(event.entityType())) {
      return;
    }
    Optional<RemittanceBatch> batch = batchOf(event.entityId());
    String batchNo = batch.map(RemittanceBatch::getBatchNo).orElse(event.entityId());
    publisher.publish(
        new IntegrationEvent(
            IntegrationTopics.REMITTANCE_BATCH_STATUS,
            IntegrationTopics.TYPE_REMITTANCE_BATCH_STATUS,
            batchNo,
            batch.map(RemittanceBatch::getCompanyId).orElse(null),
            new RemittanceBatchStatusPayload(
                batchNo,
                batch.map(RemittanceBatch::getInsurerCode).orElse(null),
                event.fromStage(),
                event.toStage(),
                event.action(),
                event.reasonCode())));
  }

  /**
   * A package version was released.
   *
   * @param event release
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
  public void on(ProductVersionReleased event) {
    publisher.publish(
        new IntegrationEvent(
            IntegrationTopics.PRODUCT_VERSION_RELEASED,
            IntegrationTopics.TYPE_PRODUCT_VERSION_RELEASED,
            event.productCode(),
            null,
            new ProductVersionReleasedPayload(
                event.productCode(),
                event.versionNo(),
                event.effectiveFrom(),
                event.sourceRequestNo(),
                event.validatedBy())));
  }

  private Optional<RemittanceBatch> batchOf(String entityId) {
    try {
      return batches.findById(Long.valueOf(entityId));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  /**
   * Payload of {@code remittance.batch.status-changed}.
   *
   * @param batchNo batch number
   * @param insurerCode insurer
   * @param fromStage previous stage
   * @param toStage new stage
   * @param action workflow action
   * @param reasonCode reason, may be null
   */
  public record RemittanceBatchStatusPayload(
      String batchNo,
      String insurerCode,
      String fromStage,
      String toStage,
      String action,
      String reasonCode) {}

  /**
   * Payload of {@code catalog.product-version.released}.
   *
   * @param productCode risk code
   * @param versionNo released version
   * @param effectiveFrom date the version sells from
   * @param sourceRequestNo package request, null for a catalog-only version
   * @param validatedBy validator
   */
  public record ProductVersionReleasedPayload(
      String productCode,
      int versionNo,
      LocalDate effectiveFrom,
      String sourceRequestNo,
      String validatedBy) {}
}
