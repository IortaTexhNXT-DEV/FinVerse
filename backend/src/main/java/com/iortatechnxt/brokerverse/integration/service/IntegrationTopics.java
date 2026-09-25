package com.iortatechnxt.brokerverse.integration.service;

import com.iortatechnxt.brokerverse.events.service.IntegrationTopic;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The topic catalogue of the business integration events (version 1 of each). A breaking change of
 * a payload is a new topic version ({@code .v2}) published next to the old one until its consumers
 * have moved.
 */
@Configuration(proxyBeanMethods = false)
public class IntegrationTopics {

  /** An invoice was booked (booking, endorsement, cancellation, policy year). */
  public static final String INVOICE_BOOKED = "bibs.booking.invoice-booked.v1";

  /** A payment was applied to (or taken off) an invoice. */
  public static final String PAYMENT_APPLIED = "bibs.cashiering.payment-applied.v1";

  /** An acknowledgement or official receipt was issued. */
  public static final String RECEIPT_ISSUED = "bibs.cashiering.receipt-issued.v1";

  /** A remittance batch moved to another stage. */
  public static final String REMITTANCE_BATCH_STATUS = "bibs.remittance.batch-status.v1";

  /** A payment request / disbursement voucher changed status. */
  public static final String DISBURSEMENT_STATUS = "bibs.disbursement.status-changed.v1";

  /** Collections has items ready in an inbound feed. */
  public static final String COLLECTION_FEED_READY = "bibs.collections.feed-ready.v1";

  /** A package product version was validated and released. */
  public static final String PRODUCT_VERSION_RELEASED = "bibs.catalog.product-version-released.v1";

  /** A client was registered (created) or changed. */
  public static final String CLIENT_CHANGED = "bibs.crm.client-changed.v1";

  /** An e-mail notification was queued for delivery. */
  public static final String NOTIFICATION_REQUESTED = "bibs.messaging.notification-requested.v1";

  /** Event type of {@link #INVOICE_BOOKED}. */
  public static final String TYPE_INVOICE_BOOKED = "booking.invoice.booked";

  /** Event type of {@link #PAYMENT_APPLIED}: applied. */
  public static final String TYPE_PAYMENT_APPLIED = "cashiering.payment.applied";

  /** Event type of {@link #PAYMENT_APPLIED}: application reversed. */
  public static final String TYPE_PAYMENT_UNAPPLIED = "cashiering.payment.unapplied";

  /** Event type of {@link #RECEIPT_ISSUED}. */
  public static final String TYPE_RECEIPT_ISSUED = "cashiering.receipt.issued";

  /** Event type of {@link #REMITTANCE_BATCH_STATUS}. */
  public static final String TYPE_REMITTANCE_BATCH_STATUS = "remittance.batch.status-changed";

  /** Event type of {@link #DISBURSEMENT_STATUS}. */
  public static final String TYPE_DISBURSEMENT_STATUS = "disbursement.request.status-changed";

  /** Event type of {@link #COLLECTION_FEED_READY}. */
  public static final String TYPE_COLLECTION_FEED_READY = "collections.feed.ready";

  /** Event type of {@link #PRODUCT_VERSION_RELEASED}. */
  public static final String TYPE_PRODUCT_VERSION_RELEASED = "catalog.product-version.released";

  /** Event type of {@link #CLIENT_CHANGED}: new client. */
  public static final String TYPE_CLIENT_REGISTERED = "crm.client.registered";

  /** Event type of {@link #CLIENT_CHANGED}: changed client. */
  public static final String TYPE_CLIENT_CHANGED = "crm.client.changed";

  /** Event type of {@link #NOTIFICATION_REQUESTED}. */
  public static final String TYPE_NOTIFICATION_REQUESTED = "messaging.notification.requested";

  /**
   * Invoice booked.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic invoiceBookedTopic() {
    return new IntegrationTopic(
        INVOICE_BOOKED,
        List.of(TYPE_INVOICE_BOOKED),
        "Booked invoices (BRNB.027): premium components, commission, shares; key invoice number");
  }

  /**
   * Payment applied.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic paymentAppliedTopic() {
    return new IntegrationTopic(
        PAYMENT_APPLIED,
        List.of(TYPE_PAYMENT_APPLIED, TYPE_PAYMENT_UNAPPLIED),
        "Payments applied to or taken off invoices (invoice ledger); key invoice number");
  }

  /**
   * Receipt issued.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic receiptIssuedTopic() {
    return new IntegrationTopic(
        RECEIPT_ISSUED,
        List.of(TYPE_RECEIPT_ISSUED),
        "Acknowledgement and official receipts issued by cashiering; key receipt number");
  }

  /**
   * Remittance batch status.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic remittanceBatchStatusTopic() {
    return new IntegrationTopic(
        REMITTANCE_BATCH_STATUS,
        List.of(TYPE_REMITTANCE_BATCH_STATUS),
        "Stage changes of remittance batches (RMTID.019/036); key batch number");
  }

  /**
   * Disbursement status.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic disbursementStatusTopic() {
    return new IntegrationTopic(
        DISBURSEMENT_STATUS,
        List.of(TYPE_DISBURSEMENT_STATUS),
        "Status of payment requests, DV stages and instruments (DIS 2.8, 3.26); key request number");
  }

  /**
   * Collection feed ready.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic collectionFeedReadyTopic() {
    return new IntegrationTopic(
        COLLECTION_FEED_READY,
        List.of(TYPE_COLLECTION_FEED_READY),
        "Inbound COLLECTION_* feeds with items ready to pull (COLLECTIONS_DESIGN 9); key feed code");
  }

  /**
   * Product version released.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic productVersionReleasedTopic() {
    return new IntegrationTopic(
        PRODUCT_VERSION_RELEASED,
        List.of(TYPE_PRODUCT_VERSION_RELEASED),
        "Package versions validated and released (PMADD06, BRPM.015); key product code");
  }

  /**
   * Client registered or changed.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic clientChangedTopic() {
    return new IntegrationTopic(
        CLIENT_CHANGED,
        List.of(TYPE_CLIENT_REGISTERED, TYPE_CLIENT_CHANGED),
        "Clients registered or changed (status, KYC, identity); key client id");
  }

  /**
   * Notification requested.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic notificationRequestedTopic() {
    return new IntegrationTopic(
        NOTIFICATION_REQUESTED,
        List.of(TYPE_NOTIFICATION_REQUESTED),
        "E-mails queued for delivery; consumed by the e-mail dispatcher; key message id");
  }
}
