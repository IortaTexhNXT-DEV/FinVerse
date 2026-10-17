package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the client acceptance status in step with the renewal (FRRN.025.01): Pending Client
 * Response (or Not Applicable for CBG Home and FFY accounts) when the Renewal Advice is sent, also
 * after a revision, and Accepted when the acceptance is recorded.
 */
@Component
public class ClientStatusListener {

  private final RenewalCandidateRepository candidates;
  private final ClientDecisions decisions;
  private final AccountRepository accounts;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param candidates renewals
   * @param decisions client status rules
   * @param accounts expiring accounts (free first year)
   * @param clock clock
   */
  public ClientStatusListener(
      RenewalCandidateRepository candidates,
      ClientDecisions decisions,
      AccountRepository accounts,
      Clock clock) {
    this.candidates = candidates;
    this.decisions = decisions;
    this.accounts = accounts;
    this.clock = clock;
  }

  /**
   * Follows a stage change of a renewal.
   *
   * @param event stage change
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!RenewalCodes.WORKFLOW.equals(event.workflowCode())) {
      return;
    }
    if ("RA_SENT".equals(event.toStage())) {
      candidates
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(c -> decisions.afterRaSent(c, freeFirstYear(c)));
    } else if ("ACCEPTED".equals(event.toStage())) {
      candidates
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(
              c ->
                  c.getPlacement()
                      .getDecision()
                      .client(CandidateDecision.ACCEPTED, null, "SYSTEM", clock.instant()));
    }
  }

  private boolean freeFirstYear(RenewalCandidate c) {
    return c.getExpiringArn() != null
        && accounts
            .findByArn(c.getExpiringArn())
            .map(a -> a.getFreeFirstYear() != null && a.getFreeFirstYear().active())
            .orElse(false);
  }
}
