package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRuleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Early-remittance incentive rules (RMTID.023, PRCID.028): maintenance and matching. The rates are
 * parked (OQ23); an invoice remitted within the window of a matching rule goes to a With Incentives
 * batch and earns the rule's rate on its basic premium.
 */
@Service
@Transactional
public class IncentiveRuleService {

  private static final String ENTITY = "RemittanceIncentiveRule";

  private final EarlyIncentiveRuleRepository rules;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param rules rules
   * @param audit audit trail
   */
  public IncentiveRuleService(EarlyIncentiveRuleRepository rules, AuditTrailService audit) {
    this.rules = rules;
    this.audit = audit;
  }

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<EarlyIncentiveRule> list(Long companyId) {
    return rules.findByCompanyIdOrderByInsurerCodeAscIdAsc(companyId);
  }

  /**
   * Adds a rule.
   *
   * @param companyId company
   * @param terms terms
   * @return rule
   */
  public EarlyIncentiveRule create(Long companyId, EarlyIncentiveRule.Terms terms) {
    validate(terms);
    EarlyIncentiveRule rule = rules.save(new EarlyIncentiveRule(companyId, terms));
    audit.record(ENTITY, rule.getId(), AuditAction.CREATE, describe(rule));
    return rule;
  }

  /**
   * Changes a rule.
   *
   * @param id rule
   * @param terms terms
   * @return rule
   */
  public EarlyIncentiveRule update(Long id, EarlyIncentiveRule.Terms terms) {
    validate(terms);
    EarlyIncentiveRule rule =
        rules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    rule.update(terms);
    audit.record(ENTITY, rule.getId(), AuditAction.UPDATE, describe(rule));
    return rule;
  }

  private static void validate(EarlyIncentiveRule.Terms terms) {
    if (terms.effectiveTo() != null && terms.effectiveTo().isBefore(terms.effectiveFrom())) {
      throw new BusinessRuleException(
          "INCENTIVE_RULE_PERIOD", "The rule cannot end before it starts");
    }
  }

  private static String describe(EarlyIncentiveRule r) {
    return r.getInsurerCode()
        + " "
        + (r.getProductLine() == null ? "all lines" : r.getProductLine())
        + " "
        + (r.getSegment() == null ? "all segments" : r.getSegment())
        + ": "
        + r.getRate()
        + "% within "
        + r.getWindowDays()
        + " days of "
        + r.getBasis()
        + (r.isActive() ? "" : " (inactive)");
  }

  /**
   * The rule that gives an invoice an early-remittance incentive when remitted on a date.
   *
   * @param invoice invoice
   * @param remittedOn remittance date
   * @return the first matching rule within its window
   */
  @Transactional(readOnly = true)
  public Optional<EarlyIncentiveRule> earlyIncentive(OpsInvoice invoice, LocalDate remittedOn) {
    var c = invoice.getClassification();
    return rules
        .findByCompanyIdAndInsurerCodeAndActiveTrueOrderByIdAsc(
            invoice.getCompanyId(), invoice.getInsurerCode())
        .stream()
        .filter(r -> r.covers(invoice.getInsurerCode(), c.productLine(), c.segment(), remittedOn))
        .filter(r -> r.isEarly(c.inceptionDate(), c.bookingDate(), remittedOn))
        .findFirst();
  }
}
