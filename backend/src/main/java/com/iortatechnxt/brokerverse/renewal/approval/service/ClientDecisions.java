package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalDispositions;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client's answer to the Renewal Advice or the proposal (FRRN.019.01, FRRN.025.01): Pending
 * Client Response, Accepted, Rejected, Request for Revision or Not Applicable, with mandatory
 * remarks for a rejection or a revision. A rejected Renewal Advice routes the account to the
 * Non-Renewal Letter at expiry and a rejected proposal makes the account Not for Renewal for the
 * reason Lost Business; a revision is answered by a revised Renewal Advice, whose sending puts the
 * status back to Pending Client Response. CBG Home and FFY accounts need no answer: their status is
 * Not Applicable once the Renewal Advice or FFY Reminder is sent, and they are For Billing
 * Generation.
 */
@Service
public class ClientDecisions {

  /** Labels of the statuses. */
  public static final Map<String, String> LABELS =
      Map.of(
          CandidateDecision.PENDING, "Pending Client Response",
          CandidateDecision.ACCEPTED, "Accepted",
          CandidateDecision.REJECTED, "Rejected",
          CandidateDecision.REVISION, "Request for Revision",
          CandidateDecision.NOT_APPLICABLE, "Not Applicable");

  private static final Set<String> NEED_REMARKS =
      Set.of(CandidateDecision.REJECTED, CandidateDecision.REVISION);
  private static final Set<RenewalStage> OPEN =
      Set.of(
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.FOR_TL_REVIEW,
          RenewalStage.RA_GENERATED,
          RenewalStage.RA_SENT,
          RenewalStage.NB_PATH);
  private static final Set<RenewalDisposition> PROPOSED =
      Set.of(RenewalDisposition.FOR_PROPOSAL, RenewalDisposition.FOR_QUOTATION);
  private static final String CBG_HOME = "PROPERTY";
  private static final String LOST_BUSINESS = "LOST_BUSINESS";
  private static final int REMARKS_MAX = 200;

  private final RenewalRecords records;
  private final RenewalDispositions dispositions;
  private final RenewalParameters parameters;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param dispositions Not for Renewal of a rejected proposal
   * @param parameters CBG segments
   * @param currentUser user
   * @param audit audit trail
   * @param clock clock
   */
  public ClientDecisions(
      RenewalRecords records,
      RenewalDispositions dispositions,
      RenewalParameters parameters,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.dispositions = dispositions;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records the client's answer.
   *
   * @param companyId company
   * @param ref renewal
   * @param status status
   * @param remarks remarks, required for Rejected and Request for Revision
   */
  @Transactional
  public void record(Long companyId, String ref, String status, String remarks) {
    requireStatus(status, remarks);
    RenewalCandidate c = records.get(companyId, ref);
    if (!OPEN.contains(c.getStage())) {
      throw new BusinessRuleException(
          "RNW_CLIENT_STAGE",
          "The client's answer is recorded once the Renewal Advice or proposal is out");
    }
    c.getPlacement()
        .getDecision()
        .client(
            status,
            remarks == null || remarks.isBlank() ? null : remarks.strip(),
            currentUser.username(),
            clock.instant());
    audit.record(
        RenewalCodes.ENTITY,
        ref,
        AuditAction.UPDATE,
        "Client acceptance status: "
            + LABELS.get(status)
            + (remarks == null ? "" : " - " + remarks));
    afterRecord(c, status, remarks);
  }

  private void afterRecord(RenewalCandidate c, String status, String remarks) {
    if (CandidateDecision.REJECTED.equals(status) && proposed(c)) {
      dispositions.record(
          c,
          new CurrentDisposition(
              RenewalDisposition.NOT_FOR_RENEWAL,
              LOST_BUSINESS,
              DispositionSource.USER,
              cut(remarks),
              null),
          null,
          null);
    }
  }

  private static void requireStatus(String status, String remarks) {
    if (!LABELS.containsKey(status)) {
      throw new BusinessRuleException("RNW_CLIENT_STATUS", "Select the client acceptance status");
    }
    if (NEED_REMARKS.contains(status) && (remarks == null || remarks.isBlank())) {
      throw new BusinessRuleException(
          "RNW_CLIENT_REMARKS", "Remarks are required for " + LABELS.get(status));
    }
  }

  private static boolean proposed(RenewalCandidate c) {
    return c.getDisposition() != null && PROPOSED.contains(c.getDisposition().code());
  }

  private static String cut(String text) {
    String t = text.strip();
    return t.length() > REMARKS_MAX ? t.substring(0, REMARKS_MAX) : t;
  }

  /**
   * Sets the status after a Renewal Advice or FFY Reminder is sent: Not Applicable for CBG Home and
   * FFY accounts, otherwise Pending Client Response (also after a revision).
   *
   * @param c renewal
   * @param freeFirstYear whether the account is a free first year account
   */
  public void afterRaSent(RenewalCandidate c, boolean freeFirstYear) {
    String next =
        freeFirstYear || cbgHome(c) ? CandidateDecision.NOT_APPLICABLE : CandidateDecision.PENDING;
    c.getPlacement()
        .getDecision()
        .client(next, null, currentUser.optionalUsername().orElse("SYSTEM"), clock.instant());
  }

  /**
   * Whether a renewal is For Billing Generation: no answer needed and not yet in a billing file.
   *
   * @param c renewal
   * @return true when billable
   */
  public static boolean forBilling(RenewalCandidate c) {
    return c.getStage() == RenewalStage.RA_SENT
        && CandidateDecision.NOT_APPLICABLE.equals(c.getPlacement().getDecision().getClientStatus())
        && c.getPlacement().getTags().getBillingFileId() == null;
  }

  private boolean cbgHome(RenewalCandidate c) {
    var p = c.getSnapshot().product();
    return p != null && parameters.cbgSegment(p.segment()) && CBG_HOME.equals(p.lineCode());
  }
}
