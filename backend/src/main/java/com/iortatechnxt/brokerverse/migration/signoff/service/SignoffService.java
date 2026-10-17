package com.iortatechnxt.brokerverse.migration.signoff.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.service.BatchLogger;
import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRun;
import com.iortatechnxt.brokerverse.migration.recon.service.ReconciliationService;
import com.iortatechnxt.brokerverse.migration.signoff.domain.Gate;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoffRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sign-off gates of an object and its batches (BRID 1.1a, 1.1b; DATA_MIGRATION_DESIGN section
 * 13; FR-DM-003): G2 mapping and G3 validation, G4 load approval, G5 reconciliation and G6 object
 * acceptance, each by its role, in order, with the segregation of duties of section 18.2: the user
 * who ran a batch does not sign its reconciliation or acceptance, and one user does not sign two
 * gates of the same batch in different roles. G1 is recorded by the decision, G7 by the go / no-go.
 */
@Service
@Transactional
public class SignoffService {

  private static final String OWNER = "DATA_OWNER";
  private static final String LEAD = "DATA_MIGRATION_LEAD";
  private static final String STEWARD = "DATA_STEWARD";
  private static final String RECON = "MIGRATION_RECON_APPROVER";

  private final BatchPlanService plans;
  private final ReconciliationService recon;
  private final MigSignoffRepository signoffs;
  private final GateRecorder gates;
  private final GateRules rules;
  private final MappingGate mapping;
  private final BatchLogger log;
  private final MigrationParameters parameters;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param plans batches
   * @param recon reconciliation
   * @param signoffs sign-offs
   * @param gates gate recorder
   * @param rules gate conditions
   * @param mapping mapping readiness
   * @param log run log
   * @param parameters retention
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public SignoffService(
      BatchPlanService plans,
      ReconciliationService recon,
      MigSignoffRepository signoffs,
      GateRecorder gates,
      GateRules rules,
      MappingGate mapping,
      BatchLogger log,
      MigrationParameters parameters,
      CurrentUser currentUser,
      Clock clock) {
    this.plans = plans;
    this.recon = recon;
    this.signoffs = signoffs;
    this.gates = gates;
    this.rules = rules;
    this.mapping = mapping;
    this.log = log;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Signs the mapping of an object (G2): its layouts in force are frozen and every code map set its
   * columns use has an approved version.
   *
   * @param companyId company
   * @param objectCode object
   * @param approve approve or reject
   * @param comment comment
   * @return the sign-off
   */
  public MigSignoff signMapping(
      Long companyId, String objectCode, boolean approve, String comment) {
    rules.requireApproved(companyId, objectCode, null, Gate.G1);
    if (approve) {
      mapping.requireReady(objectCode);
    }
    return sign(
        companyId, new MigSignoff.Scope(objectCode, null, null), Gate.G2, OWNER, approve, comment);
  }

  /**
   * Signs the validation of a batch (G3): error rate within the limit, no unmapped code in a
   * mandatory column, the client review queue empty.
   *
   * @param batchNo batch
   * @param approve approve or reject
   * @param comment comment
   * @return the sign-off
   */
  public MigSignoff signValidation(String batchNo, boolean approve, String comment) {
    MigBatch batch = plans.get(batchNo);
    batch.requireStatus("signed for validation", BatchStatus.VALIDATED);
    rules.requireApproved(batch.getCompanyId(), batch.getObjectCode(), null, Gate.G2);
    if (approve) {
      rules.requireLoadable(batch);
    }
    return sign(batch, Gate.G3, STEWARD, approve, comment);
  }

  /**
   * Approves the load of a batch (G4) after its validation is signed and the objects it depends on
   * are accepted in this environment.
   *
   * @param batchNo batch
   * @param comment comment
   * @return the batch, APPROVED
   */
  public MigBatch approveLoad(String batchNo, String comment) {
    MigBatch batch = plans.get(batchNo);
    rules.requireApproved(batch.getCompanyId(), batch.getObjectCode(), batch.getId(), Gate.G3);
    rules.requireLoadable(batch);
    rules.requireDependenciesAccepted(batch);
    batch.approveLoad(currentUser.username(), clock.instant());
    sign(batch, Gate.G4, LEAD, true, comment);
    log.info(batch, "APPROVE", "Load approved by " + currentUser.username());
    return batch;
  }

  /**
   * Signs the reconciliation of a batch (G5); no break may be open.
   *
   * @param batchNo batch
   * @param approve approve or reject
   * @param comment comment
   * @return the sign-off
   */
  public MigSignoff signReconciliation(String batchNo, boolean approve, String comment) {
    MigBatch batch = plans.get(batchNo);
    rules.requireApproved(batch.getCompanyId(), batch.getObjectCode(), batch.getId(), Gate.G4);
    GateRules.requireNotOperator(batch, currentUser.username());
    MigReconRun run =
        recon
            .latest(batch.getId())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_NOT_RECONCILED", "Reconcile the batch before signing it"));
    if (approve && run.getStatus() == MigReconRun.Status.BREAKS) {
      throw new BusinessRuleException(
          "MIG_OPEN_BREAKS", "Explain and approve every break before signing the reconciliation");
    }
    MigSignoff s = sign(batch, Gate.G5, RECON, approve, comment);
    if (approve) {
      run.sign();
    }
    return s;
  }

  /**
   * Accepts the object of a batch (G6) as the data owner or the Data Migration Lead; with both
   * approvals the batch is SIGNED_OFF and its staging data is purged after the retention.
   *
   * @param batchNo batch
   * @param role DATA_OWNER or DATA_MIGRATION_LEAD
   * @param approve approve or reject
   * @param comment comment
   * @return the sign-off
   */
  public MigSignoff signAcceptance(String batchNo, String role, boolean approve, String comment) {
    if (!OWNER.equals(role) && !LEAD.equals(role)) {
      throw new BusinessRuleException(
          "MIG_GATE_ROLE", "Accept the object as data owner or Data Migration Lead");
    }
    MigBatch batch = plans.get(batchNo);
    rules.requireApproved(batch.getCompanyId(), batch.getObjectCode(), batch.getId(), Gate.G5);
    GateRules.requireNotOperator(batch, currentUser.username());
    MigSignoff s = sign(batch, Gate.G6, role, approve, comment);
    List<MigSignoff> g6 =
        signoffs.findByBatchIdOrderByIdAsc(batch.getId()).stream()
            .filter(x -> x.getGate() == Gate.G6 && x.approved())
            .toList();
    boolean owner = g6.stream().anyMatch(x -> OWNER.equals(x.getRoleCode()));
    boolean lead = g6.stream().anyMatch(x -> LEAD.equals(x.getRoleCode()));
    if (owner && lead) {
      batch.signedOff(
          clock.instant(), BusinessClock.today(clock).plusDays(parameters.retentionDays()));
      log.info(
          batch,
          "SIGNOFF",
          "Object "
              + batch.getObjectCode()
              + " accepted; staging purged after "
              + batch.getPurgeDueOn());
    }
    return s;
  }

  /**
   * Whether an object is accepted (G6) in this environment: a batch of it is signed off.
   *
   * @param companyId company
   * @param objectCode object
   * @return true when accepted
   */
  @Transactional(readOnly = true)
  public boolean accepted(Long companyId, String objectCode) {
    return rules.accepted(companyId, objectCode);
  }

  /**
   * Every sign-off of a company.
   *
   * @param companyId company
   * @return sign-offs
   */
  @Transactional(readOnly = true)
  public List<MigSignoff> all(Long companyId) {
    return signoffs.findByCompanyIdOrderByIdAsc(companyId);
  }

  private MigSignoff sign(MigBatch batch, Gate gate, String role, boolean approve, String comment) {
    rules.requireSegregation(batch, gate, role, currentUser.username());
    return sign(
        batch.getCompanyId(),
        new MigSignoff.Scope(batch.getObjectCode(), batch.getId(), null),
        gate,
        role,
        approve,
        comment);
  }

  private MigSignoff sign(
      Long companyId,
      MigSignoff.Scope scope,
      Gate gate,
      String role,
      boolean approve,
      String comment) {
    String user = currentUser.username();
    rules.requireRole(user, role);
    if (!approve && (comment == null || comment.isBlank())) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the rejection");
    }
    return gates.record(
        companyId, scope, gate, new MigSignoff.Signer(role, user), approve, comment);
  }
}
