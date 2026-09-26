package com.iortatechnxt.brokerverse.adjustment.seed;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RefundBasis;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.PostingBatchService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.adjustment.service.RequestWorkflowService;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seed storyline of Adjustment (seed profile only), after cashiering (order 91) and remittance
 * (order 92), through the real services and the SIT/UAT users, so the workbench shows a request at
 * every stage. Marketing Collection ({@code mktcoll}) raises the requests, the processor ({@code
 * adjust}) validates, returns and posts, the team leader ({@code adjtl}) approves:
 *
 * <ul>
 *   <li>on the property invoice of {@code ARN-2026-940002}: a descriptive change still in draft, a
 *       change of the assured's information waiting for validation, a cover extension returned for
 *       its documents, and a premium rate increase waiting for approval;
 *   <li>a flat cancellation of {@code ARN-2026-940004} ("unit sold"), validated and approved: ready
 *       for the posting batch;
 *   <li>an internal adjustment of the remitted invoice of {@code ARN-2026-940001}, posted.
 * </ul>
 *
 * Runs once (skipped when requests exist); a step that fails is logged and skipped.
 */
@Component
@Profile("seed")
@Order(93)
public class AdjustmentSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AdjustmentSeedData.class);
  private static final String REQUESTER = "mktcoll";
  private static final String PROCESSOR = "adjust";
  private static final int DAYS_AFTER_INCEPTION = 30;
  private static final BigDecimal RATE_INCREASE = new BigDecimal("1500.00");

  private final EndorsementRequestService requests;
  private final RequestWorkflowService workflow;
  private final PostingBatchService batches;
  private final EndorsementRequestRepository repository;
  private final InvoiceLedgerQueryService ledger;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param requests raise requests
   * @param workflow submit, validate, approve
   * @param batches return and post
   * @param repository requests (idempotency)
   * @param ledger Operations ledger
   * @param users seed sign-in
   */
  public AdjustmentSeedData(
      EndorsementRequestService requests,
      RequestWorkflowService workflow,
      PostingBatchService batches,
      EndorsementRequestRepository repository,
      InvoiceLedgerQueryService ledger,
      SeedUsers users) {
    this.requests = requests;
    this.workflow = workflow;
    this.batches = batches;
    this.repository = repository;
    this.ledger = ledger;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (repository.count() > 0) {
      return;
    }
    original("ARN-2026-940002").ifPresent(this::propertyRequests);
    original("ARN-2026-940004").ifPresent(this::cancellation);
    original("ARN-2026-940001").ifPresent(this::internalAdjustment);
  }

  private void propertyRequests(OpsInvoice invoice) {
    step(
        "descriptive change (draft)",
        () -> raise(invoice, "NF_DESCRIPTIVE", null, null, "Correct the property address"));
    step(
        "assured information change",
        () -> {
          EndorsementRequest r =
              raise(invoice, "NF_ASSURED_INFO", null, null, "Change of the assured's address");
          return users.as(REQUESTER, () -> workflow.submit(r.getId(), "Client letter attached"));
        });
    step(
        "cover extension (returned)",
        () -> {
          EndorsementRequest r =
              raise(invoice, "NF_COVER_EXTENSION", null, null, "Add the typhoon clause");
          users.as(REQUESTER, () -> workflow.submit(r.getId(), null));
          users.as(
              PROCESSOR,
              () ->
                  batches.returnRequests(
                      List.of(r.getId()),
                      "INCOMPLETE_DOCUMENTS",
                      "Attach the insurer's clause wording"));
          return r;
        });
    step(
        "premium rate increase (for approval)",
        () -> {
          EndorsementRequest r =
              users.as(
                  REQUESTER,
                  () ->
                      requests.create(
                          new RequestDraft(
                              invoice.getInvoiceNo(),
                              terms(
                                  invoice,
                                  "FIN_PREMIUM_RATE",
                                  "PREMIUM_RATE_CHANGE",
                                  null,
                                  "Rate increased after the insurer's survey"),
                              new AmountInput(
                                  RATE_INCREASE, null, null, null, null, null, null, null),
                              null,
                              null)));
          users.as(REQUESTER, () -> workflow.submit(r.getId(), null));
          return users.as(PROCESSOR, () -> workflow.validate(r.getId(), "Survey report checked"));
        });
  }

  private void cancellation(OpsInvoice invoice) {
    step(
        "flat cancellation",
        () -> {
          EndorsementRequest r =
              raise(
                  invoice,
                  "FIN_CHANGE_COVER",
                  "FLAT_CANCELLATION",
                  "UNIT_SOLD",
                  "Vehicle sold before the cover started; flat cancellation");
          users.as(REQUESTER, () -> workflow.submit(r.getId(), null));
          users.as(PROCESSOR, () -> workflow.validate(r.getId(), "Deed of sale checked"));
          return users.as("adjtl", () -> workflow.approve(r.getId(), null));
        });
  }

  private void internalAdjustment(OpsInvoice invoice) {
    step(
        "internal adjustment (posted)",
        () -> {
          EndorsementRequest r =
              raise(invoice, "INT_ADJUSTMENT", null, null, "Correct the cost center of the AO");
          users.as(REQUESTER, () -> workflow.submit(r.getId(), null));
          users.as(PROCESSOR, () -> workflow.validate(r.getId(), null));
          users.as(
              PROCESSOR,
              () -> batches.post(invoice.getCompanyId(), List.of(r.getId()), "Seed posting"));
          return r;
        });
  }

  private EndorsementRequest raise(
      OpsInvoice invoice, String type, String requestType, String reason, String description) {
    return users.as(
        REQUESTER,
        () ->
            requests.create(
                new RequestDraft(
                    invoice.getInvoiceNo(),
                    terms(invoice, type, requestType, reason, description),
                    AmountInput.NONE,
                    null,
                    null)));
  }

  private static RequestTerms terms(
      OpsInvoice invoice, String type, String requestType, String reason, String description) {
    boolean cancellation = reason != null;
    return new RequestTerms(
        type,
        requestType,
        reason,
        "SEED-" + type,
        cancellation
            ? invoice.getClassification().inceptionDate()
            : invoice.getClassification().inceptionDate().plusDays(DAYS_AFTER_INCEPTION),
        RefundBasis.PRO_RATA,
        null,
        null,
        null,
        null,
        description,
        null);
  }

  private Optional<OpsInvoice> original(String arn) {
    return ledger.forArn(arn).stream().filter(i -> i.getKind() == InvoiceKind.BOOKING).findFirst();
  }

  private static void step(String name, Supplier<EndorsementRequest> action) {
    try {
      EndorsementRequest r = action.get();
      LOG.info("Adjustment seed: {} {}", name, r.getRequestNo());
    } catch (RuntimeException ex) {
      LOG.warn("Adjustment seed {} skipped: {}", name, ex.getMessage());
    }
  }
}
