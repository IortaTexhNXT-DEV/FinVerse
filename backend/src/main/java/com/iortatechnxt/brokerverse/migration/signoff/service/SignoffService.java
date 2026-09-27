package com.iortatechnxt.brokerverse.migration.signoff.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.load.service.BatchLogger;
import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapService;
import com.iortatechnxt.brokerverse.migration.mapping.service.LayoutService;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatchRepository;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.service.ObjectRegisterService;
import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRun;
import com.iortatechnxt.brokerverse.migration.recon.service.ReconciliationService;
import com.iortatechnxt.brokerverse.migration.signoff.domain.Gate;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoffRepository;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.jdbc.core.JdbcTemplate;
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

  private final ObjectRegisterService register;
  private final BatchPlanService plans;
  private final MigBatchRepository batches;
  private final LayoutService layouts;
  private final CodeMapService maps;
  private final ClientMatchRepository matches;
  private final ReconciliationService recon;
  private final MigSignoffRepository signoffs;
  private final GateRecorder gates;
  private final BatchLogger log;
  private final MigrationParameters parameters;
  private final JdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param register objects
   * @param plans batches
   * @param batches batch repository
   * @param layouts layouts
   * @param maps code maps
   * @param matches client pairs
   * @param recon reconciliation
   * @param signoffs sign-offs
   * @param gates gate recorder
   * @param log run log
   * @param parameters thresholds
   * @param jdbc JDBC (roles of the signer)
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public SignoffService(
      ObjectRegisterService register,
      BatchPlanService plans,
      MigBatchRepository batches,
      LayoutService layouts,
      CodeMapService maps,
      ClientMatchRepository matches,
      ReconciliationService recon,
      MigSignoffRepository signoffs,
      GateRecorder gates,
      BatchLogger log,
      MigrationParameters parameters,
      JdbcTemplate jdbc,
      CurrentUser currentUser,
      Clock clock) {
    this.register = register;
    this.plans = plans;
    this.batches = batches;
    this.layouts = layouts;
    this.maps = maps;
    this.matches = matches;
    this.recon = recon;
    this.signoffs = signoffs;
    this.gates = gates;
    this.log = log;
    this.parameters = parameters;
    this.jdbc = jdbc;
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
    MigDataObject object = register.get(objectCode);
    requireApproved(companyId, objectCode, null, Gate.G1);
    if (approve) {
      List<Layout> inForce = layouts.inForce(objectCode);
      if (inForce.isEmpty() && !object.getSourceSystems().isBlank()) {
        throw new BusinessRuleException(
            "MIG_LAYOUT_NOT_FROZEN",
            "Freeze the layouts of object " + objectCode + " before signing the mapping");
      }
      Set<String> missing = new TreeSet<>();
      for (Layout l : inForce) {
        for (LayoutColumn c : layouts.columns(l.getId())) {
          if (c.getMapSet() != null
              && !c.getMapSet().isBlank()
              && maps.approved(c.getMapSet()).isEmpty()) {
            missing.add(c.getMapSet());
          }
        }
      }
      if (!missing.isEmpty()) {
        throw new BusinessRuleException(
            "MIG_MAP_NOT_APPROVED",
            "These code maps have no approved version: " + String.join(", ", missing));
      }
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
    requireApproved(batch.getCompanyId(), batch.getObjectCode(), null, Gate.G2);
    if (approve) {
      requireLoadable(batch);
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
    requireApproved(batch.getCompanyId(), batch.getObjectCode(), batch.getId(), Gate.G3);
    requireLoadable(batch);
    requireDependenciesAccepted(batch);
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
    requireApproved(batch.getCompanyId(), batch.getObjectCode(), batch.getId(), Gate.G4);
    requireNotOperator(batch);
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
    requireApproved(batch.getCompanyId(), batch.getObjectCode(), batch.getId(), Gate.G5);
    requireNotOperator(batch);
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
    return batches.existsByCompanyIdAndObjectCodeAndStatusIn(
        companyId, objectCode, EnumSet.of(BatchStatus.SIGNED_OFF));
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

  private void requireLoadable(MigBatch batch) {
    MigDataObject object = register.get(batch.getObjectCode());
    if (batch.getErrorRate() != null
        && batch.getErrorRate().compareTo(parameters.maxErrorRate(object.isFinancial())) > 0) {
      throw new BusinessRuleException(
          "MIG_ERROR_RATE",
          batch.getErrorRate().stripTrailingZeros().toPlainString()
              + " percent of rows have errors; the limit for this object is "
              + parameters.maxErrorRate(object.isFinancial()).stripTrailingZeros().toPlainString()
              + " percent");
    }
    Integer unmapped =
        jdbc.queryForObject(
            "select count(*) from mig_issue where batch_id = ? and rule_code = 'DQ-003' and severity = 'ERROR'"
                + " and resolution = 'OPEN'",
            Integer.class,
            batch.getId());
    if (unmapped != null && unmapped > 0) {
      throw new BusinessRuleException(
          "MIG_UNMAPPED_CODES",
          "The batch has " + unmapped + " unmapped codes; map them before approving the load");
    }
    long review = matches.countByBatchIdAndDecision(batch.getId(), ClientMatch.Decision.REVIEW);
    if (review > 0) {
      throw new BusinessRuleException(
          "MIG_REVIEW_QUEUE",
          review + " client pairs are waiting for review; decide them before approving the load");
    }
  }

  private void requireDependenciesAccepted(MigBatch batch) {
    MigDataObject object = register.get(batch.getObjectCode());
    for (String dep : object.dependencies()) {
      if (!accepted(batch.getCompanyId(), dep)) {
        throw new BusinessRuleException(
            "MIG_DEPENDENCY_NOT_ACCEPTED",
            "Object " + dep + " must be accepted before this object can load");
      }
    }
  }

  private void requireNotOperator(MigBatch batch) {
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, batch.getLoadedBy())
        || CurrentUser.sameUser(user, batch.getValidatedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
  }

  private void requireApproved(Long companyId, String objectCode, Long batchId, Gate gate) {
    List<MigSignoff> list =
        batchId == null || gate == Gate.G1 || gate == Gate.G2
            ? signoffs.findByCompanyIdAndObjectCodeOrderByIdAsc(companyId, objectCode)
            : signoffs.findByBatchIdOrderByIdAsc(batchId);
    MigSignoff last = null;
    for (MigSignoff s : list) {
      if (s.getGate() == gate) {
        last = s;
      }
    }
    if (last == null || !last.approved()) {
      throw new BusinessRuleException(
          "MIG_GATE_ORDER", "Sign gate " + gate + " (" + gate.label() + ") first");
    }
  }

  private MigSignoff sign(MigBatch batch, Gate gate, String role, boolean approve, String comment) {
    requireSegregation(batch, gate, role);
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
    requireRole(user, role);
    if (!approve && (comment == null || comment.isBlank())) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Enter the reason for the rejection");
    }
    return gates.record(
        companyId, scope, gate, new MigSignoff.Signer(role, user), approve, comment);
  }

  private void requireSegregation(MigBatch batch, Gate gate, String role) {
    String user = currentUser.username();
    for (MigSignoff s : signoffs.findByBatchIdOrderByIdAsc(batch.getId())) {
      boolean otherGate = s.getGate() != gate;
      if (CurrentUser.sameUser(s.getUsername(), user)
          && otherGate
          && !s.getRoleCode().equals(role)) {
        throw new BusinessRuleException(
            "MAKER_CHECKER_VIOLATION",
            "A record cannot be authorized by the user who maintained it");
      }
      if (CurrentUser.sameUser(s.getUsername(), user) && !otherGate && s.approved()) {
        throw new BusinessRuleException(
            "MIG_GATE_SIGNED", "You have already signed gate " + gate + " of this batch");
      }
    }
  }

  private void requireRole(String user, String role) {
    Integer holds =
        jdbc.queryForObject(
            "select count(*) from sec_user_role ur join sec_user u on u.id = ur.user_id"
                + " join sec_role r on r.id = ur.role_id where lower(u.username) = lower(?) and r.code = ?",
            Integer.class,
            user,
            role);
    if (holds == null || holds == 0) {
      throw new BusinessRuleException(
          "MIG_GATE_ROLE", "This gate is signed in the role " + role + ", which you do not hold");
    }
  }
}
