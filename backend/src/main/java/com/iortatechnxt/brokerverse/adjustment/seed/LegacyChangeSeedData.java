package com.iortatechnxt.brokerverse.adjustment.seed;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.PostingBatchService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.adjustment.service.RequestWorkflowService;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * SEED PROFILE ONLY: a change made in BIBS to a migrated invoice (conformance gap G03), after the
 * migration seed (order 95): a premium rate increase of the first migrated booking invoice is
 * raised by Marketing Collection, validated, approved and posted, so the report of the changes to
 * legacy invoices has a row from day one. Idempotent: nothing is raised when the invoice has an
 * endorsement already.
 */
@Component
@Profile("seed")
@Order(96)
public class LegacyChangeSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyChangeSeedData.class);
  private static final String COMPANY = "FVI";
  private static final String REQUESTER = "mktcoll";
  private static final String PROCESSOR = "adjust";
  private static final String APPROVER = "adjtl";
  private static final int CANDIDATES = 50;

  private final CompanyRepository companies;
  private final InvoiceLedgerQueryService ledger;
  private final EndorsementRequestService requests;
  private final RequestWorkflowService workflow;
  private final PostingBatchService batches;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param ledger Operations ledger
   * @param requests raise requests
   * @param workflow submit, validate, approve
   * @param batches posting
   * @param users seed sign-in
   */
  public LegacyChangeSeedData(
      CompanyRepository companies,
      InvoiceLedgerQueryService ledger,
      EndorsementRequestService requests,
      RequestWorkflowService workflow,
      PostingBatchService batches,
      SeedUsers users) {
    this.companies = companies;
    this.ledger = ledger;
    this.requests = requests;
    this.workflow = workflow;
    this.batches = batches;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    companies
        .findByCode(COMPANY)
        .map(Company::getId)
        .flatMap(this::migrated)
        .ifPresent(this::change);
  }

  private Optional<OpsInvoice> migrated(Long companyId) {
    LedgerSearch search =
        new LedgerSearch(
            companyId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            RecordOrigin.Origin.MIGRATED);
    return ledger.searchLoaded(search, PageRequest.of(0, CANDIDATES)).stream()
        .filter(i -> i.getKind() == InvoiceKind.BOOKING && !i.isCancelled())
        .filter(i -> i.premiumBalance().signum() > 0)
        .findFirst();
  }

  private void change(OpsInvoice invoice) {
    if (ledger.family(invoice.getInvoiceNo()).size() > 1) {
      return;
    }
    try {
      EndorsementRequest r =
          users.as(
              REQUESTER,
              () ->
                  requests.create(
                      new RequestDraft(
                          invoice.getInvoiceNo(),
                          AdjustmentSeedData.terms(
                              invoice,
                              "FIN_PREMIUM_RATE",
                              "PREMIUM_RATE_CHANGE",
                              null,
                              "Rate corrected after the migration: the legacy rate omitted the"
                                  + " flood loading"),
                          new AmountInput(
                              AdjustmentSeedData.RATE_INCREASE,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null),
                          null,
                          null)));
      users.as(REQUESTER, () -> workflow.submit(r.getId(), null));
      users.as(PROCESSOR, () -> workflow.validate(r.getId(), "Insurer's endorsement checked"));
      users.as(APPROVER, () -> workflow.approve(r.getId(), null));
      users.as(
          PROCESSOR,
          () -> batches.post(invoice.getCompanyId(), List.of(r.getId()), "Daily posting"));
      LOG.info(
          "Adjustment seed: legacy invoice {} changed by {}",
          invoice.getInvoiceNo(),
          r.getRequestNo());
    } catch (RuntimeException ex) {
      LOG.warn("Adjustment seed legacy change skipped: {}", ex.getMessage());
    }
  }
}
