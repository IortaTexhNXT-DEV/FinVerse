package com.iortatechnxt.brokerverse.renewal.acceptance.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.check.service.BlockingChecks;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.check.service.FinancialImpactCheck;
import com.iortatechnxt.brokerverse.renewal.domain.AcceptanceMethod;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAcceptance;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAcceptanceRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client acceptance of the Renewal Advice (FR-RN-084; BRRN.038/040): by e-mail or signed RA (with
 * the file attached on the renewal) or by payment (with its reference). Acceptance is refused while
 * a blocking check fails; a failed financial-impact check needs the evidence and the user's
 * acknowledgement. Once accepted, the renewal account is fast-tracked to the payment gate.
 */
@Service
@Transactional
public class AcceptanceService {

  private final RenewalRecords records;
  private final RenewalAcceptanceRepository acceptances;
  private final BlockingChecks blocking;
  private final DocumentService documents;
  private final RenewalFlow flow;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param acceptances acceptances
   * @param blocking blocking checks
   * @param documents documents (evidence)
   * @param flow workflow
   * @param notices notifications
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public AcceptanceService(
      RenewalRecords records,
      RenewalAcceptanceRepository acceptances,
      BlockingChecks blocking,
      DocumentService documents,
      RenewalFlow flow,
      RenewalNotices notices,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.acceptances = acceptances;
    this.blocking = blocking;
    this.documents = documents;
    this.flow = flow;
    this.notices = notices;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records the acceptance of a renewal.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param input method and evidence
   * @param source USER, UPLOAD or SYSTEM
   * @return the acceptance
   */
  public RenewalAcceptance accept(Long companyId, String renewalRef, Input input, String source) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    RenewalRecords.requireStage(c, RenewalStage.RA_SENT);
    requireEvidence(c, input);
    boolean impact = requireChecks(c, input.financialImpactAck());
    LocalDate on = input.acceptedOn() == null ? BusinessClock.today(clock) : input.acceptedOn();
    if (on.isAfter(BusinessClock.today(clock))) {
      throw new BusinessRuleException(
          "RNW_ACCEPTANCE_DATE", "The acceptance date cannot be in the future");
    }
    RenewalAcceptance acceptance =
        acceptances.save(
            new RenewalAcceptance(
                c.getId(),
                input.method(),
                new RenewalAcceptance.Evidence(
                    input.attachmentId(), blank(input.reference()), on, blank(input.remarks())),
                source,
                impact));
    flow.act(c, "accept", TransitionNote.comment("Accepted by " + label(input.method())));
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.AUTHORIZE,
        "Acceptance by "
            + label(input.method())
            + " on "
            + on
            + " ("
            + source.toLowerCase(java.util.Locale.ROOT)
            + ")");
    List<String> owners = new ArrayList<>();
    owners.add(c.getAssignedAo());
    owners.add(c.getAssignedPo());
    notices.users(
        owners,
        RenewalCodes.EVENT_ACCEPTED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + " accepted by the client",
            c.getSnapshot().clientName() + " accepted the renewal by " + label(input.method())));
    return acceptance;
  }

  /**
   * The acceptances of a renewal, newest first.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return acceptances
   */
  @Transactional(readOnly = true)
  public List<RenewalAcceptance> of(Long companyId, String renewalRef) {
    return acceptances.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  private void requireEvidence(RenewalCandidate c, Input input) {
    if (input.method() == null) {
      throw new BusinessRuleException("RNW_ACCEPTANCE_METHOD", "Select the acceptance method");
    }
    String missing = missingEvidence(input);
    if (missing != null) {
      throw new BusinessRuleException("RNW_ACCEPTANCE_EVIDENCE", missing);
    }
    if (input.attachmentId() != null && !attached(c, input.attachmentId())) {
      throw new BusinessRuleException(
          "RNW_ACCEPTANCE_EVIDENCE", "The evidence is not a document of this renewal");
    }
  }

  private static String missingEvidence(Input input) {
    boolean referenced = blank(input.reference()) != null;
    if (input.method() == AcceptanceMethod.PAYMENT) {
      return referenced ? null : "Enter the reference of the payment";
    }
    boolean fileNeeded = input.method() == AcceptanceMethod.SIGNED_RA || !referenced;
    return fileNeeded && input.attachmentId() == null
        ? "Attach the client's e-mail or the signed Renewal Advice"
        : null;
  }

  private boolean attached(RenewalCandidate c, Long attachmentId) {
    return documents.list(new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString())).stream()
        .map(Attachment::getId)
        .anyMatch(attachmentId::equals);
  }

  /** Returns whether the financial-impact check failed (acknowledged). */
  private boolean requireChecks(RenewalCandidate c, boolean acknowledged) {
    List<CheckResult> failing = blocking.failing(c);
    boolean impact =
        failing.stream().anyMatch(r -> FinancialImpactCheck.CODE.equals(r.getCheckCode()));
    List<CheckResult> others =
        failing.stream().filter(r -> !FinancialImpactCheck.CODE.equals(r.getCheckCode())).toList();
    if (!others.isEmpty()) {
      throw new BusinessRuleException(
          "RNW_CHECK_BLOCKING",
          "Renewal "
              + c.getRenewalRef()
              + " cannot be accepted: "
              + others.stream()
                  .map(r -> CheckNames.of(r.getCheckCode()))
                  .collect(Collectors.joining(", "))
              + " failed");
    }
    if (impact && !acknowledged) {
      throw new BusinessRuleException(
          "RNW_ACCEPTANCE_IMPACT",
          "The renewal terms differ from the expiring terms: confirm that the client accepted them");
    }
    return impact;
  }

  private static String label(AcceptanceMethod method) {
    return switch (method) {
      case EMAIL -> "e-mail";
      case SIGNED_RA -> "signed Renewal Advice";
      case PAYMENT -> "payment";
    };
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * An acceptance.
   *
   * @param method method
   * @param attachmentId e-mail or signed RA on the renewal (EMAIL, SIGNED_RA)
   * @param reference payment reference (PAYMENT) or other evidence reference
   * @param acceptedOn date, default today
   * @param remarks remarks
   * @param financialImpactAck the user confirms the client accepted changed terms
   */
  public record Input(
      AcceptanceMethod method,
      Long attachmentId,
      String reference,
      LocalDate acceptedOn,
      String remarks,
      boolean financialImpactAck) {}
}
