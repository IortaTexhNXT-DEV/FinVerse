package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Calculation;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveScheme;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveScheme.Terms;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveSchemeRepository;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveTier;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Incentive schemes and their tier editor (CMRID.005/006, {@code INCENTIVE_MANAGE}): No Touch, Top
 * Up and Motor Mania are seeded without tiers until BDOI gives the targets, rates, multipliers and
 * amounts (OQ39); new schemes can be added. A tier must carry the values its calculation needs.
 */
@Service
@Transactional
public class IncentiveSchemeService {

  private static final String ENTITY = "IncentiveScheme";

  private final IncentiveSchemeRepository schemes;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param schemes schemes
   * @param audit audit trail
   */
  public IncentiveSchemeService(IncentiveSchemeRepository schemes, AuditTrailService audit) {
    this.schemes = schemes;
    this.audit = audit;
  }

  /**
   * Schemes of a company.
   *
   * @param companyId company
   * @return schemes by code, with tiers
   */
  @Transactional(readOnly = true)
  public List<IncentiveScheme> list(Long companyId) {
    return schemes.findByCompanyIdOrderByCodeAsc(companyId);
  }

  /**
   * A scheme with its tiers.
   *
   * @param id scheme
   * @return scheme
   */
  @Transactional(readOnly = true)
  public IncentiveScheme require(Long id) {
    return schemes
        .findWithTiersById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Adds a scheme.
   *
   * @param companyId company
   * @param code code
   * @param terms terms and tiers
   * @return scheme
   */
  public IncentiveScheme create(Long companyId, String code, Terms terms) {
    String c = code.strip().toUpperCase(Locale.ROOT);
    if (schemes.findByCompanyIdAndCode(companyId, c).isPresent()) {
      throw new DuplicateResourceException(ENTITY, c);
    }
    validate(terms);
    IncentiveScheme scheme = schemes.save(new IncentiveScheme(companyId, c, terms));
    audit.record(ENTITY, c, AuditAction.CREATE, describe(scheme));
    return scheme;
  }

  /**
   * Changes a scheme and replaces its tiers.
   *
   * @param id scheme
   * @param terms terms and tiers
   * @return scheme
   */
  public IncentiveScheme update(Long id, Terms terms) {
    IncentiveScheme scheme = require(id);
    validate(terms);
    scheme.update(terms);
    audit.record(ENTITY, scheme.getCode(), AuditAction.UPDATE, describe(scheme));
    return scheme;
  }

  private static void validate(Terms terms) {
    if (terms.effectiveFrom() != null
        && terms.effectiveTo() != null
        && terms.effectiveTo().isBefore(terms.effectiveFrom())) {
      throw new BusinessRuleException("INCENTIVE_SCHEME_DATES", "The scheme ends before it starts");
    }
    terms.tiers().forEach(t -> validate(terms.calculation(), t));
  }

  private static void validate(Calculation calculation, IncentiveTier t) {
    boolean tiered = calculation == Calculation.TARGET_TIERED;
    boolean ok =
        tiered
            ? positive(t.minProduction(), true) && positive(t.ratePercent(), false)
            : positive(t.minBasicPremium(), true) && positive(t.fixedAmount(), false);
    if (!ok) {
      throw new BusinessRuleException(
          "INCENTIVE_TIER_INVALID",
          tiered
              ? "Each tier needs a production target and a rate above zero"
              : "Each tier needs a minimum basic premium and a fixed amount above zero");
    }
  }

  private static boolean positive(BigDecimal value, boolean zeroAllowed) {
    return value != null && (zeroAllowed ? value.signum() >= 0 : value.signum() > 0);
  }

  private static String describe(IncentiveScheme s) {
    return s.getName()
        + " "
        + s.getCalculation()
        + ", "
        + s.getTiers().size()
        + " tier(s)"
        + (s.isActive() ? ", active" : ", inactive");
  }
}
