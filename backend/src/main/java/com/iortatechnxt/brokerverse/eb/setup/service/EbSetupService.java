package com.iortatechnxt.brokerverse.eb.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocumentRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRule;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRuleRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EB Setup (FR-EB-034, 042): the threshold rules and the required documents, maintained by the
 * holders of {@code EB_SETUP} with maker-checker: every creation or change waits for another user's
 * authorisation (My Approvals, {@link SetupApprovalSource}) before it applies.
 */
@Service
@Transactional
@SuppressWarnings(
    "PMD.GodClass") // the two EB set-up masters, each with the same maker-checker steps
public class EbSetupService {

  private static final String DEFAULT_CURRENCY = "PHP";

  private final EbThresholdRuleRepository rules;
  private final EbRequiredDocumentRepository required;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules threshold rules
   * @param required required documents
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user (checker)
   * @param clock clock
   */
  public EbSetupService(
      EbThresholdRuleRepository rules,
      EbRequiredDocumentRepository required,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rules = rules;
    this.required = required;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The threshold rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<EbThresholdRule> rules(Long companyId) {
    return rules.findByCompanyIdOrderByIdAsc(companyId);
  }

  /**
   * Creates a threshold rule pending authorisation.
   *
   * @param companyId company
   * @param data rule
   * @return rule
   */
  public EbThresholdRule createRule(Long companyId, EbThresholdRule.Data data) {
    EbThresholdRule rule = rules.save(new EbThresholdRule(companyId, check(data)));
    audit.record(EbCodes.ENTITY_THRESHOLD_RULE, rule.getId(), AuditAction.CREATE, describe(rule));
    return rule;
  }

  /**
   * Changes a threshold rule; it must be authorised again.
   *
   * @param companyId company
   * @param id rule
   * @param data rule
   * @return rule
   */
  public EbThresholdRule updateRule(Long companyId, Long id, EbThresholdRule.Data data) {
    EbThresholdRule rule = rule(companyId, id);
    rule.update(check(data));
    audit.record(EbCodes.ENTITY_THRESHOLD_RULE, id, AuditAction.UPDATE, describe(rule));
    return rule;
  }

  /**
   * Authorises a threshold rule (another user than its maker).
   *
   * @param companyId company
   * @param id rule
   * @return rule
   */
  public EbThresholdRule authorizeRule(Long companyId, Long id) {
    EbThresholdRule rule = rule(companyId, id);
    authorize(rule);
    audit.record(EbCodes.ENTITY_THRESHOLD_RULE, id, AuditAction.AUTHORIZE, describe(rule));
    return rule;
  }

  /**
   * Deactivates a threshold rule.
   *
   * @param companyId company
   * @param id rule
   * @return rule
   */
  public EbThresholdRule deactivateRule(Long companyId, Long id) {
    EbThresholdRule rule = rule(companyId, id);
    rule.deactivate();
    audit.record(EbCodes.ENTITY_THRESHOLD_RULE, id, AuditAction.DEACTIVATE, describe(rule));
    return rule;
  }

  private EbThresholdRule.Data check(EbThresholdRule.Data data) {
    checkValues(data);
    String line = blankToNull(data.benefitLine());
    if (line != null) {
      lovs.requireValid(EbCodes.LOV_BENEFIT_LINE, line, BusinessClock.today(clock));
    }
    return new EbThresholdRule.Data(
        line,
        data.measure(),
        data.amount(),
        blankToNull(data.currency()) == null ? DEFAULT_CURRENCY : data.currency().strip(),
        approver(data.approverPermission()),
        Math.max(1, data.approvalLevel()),
        data.effectiveFrom(),
        data.effectiveTo(),
        blankToNull(data.description()));
  }

  private static void checkValues(EbThresholdRule.Data data) {
    if (data.measure() == null) {
      throw new BusinessRuleException(
          "EB_THRESHOLD_MEASURE", "Select the measure: TSI or annual premium");
    }
    if (data.amount() == null || data.amount().signum() <= 0) {
      throw new BusinessRuleException("EB_THRESHOLD_AMOUNT", "Enter an amount greater than zero");
    }
    checkDates(data);
  }

  private static void checkDates(EbThresholdRule.Data data) {
    if (data.effectiveFrom() == null) {
      throw new BusinessRuleException("EB_THRESHOLD_FROM", "Enter the effective date");
    }
    if (data.effectiveTo() != null && data.effectiveTo().isBefore(data.effectiveFrom())) {
      throw new BusinessRuleException(
          "EB_THRESHOLD_DATES", "The end date must be on or after the start date");
    }
  }

  private static String approver(String given) {
    String permission =
        blankToNull(given) == null ? EbCodes.PERMISSION_THRESHOLD_APPROVE : given.strip();
    try {
      Permission.valueOf(permission);
    } catch (IllegalArgumentException e) {
      throw new BusinessRuleException(
          "EB_THRESHOLD_PERMISSION", "Select an existing approver permission", e);
    }
    return permission;
  }

  private static String describe(EbThresholdRule rule) {
    return (rule.getBenefitLine() == null ? "All lines" : rule.getBenefitLine())
        + " "
        + rule.getMeasure()
        + " >= "
        + rule.getCurrency()
        + " "
        + rule.getAmount().toPlainString()
        + ", approver "
        + rule.getApproverPermission();
  }

  private EbThresholdRule rule(Long companyId, Long id) {
    return rules
        .findById(id)
        .filter(r -> r.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_THRESHOLD_RULE, id));
  }

  /**
   * The required documents of a company.
   *
   * @param companyId company
   * @return requirements
   */
  @Transactional(readOnly = true)
  public List<EbRequiredDocument> requiredDocuments(Long companyId) {
    return required.findByCompanyIdOrderByProcessTypeAscIdAsc(companyId);
  }

  /**
   * Creates a required document pending authorisation.
   *
   * @param companyId company
   * @param data requirement
   * @return requirement
   */
  public EbRequiredDocument createRequired(Long companyId, EbRequiredDocument.Data data) {
    EbRequiredDocument.Data clean = check(data);
    boolean exists =
        required.findByCompanyIdOrderByProcessTypeAscIdAsc(companyId).stream()
            .anyMatch(r -> same(r, clean));
    if (exists) {
      throw new BusinessRuleException(
          "EB_REQUIRED_EXISTS", "This document is already required for the process and line");
    }
    EbRequiredDocument saved = required.save(new EbRequiredDocument(companyId, clean));
    audit.record(
        EbCodes.ENTITY_REQUIRED_DOCUMENT, saved.getId(), AuditAction.CREATE, describe(saved));
    return saved;
  }

  private static boolean same(EbRequiredDocument r, EbRequiredDocument.Data d) {
    return r.getProcessType().equals(d.processType())
        && r.getDocumentType().equals(d.documentType())
        && java.util.Objects.equals(r.getBenefitLine(), d.benefitLine());
  }

  /**
   * Changes a required document; it must be authorised again.
   *
   * @param companyId company
   * @param id requirement
   * @param data requirement
   * @return requirement
   */
  public EbRequiredDocument updateRequired(Long companyId, Long id, EbRequiredDocument.Data data) {
    EbRequiredDocument requirement = requirement(companyId, id);
    requirement.update(check(data));
    audit.record(EbCodes.ENTITY_REQUIRED_DOCUMENT, id, AuditAction.UPDATE, describe(requirement));
    return requirement;
  }

  /**
   * Authorises a required document (another user than its maker).
   *
   * @param companyId company
   * @param id requirement
   * @return requirement
   */
  public EbRequiredDocument authorizeRequired(Long companyId, Long id) {
    EbRequiredDocument requirement = requirement(companyId, id);
    authorize(requirement);
    audit.record(
        EbCodes.ENTITY_REQUIRED_DOCUMENT, id, AuditAction.AUTHORIZE, describe(requirement));
    return requirement;
  }

  /**
   * Deactivates a required document.
   *
   * @param companyId company
   * @param id requirement
   * @return requirement
   */
  public EbRequiredDocument deactivateRequired(Long companyId, Long id) {
    EbRequiredDocument requirement = requirement(companyId, id);
    requirement.deactivate();
    audit.record(
        EbCodes.ENTITY_REQUIRED_DOCUMENT, id, AuditAction.DEACTIVATE, describe(requirement));
    return requirement;
  }

  private EbRequiredDocument.Data check(EbRequiredDocument.Data data) {
    LocalDate today = BusinessClock.today(clock);
    if (blankToNull(data.processType()) == null) {
      throw new BusinessRuleException("EB_PROCESS_REQUIRED", "Select the process of the document");
    }
    lovs.requireValid(EbDocumentTypes.PROCESS_TYPE_LOV, data.processType(), today);
    String line = blankToNull(data.benefitLine());
    if (line != null) {
      lovs.requireValid(EbCodes.LOV_BENEFIT_LINE, line, today);
    }
    if (data.documentType() == null || !EbDocumentTypes.ALL.contains(data.documentType())) {
      throw new BusinessRuleException(
          "EB_DOCUMENT_TYPE_INVALID", "Select an Employee Benefits document type");
    }
    return new EbRequiredDocument.Data(
        data.processType(), line, data.documentType(), data.mandatory());
  }

  private String describe(EbRequiredDocument r) {
    return lovs.label("DOCUMENT_TYPE", r.getDocumentType())
        + " for "
        + lovs.label(EbDocumentTypes.PROCESS_TYPE_LOV, r.getProcessType())
        + (r.getBenefitLine() == null ? "" : " (" + r.getBenefitLine() + ")")
        + (r.isMandatory() ? ", mandatory" : ", optional");
  }

  private EbRequiredDocument requirement(Long companyId, Long id) {
    return required
        .findById(id)
        .filter(r -> r.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_REQUIRED_DOCUMENT, id));
  }

  private void authorize(AuthorizableEntity entity) {
    entity.authorize(currentUser.username(), clock.instant());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
