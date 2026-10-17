package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.brokerclaims.service.ClaimExperienceQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.service.ClaimExperienceQueryService.ClaimLine;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.HistoryView;
import com.iortatechnxt.brokerverse.renewal.domain.HistoryViewRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The account history of a renewal (BRRN.027; FR-RN-042), read-only and sourced from the mother
 * policy in BIBS, not from the extraction: the chain of prior renewals, the endorsements and the
 * payments of the expiring invoice family, and the claims of the account (Claims module; "claims
 * not connected" while it is absent, decision D4). Opening it records the view with the user, time
 * and sections: the disposition is refused until the user has viewed it. History before BIBS (EBIX
 * / QPS) is not shown.
 */
@Service
@Transactional
public class AccountHistoryService {

  private static final int MAX_CHAIN = 10;
  private static final String SECTIONS = "renewals,endorsements,payments,claims";
  private static final Set<MovementType> PAYMENTS =
      EnumSet.of(MovementType.APPLIED, MovementType.UNAPPLIED, MovementType.DP_REVERSAL);

  private final RenewalRecords records;
  private final RenewalCandidateRepository candidates;
  private final InvoiceLedgerQueryService ledger;
  private final ObjectProvider<ClaimExperienceQueryService> claims;
  private final HistoryViewRepository views;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param records renewals in scope
   * @param candidates candidates (prior renewals)
   * @param ledger Operations ledger
   * @param claims Claims read API, when deployed
   * @param views history views
   * @param audit audit trail
   * @param currentUser current user
   */
  public AccountHistoryService(
      RenewalRecords records,
      RenewalCandidateRepository candidates,
      InvoiceLedgerQueryService ledger,
      ObjectProvider<ClaimExperienceQueryService> claims,
      HistoryViewRepository views,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.records = records;
    this.candidates = candidates;
    this.ledger = ledger;
    this.claims = claims;
    this.views = views;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * Opens the account history of a renewal and records the view.
   *
   * @param companyId company
   * @param renewalRef renewal reference
   * @return the history
   */
  public AccountHistory open(Long companyId, String renewalRef) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    views.save(new HistoryView(c.getId(), SECTIONS));
    c.markHistoryViewed();
    audit.record(
        RenewalCodes.ENTITY, c.getRenewalRef(), AuditAction.OPEN, "Account history viewed");
    return history(c);
  }

  /**
   * Whether the current user opened the account history (BRRN.027 AC 4).
   *
   * @param candidate candidate
   * @return true when viewed
   */
  @Transactional(readOnly = true)
  public boolean viewedByCurrentUser(RenewalCandidate candidate) {
    return views.existsByCandidateIdAndCreatedBy(candidate.getId(), currentUser.username());
  }

  private AccountHistory history(RenewalCandidate c) {
    List<OpsInvoice> family =
        c.getExpiringInvoiceNo() == null ? List.of() : ledger.family(c.getExpiringInvoiceNo());
    List<Endorsement> endorsements = new ArrayList<>();
    List<Payment> payments = new ArrayList<>();
    for (OpsInvoice invoice : family) {
      if (invoice.getKind() != InvoiceKind.BOOKING) {
        endorsements.add(
            new Endorsement(
                invoice.getInvoiceNo(),
                invoice.getKind().name(),
                invoice.getEndorsementNo(),
                invoice.getBookingDate(),
                invoice.getGrossPremium()));
      }
      for (OpsInvoiceMovement m : ledger.movements(invoice.getInvoiceNo())) {
        if (PAYMENTS.contains(m.getMovementType())) {
          payments.add(
              new Payment(
                  invoice.getInvoiceNo(),
                  m.getValueDate(),
                  m.getMovementType().name(),
                  m.getComponent().name(),
                  m.getAmount(),
                  m.getOrNo()));
        }
      }
    }
    return new AccountHistory(priorRenewals(c), endorsements, payments, claims(c));
  }

  private List<PriorRenewal> priorRenewals(RenewalCandidate c) {
    List<PriorRenewal> chain = new ArrayList<>();
    String arn = c.getExpiringArn();
    for (int i = 0; arn != null && i < MAX_CHAIN; i++) {
      Optional<RenewalCandidate> prior = candidates.findFirstByRenewalArn(arn);
      if (prior.isEmpty()) {
        break;
      }
      RenewalCandidate p = prior.get();
      chain.add(
          new PriorRenewal(
              p.getRenewalRef(),
              p.getExpiringArn(),
              arn,
              p.getExpiryDate(),
              p.getClosedAs() == null ? p.getStage().name() : p.getClosedAs().name()));
      arn = p.getExpiringArn();
    }
    return chain;
  }

  private Claims claims(RenewalCandidate c) {
    ClaimExperienceQueryService service = claims.getIfAvailable();
    if (service == null || c.getExpiringArn() == null) {
      return new Claims(false, 0, 0, List.of());
    }
    var experience = service.summary(c.getExpiringArn(), null);
    return new Claims(
        true, experience.claimCount(), experience.openCount(), List.copyOf(experience.claims()));
  }

  /**
   * The account history.
   *
   * @param priorRenewals prior renewals, latest first
   * @param endorsements endorsements of the expiring invoice family
   * @param payments payments of the family
   * @param claims claims of the account
   */
  public record AccountHistory(
      List<PriorRenewal> priorRenewals,
      List<Endorsement> endorsements,
      List<Payment> payments,
      Claims claims) {}

  /**
   * A prior renewal.
   *
   * @param renewalRef renewal reference
   * @param expiringArn ARN it renewed
   * @param renewalArn ARN it created
   * @param expiry expiry of the policy it renewed
   * @param outcome outcome
   */
  public record PriorRenewal(
      String renewalRef, String expiringArn, String renewalArn, LocalDate expiry, String outcome) {}

  /**
   * An endorsement or cancellation invoice.
   *
   * @param invoiceNo invoice
   * @param kind kind
   * @param endorsementNo endorsement number
   * @param bookedOn booking date
   * @param grossPremium gross premium
   */
  public record Endorsement(
      String invoiceNo,
      String kind,
      String endorsementNo,
      LocalDate bookedOn,
      BigDecimal grossPremium) {}

  /**
   * A payment movement.
   *
   * @param invoiceNo invoice
   * @param valueDate value date
   * @param type movement type
   * @param component component
   * @param amount amount
   * @param orNo official receipt
   */
  public record Payment(
      String invoiceNo,
      LocalDate valueDate,
      String type,
      String component,
      BigDecimal amount,
      String orNo) {}

  /**
   * Claims of the account.
   *
   * @param connected whether the Claims module answered
   * @param count claims
   * @param open open claims
   * @param lines the claims
   */
  public record Claims(boolean connected, int count, int open, List<ClaimLine> lines) {}
}
