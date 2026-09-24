package com.iortatechnxt.brokerverse.adjustment.demo;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RefundBasis;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.adjustment.service.RequestWorkflowService;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Demo storyline of Adjustment (demo profile only), after the booking demo (order 80) and the
 * ledger replay (order 90), through the real services and the demo users:
 *
 * <ul>
 *   <li>Marketing Collection ({@code mktcoll}) asks for a change of the assured's information on
 *       the invoice of {@code ARN-2026-940003}: submitted, waiting for validation;
 *   <li>and a flat cancellation of {@code ARN-2026-940004} ("unit sold"), validated by {@code
 *       adjust} and approved by {@code adjtl}: ready for the posting batch.
 * </ul>
 *
 * Runs once (skipped when requests exist); a step that fails is logged and skipped.
 */
@Component
@Profile("demo")
@Order(95)
public class AdjustmentDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AdjustmentDemoData.class);
  private static final String REQUESTER = "mktcoll";
  private static final int DAYS_AFTER_INCEPTION = 30;

  private final EndorsementRequestService requests;
  private final RequestWorkflowService workflow;
  private final EndorsementRequestRepository repository;
  private final InvoiceLedgerQueryService ledger;
  private final UserDetailsService users;

  /**
   * Creates the loader.
   *
   * @param requests raise requests
   * @param workflow submit, validate, approve
   * @param repository requests (idempotency)
   * @param ledger Operations ledger
   * @param users demo users
   */
  public AdjustmentDemoData(
      EndorsementRequestService requests,
      RequestWorkflowService workflow,
      EndorsementRequestRepository repository,
      InvoiceLedgerQueryService ledger,
      UserDetailsService users) {
    this.requests = requests;
    this.workflow = workflow;
    this.repository = repository;
    this.ledger = ledger;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (repository.count() > 0) {
      return;
    }
    original("ARN-2026-940003").ifPresent(this::assuredChange);
    original("ARN-2026-940004").ifPresent(this::cancellation);
  }

  private void assuredChange(OpsInvoice invoice) {
    step(
        "assured information change",
        () -> {
          EndorsementRequest r =
              as(
                  REQUESTER,
                  () ->
                      requests.create(
                          draft(
                              invoice,
                              "NF_ASSURED_INFO",
                              null,
                              null,
                              "Change of the assured's mailing address")));
          return as(REQUESTER, () -> workflow.submit(r.getId(), "Client letter attached"));
        });
  }

  private void cancellation(OpsInvoice invoice) {
    step(
        "flat cancellation",
        () -> {
          EndorsementRequest r =
              as(
                  REQUESTER,
                  () ->
                      requests.create(
                          draft(
                              invoice,
                              "FIN_CHANGE_COVER",
                              "FLAT_CANCELLATION",
                              "UNIT_SOLD",
                              "Vehicle sold before the cover started; flat cancellation")));
          as(REQUESTER, () -> workflow.submit(r.getId(), null));
          as("adjust", () -> workflow.validate(r.getId(), "Deed of sale checked"));
          return as("adjtl", () -> workflow.approve(r.getId(), null));
        });
  }

  private static RequestDraft draft(
      OpsInvoice invoice, String type, String requestType, String reason, String description) {
    return new RequestDraft(
        invoice.getInvoiceNo(),
        new RequestTerms(
            type,
            requestType,
            reason,
            null,
            requestType == null
                ? invoice.getClassification().inceptionDate().plusDays(DAYS_AFTER_INCEPTION)
                : invoice.getClassification().inceptionDate(),
            RefundBasis.PRO_RATA,
            null,
            null,
            null,
            null,
            description,
            null),
        AmountInput.NONE,
        null,
        null);
  }

  private Optional<OpsInvoice> original(String arn) {
    return ledger.forArn(arn).stream().filter(i -> i.getKind() == InvoiceKind.BOOKING).findFirst();
  }

  private void step(String name, Supplier<EndorsementRequest> action) {
    try {
      EndorsementRequest r = action.get();
      LOG.info("Adjustment demo: {} {} is {}", name, r.getRequestNo(), r.getStage());
    } catch (RuntimeException ex) {
      LOG.warn("Adjustment demo {} skipped: {}", name, ex.getMessage());
    }
  }

  private <T> T as(String username, Supplier<T> action) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    var details = users.loadUserByUsername(username);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }
}
