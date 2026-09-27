package com.iortatechnxt.brokerverse.renewal.submitted;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckEngine;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotClient;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotMortgage;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotProduct;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotSales;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.extraction.service.CandidateFactory;
import com.iortatechnxt.brokerverse.renewal.rules.service.InitiationService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Renewal's side of the Submitted Policies hand-off (wave R3; RENEWAL_DESIGN section 2.3): a
 * masterlist record handed over becomes a renewal candidate of source {@code SUBMITTED_POLICY}
 * keyed by its masterlist number (idempotent: a record offered again gets its existing renewal),
 * snapshot from the policy, risk and loan data with the insurer the Submitted Policies rules
 * assigned, checked and initiated at once because the expiry scan is the initiation (BRRN.021). The
 * terms (RA template, handler, Account Officer, mailing address, Renew with BDOI) are kept in
 * {@code rnw_submitted_handoff} for the letters. From there the renewal follows the Renewal flows:
 * disposition, renewal account (origin SUBMITTED_POLICY, renewing the masterlist number), RA (Free
 * First Year variant when the record is FFY; printed through the mail house when the client has no
 * e-mail), placement and booking. The status query answers from the candidate. Each hand-off runs
 * in its own transaction; a failure is answered as REFUSED with the reason.
 */
@Component
public class SubmittedPolicyRenewalHandOff implements RenewalHandOff {

  private static final Logger LOG = LoggerFactory.getLogger(SubmittedPolicyRenewalHandOff.class);

  private final RenewalCandidateRepository candidates;
  private final SubmittedHandOffRecordRepository handOffs;
  private final Collaborators renewal;
  private final AuditTrailService audit;
  private final TransactionTemplate tx;

  /**
   * The Renewal services a hand-off goes through.
   *
   * @param factory candidate references
   * @param flow the renewal work case
   * @param engine checks
   * @param initiation initiation
   */
  public record Collaborators(
      CandidateFactory factory,
      RenewalFlow flow,
      CheckEngine engine,
      InitiationService initiation) {}

  /**
   * Creates the adapter.
   *
   * @param candidates renewals
   * @param handOffs hand-off terms
   * @param factory candidate references
   * @param flow the renewal work case
   * @param engine checks
   * @param initiation initiation
   * @param audit audit trail
   * @param transactions transaction manager
   */
  @SuppressWarnings("java:S107") // the Renewal steps of a hand-off
  public SubmittedPolicyRenewalHandOff(
      RenewalCandidateRepository candidates,
      SubmittedHandOffRecordRepository handOffs,
      CandidateFactory factory,
      RenewalFlow flow,
      CheckEngine engine,
      InitiationService initiation,
      AuditTrailService audit,
      PlatformTransactionManager transactions) {
    this.candidates = candidates;
    this.handOffs = handOffs;
    this.renewal = new Collaborators(factory, flow, engine, initiation);
    this.audit = audit;
    this.tx = new TransactionTemplate(transactions);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public HandOffResult handOff(HandOffRequest request) {
    try {
      RenewalCandidate c = tx.execute(s -> take(request));
      return c == null
          ? new HandOffResult(Outcome.REFUSED, null, null, "Not taken")
          : new HandOffResult(
              Outcome.HANDED_OFF,
              c.getRenewalRef(),
              c.getRenewalArn(),
              "Renewal " + c.getRenewalRef() + " " + c.getStage().label());
    } catch (RuntimeException ex) {
      LOG.warn("Hand-off of {} refused: {}", request.sbmNo(), ex.getMessage());
      return new HandOffResult(Outcome.REFUSED, null, null, ex.getMessage());
    }
  }

  @Override
  public Optional<HandOffStatus> status(Long companyId, String sbmNo) {
    return tx.execute(
        s ->
            candidates
                .findByCompanyIdAndSourceAndSourceRef(
                    companyId, CandidateSource.SUBMITTED_POLICY, sbmNo)
                .map(SubmittedPolicyRenewalHandOff::statusOf));
  }

  private RenewalCandidate take(HandOffRequest request) {
    Optional<RenewalCandidate> existing =
        candidates.findByCompanyIdAndSourceAndSourceRef(
            request.companyId(), CandidateSource.SUBMITTED_POLICY, request.sbmNo());
    if (existing.isPresent()) {
      return existing.get();
    }
    RenewalCandidate c =
        candidates.save(
            new RenewalCandidate(
                request.companyId(),
                renewal.factory().nextReference(),
                new RenewalCandidate.Origin(
                    CandidateSource.SUBMITTED_POLICY, request.sbmNo(), null, null, null),
                snapshot(request),
                null));
    RenewalTerms terms = request.terms();
    handOffs.save(
        new SubmittedHandOffRecord(
            request.companyId(),
            c.getId(),
            request.sbmNo(),
            new SubmittedHandOffRecord.Terms(
                terms.raTemplate(),
                terms.assignedInsurer(),
                terms.handlerUsername(),
                terms.aoUsername(),
                request.policy().contact() == null
                    ? null
                    : request.policy().contact().mailingAddress(),
                terms.manual())));
    renewal.flow().start(c);
    renewal.engine().run(c, CheckTrigger.EXTRACTION);
    renewal.initiation().initiateOne(c);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        "Handed over from submitted policy "
            + request.sbmNo()
            + (terms.manual() ? " (Renew with BDOI)" : ""));
    return c;
  }

  /**
   * The listing snapshot of a handed-over policy: the assigned insurer when the rules gave one.
   *
   * @param r hand-off request
   * @return snapshot
   */
  static CandidateSnapshot snapshot(HandOffRequest r) {
    PolicyData p = r.policy();
    String insurer =
        r.terms().assignedInsurer() != null ? r.terms().assignedInsurer() : p.insurerCode();
    String mortgagee = p.loan() == null ? null : p.loan().mortgagee();
    return new CandidateSnapshot(
        p.policyNo(),
        null,
        null,
        p.loan() == null ? null : p.loan().pnNo(),
        new SnapshotClient(
            null,
            p.cif(),
            p.borrowerName() != null ? p.borrowerName() : p.assuredName(),
            p.assuredName(),
            p.contact() == null ? null : p.contact().email()),
        new SnapshotProduct(null, null, null, p.segment(), "SUBMITTED_POLICY", null),
        new SnapshotSales(null, null, null, null, null, r.terms().aoUsername()),
        insurer,
        new SnapshotMortgage(mortgagee != null && !mortgagee.isBlank(), mortgagee),
        false,
        p.period().inception(),
        p.period().expiry(),
        new SnapshotPremium(null, p.totalPremium(), p.sumInsured(), null, null, "PHP"),
        null,
        null);
  }

  private static HandOffStatus statusOf(RenewalCandidate c) {
    boolean closed = c.getStage() == RenewalStage.CLOSED || c.getStage() == RenewalStage.RENEWED;
    State state = State.OPEN;
    if (closed) {
      ClosedAs how = c.getClosedAs() == null ? ClosedAs.RENEWED : c.getClosedAs();
      state =
          switch (how) {
            case RENEWED, BOOKED_OTHER_INVOICE -> State.RENEWED;
            case NOT_RENEWED -> State.NOT_RENEWED;
            case LOST -> State.LOST;
            case EXPIRED_UNRENEWED -> State.EXPIRED_UNRENEWED;
          };
    }
    return new HandOffStatus(
        state,
        c.getRenewalRef(),
        c.getRenewalArn(),
        closed && c.getDisposition() != null ? c.getDisposition().reasonCode() : null);
  }
}
