package com.iortatechnxt.brokerverse.migration.signoff.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatchRepository;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.service.ObjectRegisterService;
import com.iortatechnxt.brokerverse.migration.signoff.domain.Gate;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoffRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The conditions of the sign-off gates (DATA_MIGRATION_DESIGN sections 13 and 18.2): gate order,
 * mapping readiness (layouts frozen, code maps approved), a loadable batch (error rate, unmapped
 * codes, client review queue), dependencies accepted, roles held and the segregation of duties (the
 * operator of a batch does not sign G5 or G6; one person does not sign two gates of one batch in
 * different roles).
 */
@Component
class GateRules {

  private final ObjectRegisterService register;
  private final MigBatchRepository batches;
  private final ClientMatchRepository matches;
  private final MigSignoffRepository signoffs;
  private final MigrationParameters parameters;
  private final JdbcTemplate jdbc;

  @SuppressWarnings("java:S107") // constructor injection
  GateRules(
      ObjectRegisterService register,
      MigBatchRepository batches,
      ClientMatchRepository matches,
      MigSignoffRepository signoffs,
      MigrationParameters parameters,
      JdbcTemplate jdbc) {
    this.register = register;
    this.batches = batches;
    this.matches = matches;
    this.signoffs = signoffs;
    this.parameters = parameters;
    this.jdbc = jdbc;
  }

  /** The error rate is within the limit, no mandatory code is unmapped, no client pair waits. */
  void requireLoadable(MigBatch batch) {
    MigDataObject object = register.get(batch.getObjectCode());
    BigDecimal limit = parameters.maxErrorRate(object.isFinancial());
    if (batch.getErrorRate() != null && batch.getErrorRate().compareTo(limit) > 0) {
      throw new BusinessRuleException(
          "MIG_ERROR_RATE",
          batch.getErrorRate().stripTrailingZeros().toPlainString()
              + " percent of rows have errors; the limit for this object is "
              + limit.stripTrailingZeros().toPlainString()
              + " percent");
    }
    Integer unmapped =
        jdbc.queryForObject(
            "select count(*) from mig_issue where batch_id = ? and rule_code = 'DQ-003'"
                + " and severity = 'ERROR' and resolution = 'OPEN'",
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

  /** Every object the batch's object depends on is accepted in this environment. */
  void requireDependenciesAccepted(MigBatch batch) {
    for (String dep : register.get(batch.getObjectCode()).dependencies()) {
      if (!accepted(batch.getCompanyId(), dep)) {
        throw new BusinessRuleException(
            "MIG_DEPENDENCY_NOT_ACCEPTED",
            "Object " + dep + " must be accepted before this object can load");
      }
    }
  }

  /** Whether an object is accepted: a batch of it is signed off. */
  boolean accepted(Long companyId, String objectCode) {
    return batches.existsByCompanyIdAndObjectCodeAndStatusIn(
        companyId, objectCode, EnumSet.of(BatchStatus.SIGNED_OFF));
  }

  /** The operator who validated or loaded a batch does not sign its later gates. */
  static void requireNotOperator(MigBatch batch, String user) {
    if (CurrentUser.sameUser(user, batch.getLoadedBy())
        || CurrentUser.sameUser(user, batch.getValidatedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
  }

  /** The previous gate is approved (G1-G2 per object, the others per batch). */
  void requireApproved(Long companyId, String objectCode, Long batchId, Gate gate) {
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

  /** One person signs a gate of a batch once, and no other gate of it in another role. */
  void requireSegregation(MigBatch batch, Gate gate, String role, String user) {
    for (MigSignoff s : signoffs.findByBatchIdOrderByIdAsc(batch.getId())) {
      if (CurrentUser.sameUser(s.getUsername(), user)) {
        requireSeparate(s, gate, role);
      }
    }
  }

  private static void requireSeparate(MigSignoff s, Gate gate, String role) {
    if (s.getGate() != gate && !s.getRoleCode().equals(role)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    if (s.getGate() == gate && s.approved()) {
      throw new BusinessRuleException(
          "MIG_GATE_SIGNED", "You have already signed gate " + gate + " of this batch");
    }
  }

  /** The user holds the role a gate is signed in. */
  void requireRole(String user, String role) {
    Integer holds =
        jdbc.queryForObject(
            "select count(*) from sec_user_role ur join sec_user u on u.id = ur.user_id"
                + " join sec_role r on r.id = ur.role_id"
                + " where lower(u.username) = lower(?) and r.code = ?",
            Integer.class,
            user,
            role);
    if (holds == null || holds == 0) {
      throw new BusinessRuleException(
          "MIG_GATE_ROLE", "This gate is signed in the role " + role + ", which you do not hold");
    }
  }
}
