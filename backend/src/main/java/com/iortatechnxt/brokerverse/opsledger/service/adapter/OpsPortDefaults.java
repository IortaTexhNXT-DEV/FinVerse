package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffRefundValidationSource;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;
import com.iortatechnxt.brokerverse.opsledger.service.port.InsurerFileInbox;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceCorrectionSink;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReversalRequester;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapters of the Operations ports (OPERATIONS_DESIGN sections 2.1 and 8). Each one is
 * registered only when no module provides the port, so an Operations module replaces a default by
 * declaring its implementation as a component-scanned bean ({@code @Service} / {@code @Component})
 * and each module's tests run without the others. The Collections ports (COLLECTIONS_DESIGN section
 * 9) and the Accounting / Disbursement ports (ACCOUNTING_DISBURSEMENT_DESIGN 2.2) follow the same
 * rule.
 */
@Configuration(proxyBeanMethods = false)
public class OpsPortDefaults {

  /**
   * OR issuing by hand-off until cashiering implements {@link ReceiptIssuer}.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(ReceiptIssuer.class)
  public ReceiptIssuer defaultReceiptIssuer(HandoffService handoffs, ObjectMapper json) {
    return new HandoffReceiptIssuer(handoffs, json);
  }

  /**
   * Refuses re-application of paid invoices until cashiering implements {@link PaymentReapplier}.
   *
   * @param ledger ledger reads
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(PaymentReapplier.class)
  public PaymentReapplier defaultPaymentReapplier(InvoiceLedgerQueryService ledger) {
    return new LedgerPaymentReapplier(ledger);
  }

  /**
   * Unapplied items by hand-off until cashiering implements {@link UnappliedSink}.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(UnappliedSink.class)
  public UnappliedSink defaultUnappliedSink(HandoffService handoffs, ObjectMapper json) {
    return new HandoffUnappliedSink(handoffs, json);
  }

  /**
   * The in-app Disbursement queue until the Disbursement system is known (OQ02).
   *
   * @param queue queue
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(DisbursementGateway.class)
  public DisbursementGateway defaultDisbursementGateway(DisbursementQueueService queue) {
    return new QueueDisbursementGateway(queue);
  }

  /**
   * Manual transport of the Collection feeds (OQ01, OQ45).
   *
   * @param flowIn flow-in runs
   * @param repository extract repository
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(CollectionFeed.class)
  public CollectionFeed defaultCollectionFeed(
      FlowInService flowIn, ExtractRepositoryService repository) {
    return new ManualCollectionFeed(flowIn, repository);
  }

  /**
   * No insurer inbox: files are uploaded by users (OQ22, OQ29, OQ38).
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(InsurerFileInbox.class)
  public InsurerFileInbox defaultInsurerFileInbox() {
    return new ManualInsurerFileInbox();
  }

  /**
   * The in-system extract repository in place of the shared drive (OQ17).
   *
   * @param repository extract repository
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(FileDropPort.class)
  public FileDropPort defaultFileDrop(ExtractRepositoryService repository) {
    return new RepositoryFileDrop(repository);
  }

  /**
   * No early remittance incentive rule while remittance is not installed (OQ23).
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(EarlyIncentiveRules.class)
  public EarlyIncentiveRules defaultEarlyIncentiveRules() {
    return (companyId, subject) -> Optional.empty();
  }

  /**
   * No unapplied items to show to collectors until cashiering implements {@link UnappliedDirectory}
   * (BRCLXN.034-036).
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(UnappliedDirectory.class)
  public UnappliedDirectory defaultUnappliedDirectory() {
    return new EmptyUnappliedDirectory();
  }

  /**
   * Collector disposition requests by hand-off until cashiering implements {@link
   * UnappliedDispositionRequests} (BRCLXN.030-033).
   *
   * @param handoffs hand-offs
   * @param json JSON mapper
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(UnappliedDispositionRequests.class)
  public UnappliedDispositionRequests defaultUnappliedDispositionRequests(
      HandoffService handoffs, ObjectMapper json) {
    return new HandoffDispositionRequests(handoffs, json);
  }

  /**
   * Refund validations by hand-off while neither acsl nor cashiering validates refunds (MKT 1.11.0,
   * ACSL 2.5.5). Callers go through {@code RefundValidations}, which also hands over the validator
   * whose module is missing once the other one is installed.
   *
   * @param handoffs hand-offs
   * @param json JSON mapper
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(RefundValidationSource.class)
  public RefundValidationSource defaultRefundValidationSource(
      HandoffService handoffs, ObjectMapper json) {
    return new HandoffRefundValidationSource(handoffs, json);
  }

  /**
   * Payment reversals by hand-off until cashiering implements {@link PaymentReversalRequester}
   * (ACSL 2.6.0-2.6.1).
   *
   * @param handoffs hand-offs
   * @param json JSON mapper
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(PaymentReversalRequester.class)
  public PaymentReversalRequester defaultPaymentReversalRequester(
      HandoffService handoffs, ObjectMapper json) {
    return new HandoffPaymentReversalRequester(handoffs, json);
  }

  /**
   * ACSL corrections recorded as CORRECTION movements of the invoice ledger (ACSL 2.9.1, 2.16.0).
   *
   * @param ledger ledger postings
   * @param query ledger reads
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(InvoiceCorrectionSink.class)
  public InvoiceCorrectionSink defaultInvoiceCorrectionSink(
      InvoiceLedgerService ledger, InvoiceLedgerQueryService query) {
    return new LedgerInvoiceCorrectionSink(ledger, query);
  }
}
