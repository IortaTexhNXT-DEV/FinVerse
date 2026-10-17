package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Status;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Values;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChangeRepository;
import java.time.Clock;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remittance Settings (FRS BRD-02 FRID.RMT.003, delivery work DW-RMT-04): the extraction schedule,
 * the shared folder of each remittance type and the naming of the extract files. A change is
 * proposed by a maker ({@code REMIT_SETTINGS_MAINTAIN}) and takes effect when the Remittance Team
 * Leader approves it ({@code REMIT_SETTINGS_APPROVE}); until a change is approved the defaults
 * below apply.
 */
@Service
@Transactional
public class RemittanceSettingsService {

  /** Entity name of the audit trail. */
  public static final String ENTITY = "RemittanceSettings";

  /** Settings before any approved change: no schedule, the naming of BDOI's FRS. */
  public static final Values DEFAULTS =
      new Values(
          false,
          "20:00",
          true,
          "Remittance/Normal - Peso",
          "Remittance/Normal - Dollar",
          "Remittance/With Incentives",
          "{date}{insurer}{settlement}_{type}",
          "AM01");

  private static final int MAX_PATTERN = 120;
  private static final Pattern TIME = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
  private static final Pattern FOLDER = Pattern.compile("[A-Za-z0-9 ._/\\\\-]{1,250}");
  private static final Pattern PREFIX = Pattern.compile("[A-Za-z0-9]{0,10}");

  private final RemittanceSettingChangeRepository changes;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param changes setting changes
   * @param audit audit trail
   * @param currentUser signed-in user
   * @param clock clock
   */
  public RemittanceSettingsService(
      RemittanceSettingChangeRepository changes,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.changes = changes;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The settings in force.
   *
   * @param companyId company
   * @return latest approved settings, else the defaults
   */
  @Transactional(readOnly = true)
  public Values current(Long companyId) {
    return changes
        .findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, Status.APPROVED)
        .map(RemittanceSettingChange::values)
        .orElse(DEFAULTS);
  }

  /**
   * The shared folder of a remittance type.
   *
   * @param values settings
   * @param type remittance type
   * @return folder
   */
  public static String folder(Values values, RemittanceType type) {
    return switch (type) {
      case NORMAL_USD -> values.folderNormalUsd();
      case WITH_INCENTIVES -> values.folderWithIncentives();
      case NORMAL_PHP, SPECIAL -> values.folderNormalPhp();
    };
  }

  /**
   * Changes of a company, newest first.
   *
   * @param companyId company
   * @return changes
   */
  @Transactional(readOnly = true)
  public List<RemittanceSettingChange> history(Long companyId) {
    return changes.findTop50ByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * Proposes a change (maker); one change at a time waits for approval.
   *
   * @param companyId company
   * @param values proposed settings
   * @param reason reason
   * @return the pending change
   */
  public RemittanceSettingChange propose(Long companyId, Values values, String reason) {
    validate(values);
    if (changes.existsByCompanyIdAndStatus(companyId, Status.PENDING)) {
      throw new BusinessRuleException(
          "REMIT_SETTING_PENDING",
          "A change of the settings is waiting for approval; it must be decided first");
    }
    RemittanceSettingChange change =
        changes.save(
            new RemittanceSettingChange(
                companyId, values, reason, currentUser.username(), clock.instant()));
    audit.record(ENTITY, change.getId(), AuditAction.CREATE, "Change proposed: " + reason);
    return change;
  }

  /**
   * Approves or rejects a pending change (Remittance Team Leader, not the maker).
   *
   * @param id change
   * @param approve true to approve
   * @param comment comment, may be null
   * @return the change
   */
  public RemittanceSettingChange decide(Long id, boolean approve, String comment) {
    RemittanceSettingChange change =
        changes.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    if (approve) {
      changes
          .findFirstByCompanyIdAndStatusOrderByIdDesc(change.getCompanyId(), Status.APPROVED)
          .ifPresent(RemittanceSettingChange::superseded);
    }
    change.decide(approve, currentUser.username(), comment, clock.instant());
    audit.record(
        ENTITY,
        change.getId(),
        approve ? AuditAction.AUTHORIZE : AuditAction.UPDATE,
        approve ? "Change approved" : "Change rejected");
    return change;
  }

  private static void validate(Values v) {
    if (v.extractionTime() == null || !TIME.matcher(v.extractionTime()).matches()) {
      throw invalid("The extraction time must be a time such as 20:00");
    }
    requireFolders(v);
    if (!patternValid(v.filePattern())) {
      throw invalid("The file name pattern must contain {insurer} and {settlement}");
    }
    if (v.textPrefix() == null || !PREFIX.matcher(v.textPrefix()).matches()) {
      throw invalid("The text file prefix may hold up to 10 letters and digits");
    }
  }

  private static void requireFolders(Values v) {
    for (String folder :
        List.of(v.folderNormalPhp(), v.folderNormalUsd(), v.folderWithIncentives())) {
      if (folder == null || !FOLDER.matcher(folder).matches()) {
        throw invalid("A shared folder may hold letters, digits, spaces and . _ - / only");
      }
    }
  }

  private static boolean patternValid(String pattern) {
    return pattern != null
        && pattern.contains("{insurer}")
        && pattern.contains("{settlement}")
        && pattern.length() <= MAX_PATTERN;
  }

  private static BusinessRuleException invalid(String message) {
    return new BusinessRuleException("REMIT_SETTING_INVALID", message);
  }
}
