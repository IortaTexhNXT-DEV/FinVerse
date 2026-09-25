package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.AutoBookRule;
import com.iortatechnxt.brokerverse.booking.domain.AutoBookRuleRepository;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveRule;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveRuleRepository;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveCriteriaService;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveCriteriaService.IncentiveFacts;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking rules maintained on the setup screen: auto-book rules (BRNB.076) and incentive
 * eligibility (BRNB.107). Every change is audited.
 *
 * <p>Product Maintenance (PMADD07/08; PRODUCT_MAINTENANCE_DESIGN section 9.4): incentive
 * eligibility is decided by the catalog incentive criteria ({@link IncentiveCriteriaService}); the
 * booking incentive rules were copied into the catalog (V871) and are frozen: they stay readable
 * and can no longer be created or changed here ({@code INCENTIVE_RULES_FROZEN}).
 */
@Service
@Transactional
public class BookingRuleService {

  private static final String AUTO_BOOK = "AutoBookRule";
  private static final String INCENTIVE = "IncentiveRule";

  private final AutoBookRuleRepository autoBook;
  private final IncentiveRuleRepository incentives;
  private final IncentiveCriteriaService criteria;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param autoBook auto-book rules
   * @param incentives frozen booking incentive rules (read-only)
   * @param criteria catalog incentive criteria
   * @param audit audit trail
   */
  public BookingRuleService(
      AutoBookRuleRepository autoBook,
      IncentiveRuleRepository incentives,
      IncentiveCriteriaService criteria,
      AuditTrailService audit) {
    this.autoBook = autoBook;
    this.incentives = incentives;
    this.criteria = criteria;
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
   * The codes of the catalog incentive criteria a booking matches (PMADD07; BRNB.107).
   *
   * @param companyId company
   * @param facts product, cover type, segment, channel and insurer
   * @param date booking date
   * @return criteria codes, empty when not eligible
   */
  @Transactional(readOnly = true)
  public List<String> incentiveCriteria(Long companyId, RuleFacts facts, LocalDate date) {
    return criteria.matching(
        companyId,
        new IncentiveFacts(
            facts.productCode(),
            facts.coverTypeCode(),
            facts.segment(),
            facts.channel(),
            facts.insurerCode(),
            date));
  }

  /**
   * Whether a booking is incentive eligible: at least one catalog incentive criterion matches
   * (PMADD07; BRNB.107).
   *
   * @param companyId company
   * @param facts product, cover type, segment, channel and insurer
   * @param date booking date
   * @return incentive flag
   */
  @Transactional(readOnly = true)
  public boolean incentiveEligible(Long companyId, RuleFacts facts, LocalDate date) {
    return !incentiveCriteria(companyId, facts, date).isEmpty();
  }

  /**
   * Refused: the booking incentive rules are frozen; incentive criteria are maintained in Product
   * Maintenance (PMADD07).
   *
   * @param companyId company
   * @param rule criteria
   * @return never
   */
  public IncentiveRule createIncentiveRule(Long companyId, IncentiveRule.Criteria rule) {
    throw frozen();
  }

  /**
   * Refused: the booking incentive rules are frozen (PMADD07).
   *
   * @param id rule
   * @param rule criteria
   * @return never
   */
  public IncentiveRule updateIncentiveRule(Long id, IncentiveRule.Criteria rule) {
    if (!incentives.existsById(id)) {
      throw new ResourceNotFoundException(INCENTIVE, id);
    }
    throw frozen();
  }

  private static BusinessRuleException frozen() {
    return new BusinessRuleException(
        "INCENTIVE_RULES_FROZEN",
        "Incentive rules are maintained in Product Maintenance > Incentive Criteria");
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
   * What an incentive criterion is matched on.
   *
   * @param productCode product
   * @param coverTypeCode cover type
   * @param segment market segment
   * @param channel source channel
   * @param insurerCode lead insurer
   */
  public record RuleFacts(
      String productCode,
      String coverTypeCode,
      String segment,
      String channel,
      String insurerCode) {}
}
