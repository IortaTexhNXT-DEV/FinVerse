package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEvent;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEventRepository;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimField;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps every change of a tracked claim field twice (FR-CL-003): a row of the claim timeline
 * ({@code bcl_claim_event}) and an audit entry, in the caller's transaction. Public contract of the
 * module: CL1-A records the reported date, claimant, cover version and authorization code changes
 * through {@link #record}.
 */
@Service
@Transactional
public class ClaimEventRecorder {

  private final ClaimEventRepository events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param events claim timeline
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ClaimEventRecorder(
      ClaimEventRepository events, AuditTrailService audit, CurrentUser currentUser, Clock clock) {
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a change; nothing when the value did not change.
   *
   * @param claim claim
   * @param field field
   * @param values old and new value
   * @param reason reason or remark, may be null
   * @return true when recorded
   */
  public boolean record(Claim claim, ClaimField field, ClaimEvent.Values values, String reason) {
    if (Objects.equals(values.oldValue(), values.newValue())) {
      return false;
    }
    events.save(
        new ClaimEvent(
            claim.getId(),
            field,
            values,
            new ClaimEvent.Author(currentUser.username(), clock.instant(), reason)));
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        field
            + ": "
            + text(values.oldValue())
            + " -> "
            + text(values.newValue())
            + (reason == null || reason.isBlank() ? "" : " (" + reason + ")"));
    return true;
  }

  private static String text(String value) {
    return value == null ? "-" : value;
  }
}
