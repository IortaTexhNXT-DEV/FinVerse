package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient.RecipientValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipientRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule.RuleValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRuleRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service-fee configuration (FRBS 2.10.0; values AQ20): the rate per service-fee segment and the
 * market segments it covers, and the recipient and cost centre of each sales unit.
 */
@Service
@Transactional
public class ServiceFeeSetupService {

  private static final String RULE = "FrbsServiceFeeRule";
  private static final String RECIPIENT = "FrbsServiceFeeRecipient";
  private static final String SEGMENT_LOV = "SERVICE_FEE_SEGMENT";
  private static final String MARKET_LOV = "MARKET_SEGMENT";
  private static final BigDecimal MAX_RATE = BigDecimal.valueOf(100);

  private final ServiceFeeRuleRepository rules;
  private final ServiceFeeRecipientRepository recipients;
  private final LovService lovs;
  private final DimensionService dimensions;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules rules
   * @param recipients recipients
   * @param lovs lists of values
   * @param dimensions cost centres
   * @param audit audit trail
   * @param clock clock
   */
  public ServiceFeeSetupService(
      ServiceFeeRuleRepository rules,
      ServiceFeeRecipientRepository recipients,
      LovService lovs,
      DimensionService dimensions,
      AuditTrailService audit,
      Clock clock) {
    this.rules = rules;
    this.recipients = recipients;
    this.lovs = lovs;
    this.dimensions = dimensions;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Every rule.
   *
   * @return rules by segment and start
   */
  @Transactional(readOnly = true)
  public List<ServiceFeeRule> rules() {
    return rules.findAllByOrderBySegmentAscEffectiveFromAsc();
  }

  /**
   * Adds a rule.
   *
   * @param values content
   * @return rule
   */
  public ServiceFeeRule createRule(RuleValues values) {
    validate(values);
    ServiceFeeRule rule = rules.save(new ServiceFeeRule(values));
    audit.record(RULE, rule.getId(), AuditAction.CREATE, describe(values));
    return rule;
  }

  /**
   * Changes a rule; runs already computed keep their rates.
   *
   * @param id rule
   * @param values content
   * @return rule
   */
  public ServiceFeeRule updateRule(Long id, RuleValues values) {
    ServiceFeeRule rule =
        rules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service-fee rule", id));
    validate(values);
    rule.apply(values);
    audit.record(RULE, id, AuditAction.UPDATE, describe(values));
    return rule;
  }

  /**
   * The recipients of a company.
   *
   * @param companyId company
   * @return recipients by unit
   */
  @Transactional(readOnly = true)
  public List<ServiceFeeRecipient> recipients(Long companyId) {
    return recipients.findByCompanyIdOrderBySalesUnitAsc(companyId);
  }

  /**
   * Names the recipient of a sales unit.
   *
   * @param companyId company
   * @param salesUnit unit
   * @param values payee, cost centre and status
   * @return recipient
   */
  public ServiceFeeRecipient createRecipient(
      Long companyId, String salesUnit, RecipientValues values) {
    String unit = salesUnit == null ? "" : salesUnit.trim().toUpperCase(Locale.ROOT);
    if (unit.isEmpty()) {
      throw new BusinessRuleException("SERVICE_FEE_UNIT", "Give the sales unit");
    }
    if (recipients.findByCompanyIdAndSalesUnit(companyId, unit).isPresent()) {
      throw new DuplicateResourceException("Service-fee recipient", unit);
    }
    validate(companyId, values);
    ServiceFeeRecipient r = recipients.save(new ServiceFeeRecipient(companyId, unit, values));
    audit.record(RECIPIENT, r.getId(), AuditAction.CREATE, unit + " -> " + values.payeeCode());
    return r;
  }

  /**
   * Changes the recipient of a unit.
   *
   * @param id recipient
   * @param values payee, cost centre and status
   * @return recipient
   */
  public ServiceFeeRecipient updateRecipient(Long id, RecipientValues values) {
    ServiceFeeRecipient r =
        recipients
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service-fee recipient", id));
    validate(r.getCompanyId(), values);
    r.apply(values);
    audit.record(RECIPIENT, id, AuditAction.UPDATE, r.getSalesUnit() + " -> " + values.payeeCode());
    return r;
  }

  private void validate(RuleValues v) {
    LocalDate today = LocalDate.now(clock);
    if (v.segment() == null || v.rate() == null || v.effectiveFrom() == null) {
      throw new BusinessRuleException(
          "SERVICE_FEE_RULE", "Give the segment, the rate and the start date");
    }
    lovs.requireValid(SEGMENT_LOV, v.segment(), today);
    if (v.marketSegments().isEmpty()) {
      throw new BusinessRuleException(
          "SERVICE_FEE_RULE", "List the market segments the rule covers");
    }
    v.marketSegments().forEach(m -> lovs.requireValid(MARKET_LOV, m, today));
    if (v.rate().signum() <= 0 || v.rate().compareTo(MAX_RATE) > 0) {
      throw new BusinessRuleException("SERVICE_FEE_RATE", "The rate is a percentage above 0");
    }
    if (v.effectiveTo() != null && v.effectiveTo().isBefore(v.effectiveFrom())) {
      throw new BusinessRuleException("SERVICE_FEE_DATES", "The rule ends before it starts");
    }
  }

  private void validate(Long companyId, RecipientValues v) {
    if (v.payeeCode() == null
        || v.payeeCode().isBlank()
        || v.payeeName() == null
        || v.payeeName().isBlank()) {
      throw new BusinessRuleException(
          "SERVICE_FEE_RECIPIENT", "Give the payee code and name of the recipient");
    }
    dimensions.validateOptional(companyId, DimensionType.COST_CENTER, v.costCenter());
  }

  private static String describe(RuleValues v) {
    return v.segment()
        + " "
        + v.rate().stripTrailingZeros().toPlainString()
        + "% of "
        + ServiceFeeRule.BASE
        + " for "
        + String.join(",", v.marketSegments())
        + " from "
        + v.effectiveFrom();
  }
}
