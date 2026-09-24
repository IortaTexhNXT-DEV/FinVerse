package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.AutoBookRule;
import com.iortatechnxt.brokerverse.booking.domain.AutoBookRuleRepository;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveRule;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveRuleRepository;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking rules maintained on the setup screen: auto-book rules (BRNB.076) and incentive
 * eligibility rules (BRNB.107, content parked Q33). Every change is audited.
 */
@Service
@Transactional
public class BookingRuleService {

  private static final String AUTO_BOOK = "AutoBookRule";
  private static final String INCENTIVE = "IncentiveRule";

  private final AutoBookRuleRepository autoBook;
  private final IncentiveRuleRepository incentives;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param autoBook auto-book rules
   * @param incentives incentive rules
   * @param audit audit trail
   */
  public BookingRuleService(
      AutoBookRuleRepository autoBook,
      IncentiveRuleRepository incentives,
      AuditTrailService audit) {
    this.autoBook = autoBook;
    this.incentives = incentives;
    this.audit = audit;
  }

  /**
   * Auto-book rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<AutoBookRule> autoBookRules(Long companyId) {
    return autoBook.findByCompanyIdOrderByIdAsc(companyId);
  }

  /**
   * Whether an enabled auto-book rule covers a product and segment (BRNB.076).
   *
   * @param companyId company
   * @param productCode product
   * @param segment market segment
   * @return true when the account is booked automatically
   */
  @Transactional(readOnly = true)
  public boolean autoBooks(Long companyId, String productCode, String segment) {
    return autoBookRules(companyId).stream().anyMatch(r -> r.matches(productCode, segment));
  }

  /**
   * Adds an auto-book rule.
   *
   * @param companyId company
   * @param criteria criteria
   * @return rule
   */
  public AutoBookRule createAutoBookRule(Long companyId, AutoBookRule.Criteria criteria) {
    AutoBookRule rule = autoBook.save(new AutoBookRule(companyId, criteria));
    audit.record(AUTO_BOOK, rule.getId(), AuditAction.CREATE, describe(rule));
    return rule;
  }

  /**
   * Changes an auto-book rule.
   *
   * @param id rule
   * @param criteria criteria
   * @return rule
   */
  public AutoBookRule updateAutoBookRule(Long id, AutoBookRule.Criteria criteria) {
    AutoBookRule rule =
        autoBook.findById(id).orElseThrow(() -> new ResourceNotFoundException(AUTO_BOOK, id));
    rule.apply(criteria);
    audit.record(AUTO_BOOK, rule.getId(), AuditAction.UPDATE, describe(rule));
    return rule;
  }

  /**
   * Incentive rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<IncentiveRule> incentiveRules(Long companyId) {
    return incentives.findByCompanyIdOrderByIdAsc(companyId);
  }

  /**
   * Whether a booking is incentive eligible: an active rule matches its product, segment, channel
   * and booking date (BRNB.107).
   *
   * @param companyId company
   * @param facts product, segment and channel
   * @param date booking date
   * @return incentive flag
   */
  @Transactional(readOnly = true)
  public boolean incentiveEligible(Long companyId, RuleFacts facts, LocalDate date) {
    return incentiveRules(companyId).stream()
        .anyMatch(r -> r.matches(facts.productCode(), facts.segment(), facts.channel(), date));
  }

  /**
   * Adds an incentive rule.
   *
   * @param companyId company
   * @param criteria criteria
   * @return rule
   */
  public IncentiveRule createIncentiveRule(Long companyId, IncentiveRule.Criteria criteria) {
    IncentiveRule rule = incentives.save(new IncentiveRule(companyId, criteria));
    audit.record(INCENTIVE, rule.getId(), AuditAction.CREATE, rule.getDescription());
    return rule;
  }

  /**
   * Changes an incentive rule.
   *
   * @param id rule
   * @param criteria criteria
   * @return rule
   */
  public IncentiveRule updateIncentiveRule(Long id, IncentiveRule.Criteria criteria) {
    IncentiveRule rule =
        incentives.findById(id).orElseThrow(() -> new ResourceNotFoundException(INCENTIVE, id));
    rule.apply(criteria);
    audit.record(INCENTIVE, rule.getId(), AuditAction.UPDATE, rule.getDescription());
    return rule;
  }

  private static String describe(AutoBookRule rule) {
    return rule.getDescription()
        + " (product "
        + (rule.getProductCode() == null ? "any" : rule.getProductCode())
        + ", segment "
        + (rule.getMarketSegment() == null ? "any" : rule.getMarketSegment())
        + (rule.isEnabled() ? ", enabled)" : ", disabled)");
  }

  /**
   * What a rule is matched on.
   *
   * @param productCode product
   * @param segment market segment
   * @param channel source channel
   */
  public record RuleFacts(String productCode, String segment, String channel) {}
}
