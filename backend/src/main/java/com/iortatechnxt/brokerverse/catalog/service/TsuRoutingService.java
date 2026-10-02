package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuFacts;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRuleRepository;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule-based TSU involvement (BRNB.098): answers "does this account need TSU clearance?" from the
 * product's TSU involvement and the routing rules (package / non-package, fleet, locations, TSI,
 * endorsement type). The criteria are parked (Q04); the defaults are seeded in V812.
 */
@Service
@Transactional
public class TsuRoutingService {

  /** Decision code when the total sum insured exceeds the product's package TSI limit. */
  public static final String PACKAGE_LIMIT = "PACKAGE_TSI_LIMIT";

  private final TsuRuleRepository rules;
  private final ProductCatalogService catalog;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param rules routing rules
   * @param catalog lines (rule validation)
   * @param audit audit trail
   */
  public TsuRoutingService(
      TsuRuleRepository rules, ProductCatalogService catalog, AuditTrailService audit) {
    this.rules = rules;
    this.catalog = catalog;
    this.audit = audit;
  }

  /**
   * Every rule in evaluation order.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<TsuRule> rules() {
    return rules.findAllByOrderByPriorityAscCodeAsc();
  }

  /**
   * Adds a rule, pending authorization.
   *
   * @param code code
   * @param criteria criteria
   * @return rule
   */
  public TsuRule create(String code, TsuCriteria criteria) {
    if (rules.findByCode(code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.TSU_RULE.label(), code);
    }
    validate(criteria);
    TsuRule saved = rules.save(new TsuRule(code, criteria));
    audit.record(CatalogKind.TSU_RULE.label(), code, AuditAction.CREATE, criteria.description());
    return saved;
  }

  /**
   * Changes a rule, pending authorization.
   *
   * @param id rule
   * @param criteria criteria
   * @return rule
   */
  public TsuRule update(Long id, TsuCriteria criteria) {
    TsuRule rule =
        rules
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.TSU_RULE.label(), id));
    validate(criteria);
    rule.update(criteria);
    audit.record(
        CatalogKind.TSU_RULE.label(), rule.getCode(), AuditAction.UPDATE, criteria.description());
    return rule;
  }

  private void validate(TsuCriteria criteria) {
    if (criteria.lineCode() != null) {
      catalog.requireLine(criteria.lineCode());
    }
  }

  /**
   * Whether an account needs TSU clearance, and why.
   *
   * @param product product
   * @param facts account facts
   * @return decision with the matching rule
   */
  @Transactional(readOnly = true)
  public TsuDecision evaluate(RiskProduct product, TsuFacts facts) {
    if (product.getTsuInvolvement() == TsuInvolvement.ALWAYS) {
      return new TsuDecision(true, null, "Product " + product.getCode() + " is always TSU-cleared");
    }
    if (product.getTsuInvolvement() == TsuInvolvement.NEVER) {
      return TsuDecision.NOT_REQUIRED;
    }
    if (product.exceedsPackageLimit(facts.totalSumInsured())) {
      return new TsuDecision(
          true,
          PACKAGE_LIMIT,
          "Total sum insured above the package limit of "
              + product.getMaxSumInsured().toPlainString()
              + " for "
              + product.getCode());
    }
    return rules().stream()
        .filter(r -> r.isActive() && r.matches(facts))
        .findFirst()
        .map(r -> new TsuDecision(true, r.getCode(), r.getDescription()))
        .orElse(TsuDecision.NOT_REQUIRED);
  }

  /**
   * TSU routing decision.
   *
   * @param required whether TSU must clear the account
   * @param ruleCode matching rule, null when decided by the product or not required
   * @param reason explanation shown to users
   */
  public record TsuDecision(boolean required, String ruleCode, String reason) {

    /** No TSU involvement. */
    public static final TsuDecision NOT_REQUIRED =
        new TsuDecision(false, null, "No TSU routing rule applies");
  }
}
