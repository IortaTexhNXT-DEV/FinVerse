package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSetting;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSettingRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCodeRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renewal Setup of the non-renewable risk codes (BRRN.009) and of the check settings (BRRN.020):
 * each change waits for a checker other than its maker; records are end-dated or deactivated, never
 * deleted.
 */
@Service
@Transactional
public class RenewalSetupService {

  /** Audit entity of a non-renewable risk code. */
  public static final String ENTITY_RISK_CODE = "RenewalRiskCode";

  /** Audit entity of a check setting. */
  public static final String ENTITY_CHECK_SETTING = "RenewalCheckSetting";

  private final NonRenewableRiskCodeRepository riskCodes;
  private final CheckSettingRepository settings;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param riskCodes non-renewable risk codes
   * @param settings check settings
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RenewalSetupService(
      NonRenewableRiskCodeRepository riskCodes,
      CheckSettingRepository settings,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.riskCodes = riskCodes;
    this.settings = settings;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The non-renewable risk codes of a company.
   *
   * @param companyId company
   * @return codes
   */
  @Transactional(readOnly = true)
  public List<NonRenewableRiskCode> riskCodes(Long companyId) {
    return riskCodes.findByCompanyIdOrderByRiskCodeAscIdAsc(companyId);
  }

  /**
   * Adds a non-renewable risk code, pending authorization.
   *
   * @param companyId company
   * @param data code
   * @return code
   */
  public NonRenewableRiskCode createRiskCode(Long companyId, NonRenewableRiskCode.Data data) {
    NonRenewableRiskCode code = riskCodes.save(new NonRenewableRiskCode(companyId, check(data)));
    audit.record(ENTITY_RISK_CODE, code.getId(), AuditAction.CREATE, describe(code));
    return code;
  }

  /**
   * Changes a non-renewable risk code; it must be authorized again.
   *
   * @param companyId company
   * @param id code
   * @param data code
   * @return code
   */
  public NonRenewableRiskCode updateRiskCode(
      Long companyId, Long id, NonRenewableRiskCode.Data data) {
    NonRenewableRiskCode code = riskCode(companyId, id);
    code.update(check(data));
    audit.record(ENTITY_RISK_CODE, id, AuditAction.UPDATE, describe(code));
    return code;
  }

  /**
   * Authorizes a non-renewable risk code.
   *
   * @param companyId company
   * @param id code
   * @return code
   */
  public NonRenewableRiskCode authorizeRiskCode(Long companyId, Long id) {
    NonRenewableRiskCode code = riskCode(companyId, id);
    authorize(code);
    audit.record(ENTITY_RISK_CODE, id, AuditAction.AUTHORIZE, describe(code));
    return code;
  }

  /**
   * Deactivates a non-renewable risk code.
   *
   * @param companyId company
   * @param id code
   * @return code
   */
  public NonRenewableRiskCode deactivateRiskCode(Long companyId, Long id) {
    NonRenewableRiskCode code = riskCode(companyId, id);
    code.deactivate();
    audit.record(ENTITY_RISK_CODE, id, AuditAction.DEACTIVATE, describe(code));
    return code;
  }

  /**
   * The check settings.
   *
   * @return settings by check
   */
  @Transactional(readOnly = true)
  public List<CheckSetting> checkSettings() {
    return settings.findAllByOrderByCheckCodeAsc();
  }

  /**
   * Changes a check setting; it must be authorized again (the previous values apply meanwhile).
   *
   * @param checkCode check
   * @param active active
   * @param severity severity
   * @param parameters parameters
   * @return setting
   */
  public CheckSetting updateCheckSetting(
      String checkCode, boolean active, CheckSeverity severity, String parameters) {
    if (severity == null) {
      throw new BusinessRuleException("RNW_CHECK_SEVERITY", "Choose the severity of the check");
    }
    CheckSetting setting = setting(checkCode);
    setting.update(active, severity, parameters == null ? null : parameters.strip());
    audit.record(
        ENTITY_CHECK_SETTING,
        setting.getId(),
        AuditAction.UPDATE,
        CheckNames.of(checkCode) + ": " + severity.label() + (active ? "" : ", inactive"));
    return setting;
  }

  /**
   * Authorizes a check setting.
   *
   * @param checkCode check
   * @return setting
   */
  public CheckSetting authorizeCheckSetting(String checkCode) {
    CheckSetting setting = setting(checkCode);
    authorize(setting);
    audit.record(
        ENTITY_CHECK_SETTING, setting.getId(), AuditAction.AUTHORIZE, CheckNames.of(checkCode));
    return setting;
  }

  private CheckSetting setting(String checkCode) {
    return settings
        .findByCheckCode(checkCode)
        .orElseThrow(() -> new ResourceNotFoundException("Check setting", checkCode));
  }

  private NonRenewableRiskCode riskCode(Long companyId, Long id) {
    return riskCodes
        .findById(id)
        .filter(c -> c.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Non-renewable risk code", id));
  }

  private void authorize(AuthorizableEntity entity) {
    entity.authorize(currentUser.username(), clock.instant());
  }

  private static NonRenewableRiskCode.Data check(NonRenewableRiskCode.Data data) {
    if (data.riskCode() == null || data.riskCode().isBlank()) {
      throw new BusinessRuleException("RNW_RISK_CODE_REQUIRED", "Enter the risk code");
    }
    if (data.reason() == null || data.reason().isBlank() || data.effectiveFrom() == null) {
      throw new BusinessRuleException(
          "RNW_RISK_CODE_INCOMPLETE", "Enter the reason and the effective date");
    }
    return new NonRenewableRiskCode.Data(
        data.riskCode().strip(),
        blankToNull(data.lineCode()),
        data.reason().strip(),
        data.effectiveFrom(),
        data.effectiveTo());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String describe(NonRenewableRiskCode code) {
    return "Risk code "
        + code.getRiskCode()
        + " not renewable from "
        + code.getEffectiveFrom()
        + (code.getEffectiveTo() == null ? "" : " to " + code.getEffectiveTo());
  }
}
