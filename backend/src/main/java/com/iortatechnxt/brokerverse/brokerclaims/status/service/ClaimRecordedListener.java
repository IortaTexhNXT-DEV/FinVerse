package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecorded;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Sets the first status of a claim when it is recorded (BRCLM.010/019, FR-CM-042 R1; contract of
 * wave CL1-A, design section 18): on {@link ClaimRecorded}, inside the recording transaction, the
 * status chosen at recording (default {@code NEW_INCOMPLETE_DOCS}) is set, the next follow-up date
 * computed, the workflow case aligned and {@code ClaimStatusChanged} published with {@code from =
 * null}.
 */
@Component
public class ClaimRecordedListener {

  /** First status when the recording names none. */
  static final String DEFAULT_STATUS = "NEW_INCOMPLETE_DOCS";

  private final ClaimLookup lookup;
  private final ClaimStatusService statuses;

  /**
   * Creates the listener.
   *
   * @param lookup claims of the company
   * @param statuses status engine
   */
  public ClaimRecordedListener(ClaimLookup lookup, ClaimStatusService statuses) {
    this.lookup = lookup;
    this.statuses = statuses;
  }

  /**
   * Sets the first status of the recorded claim.
   *
   * @param event recorded claim
   */
  @EventListener
  public void on(ClaimRecorded event) {
    Claim claim = lookup.require(event.companyId(), event.claimId());
    if (claim.getProgress().getStatusCode() != null) {
      return;
    }
    String status =
        event.initialStatus() == null || event.initialStatus().isBlank()
            ? DEFAULT_STATUS
            : event.initialStatus();
    statuses.recordInitialStatus(claim, status, null);
  }
}
