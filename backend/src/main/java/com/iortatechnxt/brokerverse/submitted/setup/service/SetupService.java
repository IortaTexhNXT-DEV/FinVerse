package com.iortatechnxt.brokerverse.submitted.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrixRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmInsurerRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmInsurerRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSourceRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStatusMap;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStatusMapRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmUserScope;
import com.iortatechnxt.brokerverse.submitted.domain.SbmUserScopeRepository;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Submitted Policies Setup (FRS section 9.3): the limit rules, insurer rules, letter rules and
 * approval matrix levels with maker and checker ({@code SBM_RULE_MAINTAIN}, {@code
 * SBM_RULE_APPROVE}), and the source register, legacy status map and user scopes maintained
 * directly. Every change is audited.
 */
@Service
@Transactional
public class SetupService {

  private static final String ENTITY = "SubmittedSetup";
  private static final Set<String> CHANNELS = Set.of("EMAIL", "PRINT", "BANK_COUNTERPART");
  private static final Set<String> DOCUMENTS = Set.of("IAAF", "TOR");

  private final SbmLimitRuleRepository limits;
  private final SbmInsurerRuleRepository insurers;
  private final SbmLetterRuleRepository letters;
  private final SbmApprovalMatrixRepository matrix;
  private final SbmSourceRepository sources;
  private final SbmStatusMapRepository statusMap;
  private final SbmUserScopeRepository scopes;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param limits limit rules
   * @param insurers insurer rules
   * @param letters letter rules
   * @param matrix approval matrices
   * @param sources sources
   * @param statusMap legacy status map
   * @param scopes user scopes
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // the masters of the setup
  public SetupService(
      SbmLimitRuleRepository limits,
      SbmInsurerRuleRepository insurers,
      SbmLetterRuleRepository letters,
      SbmApprovalMatrixRepository matrix,
      SbmSourceRepository sources,
      SbmStatusMapRepository statusMap,
      SbmUserScopeRepository scopes,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.limits = limits;
    this.insurers = insurers;
    this.letters = letters;
    this.matrix = matrix;
    this.sources = sources;
    this.statusMap = statusMap;
    this.scopes = scopes;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Limit rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<SbmLimitRule> limitRules(Long companyId) {
    return limits.findByCompanyIdOrderByInsurerCodeAscIdAsc(companyId);
  }

  /**
   * Saves a limit rule (new when id is null), pending approval.
   *
   * @param companyId company
   * @param id rule, null for a new one
   * @param l limits
   * @return rule
   */
  public SbmLimitRule saveLimitRule(Long companyId, Long id, SbmLimitRule.Limits l) {
    if (l.maxSumInsured() == null && l.maxVehicleAge() == null && l.attribute() == null) {
      throw new BusinessRuleException("SBM_LIMIT_EMPTY", "Enter at least one limit");
    }
    segment(l.segment());
    SbmLimitRule r = id == null ? limits.save(new SbmLimitRule(companyId, l)) : changed(limits, id, x -> x.change(l));
    audit.record(ENTITY, "limit " + r.getId(), AuditAction.UPDATE, "Limit rule of " + l.insurerCode());
    return r;
  }

  /**
   * Insurer rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<SbmInsurerRule> insurerRules(Long companyId) {
    return insurers.findByCompanyIdOrderBySegmentAscPriorityDesc(companyId);
  }

  /**
   * Saves an insurer rule, pending approval.
   *
   * @param companyId company
   * @param id rule, null for a new one
   * @param row content
   * @return rule
   */
  public SbmInsurerRule saveInsurerRule(Long companyId, Long id, SbmInsurerRule.Row row) {
    segment(row.segment());
    SbmInsurerRule r =
        id == null ? insurers.save(new SbmInsurerRule(companyId, row)) : changed(insurers, id, x -> x.change(row));
    audit.record(ENTITY, "insurer " + r.getId(), AuditAction.UPDATE, "Insurer rule " + row.insurerCode());
    return r;
  }

  /**
   * Letter rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<SbmLetterRule> letterRules(Long companyId) {
    return letters.findByCompanyIdOrderByLetterTypeAscIdAsc(companyId);
  }

  /**
   * Saves a letter rule, pending approval.
   *
   * @param companyId company
   * @param id rule, null for a new one
   * @param row content
   * @return rule
   */
  public SbmLetterRule saveLetterRule(Long companyId, Long id, SbmLetterRule.Row row) {
    LocalDate today = BusinessClock.today(clock);
    lovs.requireValid("SBM_LETTER_TYPE", row.letterType(), today);
    if (!CHANNELS.contains(row.channel())) {
      throw new BusinessRuleException("SBM_CHANNEL", "Select e-mail, bank counterpart or print");
    }
    segment(row.segment());
    if (row.bucket() != null) {
      lovs.requireValid(SubmittedCodes.LOV_BUCKET, row.bucket(), today);
    }
    if (row.status() != null) {
      SbmPolicyStatus.valueOf(row.status());
    }
    SbmLetterRule r =
        id == null ? letters.save(new SbmLetterRule(companyId, row)) : changed(letters, id, x -> x.change(row));
    audit.record(ENTITY, "letter " + r.getId(), AuditAction.UPDATE, "Letter rule " + row.letterType());
    return r;
  }

  /**
   * Approval matrix levels of a company.
   *
   * @param companyId company
   * @return levels
   */
  @Transactional(readOnly = true)
  public List<SbmApprovalMatrix> matrix(Long companyId) {
    return matrix.findByCompanyIdOrderByDocumentAscSegmentAscTsiFromAscLevelAsc(companyId);
  }

  /**
   * Saves an approval matrix level, pending approval.
   *
   * @param companyId company
   * @param id level, null for a new one
   * @param row content
   * @return level
   */
  public SbmApprovalMatrix saveMatrix(Long companyId, Long id, SbmApprovalMatrix.Row row) {
    if (!DOCUMENTS.contains(row.document())) {
      throw new BusinessRuleException("SBM_MATRIX_DOCUMENT", "Select IAAF or TOR");
    }
    try {
      Permission.valueOf(row.permission());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new BusinessRuleException("SBM_MATRIX_PERMISSION", "Select the permission of the approvers");
    }
    segment(row.segment());
    SbmApprovalMatrix m =
        id == null ? matrix.save(new SbmApprovalMatrix(companyId, row)) : changed(matrix, id, x -> x.change(row));
    audit.record(ENTITY, "matrix " + m.getId(), AuditAction.UPDATE, row.document() + " level " + row.level());
    return m;
  }

  /**
   * Approves a pending record of a kind (not by its maker).
   *
   * @param kind LIMIT, INSURER, LETTER or MATRIX
   * @param id record
   * @return the record
   */
  public AuthorizableEntity authorize(String kind, Long id) {
    AuthorizableEntity e = find(kind, id);
    e.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, kind.toLowerCase(Locale.ROOT) + " " + id, AuditAction.AUTHORIZE, "Approved");
    return e;
  }

  /**
   * Deactivates a record of a kind.
   *
   * @param kind LIMIT, INSURER, LETTER or MATRIX
   * @param id record
   * @return the record
   */
  public AuthorizableEntity deactivate(String kind, Long id) {
    AuthorizableEntity e = find(kind, id);
    e.deactivate();
    audit.record(ENTITY, kind.toLowerCase(Locale.ROOT) + " " + id, AuditAction.DEACTIVATE, "Deactivated");
    return e;
  }

  private AuthorizableEntity find(String kind, Long id) {
    JpaRepository<? extends AuthorizableEntity, Long> repo =
        switch (kind) {
          case "LIMIT" -> limits;
          case "INSURER" -> insurers;
          case "LETTER" -> letters;
          case "MATRIX" -> matrix;
          default -> throw new BusinessRuleException("SBM_SETUP_KIND", "Unknown setup record");
        };
    return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Setup record", id));
  }

  /**
   * The source register.
   *
   * @return sources
   */
  @Transactional(readOnly = true)
  public List<SbmSource> sources() {
    return sources.findAllByOrderByCodeAsc();
  }

  /**
   * Changes a source.
   *
   * @param id source
   * @param name name
   * @param mandatoryFields mandatory fields of a manual entry
   * @param active active
   * @return source
   */
  public SbmSource saveSource(Long id, String name, String mandatoryFields, boolean active) {
    SbmSource s = sources.findById(id).orElseThrow(() -> new ResourceNotFoundException("Source", id));
    s.maintain(name, mandatoryFields, active);
    audit.record(ENTITY, "source " + s.getCode(), AuditAction.UPDATE, "Source changed");
    return s;
  }

  /**
   * The legacy status map.
   *
   * @return mappings
   */
  @Transactional(readOnly = true)
  public List<SbmStatusMap> statusMap() {
    return statusMap.findAllByOrderByLegacyStatusAsc();
  }

  /**
   * Saves a legacy status mapping.
   *
   * @param legacyStatus legacy status
   * @param status status
   * @param bucket bucket, may be null
   * @return mapping
   */
  public SbmStatusMap saveStatusMap(String legacyStatus, SbmPolicyStatus status, String bucket) {
    String key = legacyStatus.strip().toUpperCase(Locale.ROOT);
    if (bucket != null) {
      lovs.requireValid(SubmittedCodes.LOV_BUCKET, bucket, BusinessClock.today(clock));
    }
    SbmStatusMap m =
        statusMap
            .findByLegacyStatus(key)
            .map(
                x -> {
                  x.maintain(status, bucket);
                  return x;
                })
            .orElseGet(() -> statusMap.save(new SbmStatusMap(key, status, bucket)));
    audit.record(ENTITY, "status " + key, AuditAction.UPDATE, "Status map");
    return m;
  }

  /**
   * User scopes of a company.
   *
   * @param companyId company
   * @return scopes
   */
  @Transactional(readOnly = true)
  public List<SbmUserScope> scopes(Long companyId) {
    return scopes.findByCompanyIdOrderByUsernameAsc(companyId);
  }

  /**
   * Saves the scope of a user.
   *
   * @param companyId company
   * @param username user
   * @param segments segments, empty for all
   * @param ownOnly own records only
   * @return scope
   */
  public SbmUserScope saveScope(Long companyId, String username, List<String> segments, boolean ownOnly) {
    segments.forEach(this::segment);
    SbmUserScope s =
        scopes
            .findByCompanyIdAndUsername(companyId, username)
            .map(
                x -> {
                  x.maintain(segments, ownOnly);
                  return x;
                })
            .orElseGet(() -> scopes.save(new SbmUserScope(companyId, username, segments, ownOnly)));
    audit.record(ENTITY, "scope " + username, AuditAction.UPDATE, "User scope");
    return s;
  }

  private void segment(String segment) {
    if (segment != null && !segment.isBlank()) {
      lovs.requireValid(SubmittedCodes.LOV_SEGMENT, segment, BusinessClock.today(clock));
    }
  }

  private static <E extends AuthorizableEntity> E changed(
      JpaRepository<E, Long> repo, Long id, java.util.function.Consumer<E> change) {
    E e = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Setup record", id));
    change.accept(e);
    return e;
  }
}
