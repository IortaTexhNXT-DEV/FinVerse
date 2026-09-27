package com.iortatechnxt.brokerverse.migration.signoff.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.signoff.domain.Gate;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoffRepository;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Records a sign-off that an approval step implies (gate G1 when a decision is approved, G4 when a
 * load is approved, G7 when the go / no-go board decides), so the sign-off matrix and its report
 * show every gate in one place.
 */
@Component
public class GateRecorder {

  private final MigSignoffRepository signoffs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param signoffs sign-offs
   * @param audit audit trail
   * @param clock clock
   */
  public GateRecorder(MigSignoffRepository signoffs, AuditTrailService audit, Clock clock) {
    this.signoffs = signoffs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records a sign-off.
   *
   * @param companyId company
   * @param scope object, batch and plan
   * @param gate gate
   * @param signer role and user
   * @param approved approved or rejected
   * @param comment comment
   * @return the sign-off
   */
  public MigSignoff record(
      Long companyId,
      MigSignoff.Scope scope,
      Gate gate,
      MigSignoff.Signer signer,
      boolean approved,
      String comment) {
    MigSignoff saved =
        signoffs.save(
            new MigSignoff(
                companyId,
                scope,
                gate,
                signer,
                approved ? MigSignoff.Decision.APPROVED : MigSignoff.Decision.REJECTED,
                comment,
                clock.instant()));
    audit.record(
        MigrationCodes.ENTITY_SIGNOFF,
        saved.getId(),
        approved ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        gate
            + " "
            + gate.label()
            + " of "
            + scope.objectCode()
            + (scope.batchId() == null ? "" : " batch " + scope.batchId())
            + (approved ? " approved" : " rejected")
            + " by "
            + signer.username()
            + " as "
            + signer.role());
    return saved;
  }
}
