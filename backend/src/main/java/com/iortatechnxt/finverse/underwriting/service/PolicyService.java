package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.finverse.underwriting.domain.OpenCover;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.finverse.underwriting.domain.PolicySearchCriteria;
import com.iortatechnxt.finverse.underwriting.domain.PolicyTerms;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.RiskValues;
import com.iortatechnxt.finverse.underwriting.domain.UnderwritingSpecifications;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maker side of policies: premium preview, draft creation and maintenance, submission for approval.
 * Approval (with accounting) is in {@link PolicyApprovalService}.
 */
@Service
@Transactional
public class PolicyService {

  static final String ENTITY = "Policy";

  private final PolicyRepository policies;
  private final ProductService products;
  private final UnderwritingParties parties;
  private final PolicyPremiumCalculator calculator;
  private final UnderwritingNumbers numbers;
  private final OrganizationService organization;
  private final CurrencyService currencies;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param policies repository
   * @param products products
   * @param parties party resolution
   * @param calculator premium calculator
   * @param numbers document numbers
   * @param organization branches
   * @param currencies currencies
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PolicyService(
      PolicyRepository policies,
      ProductService products,
      UnderwritingParties parties,
      PolicyPremiumCalculator calculator,
      UnderwritingNumbers numbers,
      OrganizationService organization,
      CurrencyService currencies,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.policies = policies;
    this.products = products;
    this.parties = parties;
    this.calculator = calculator;
    this.numbers = numbers;
    this.organization = organization;
    this.currencies = currencies;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Searches policies.
   *
   * @param criteria filters
   * @param pageable paging
   * @return page of policies (product and parties loaded)
   */
  @Transactional(readOnly = true)
  public Page<Policy> search(PolicySearchCriteria criteria, Pageable pageable) {
    return policies.findAll(UnderwritingSpecifications.policies(criteria), pageable);
  }

  /**
   * Gets a policy with risks, product and parties.
   *
   * @param id id
   * @return policy
   */
  @Transactional(readOnly = true)
  public Policy get(Long id) {
    return policies
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Computes the premium of a policy request without saving anything.
   *
   * @param r request
   * @return premium figures
   */
  @Transactional(readOnly = true)
  public PremiumBreakdown preview(PolicyRequest r) {
    Product product = products.requireActive(r.companyId(), r.productId());
    Policy draft = new Policy("PREVIEW", terms(r, product), null, null);
    draft.replaceRisks(risks(r.risks()));
    return calculator.forPolicy(draft, commission(draft, r.commissionRate()));
  }

  /**
   * Creates a draft policy.
   *
   * @param r request
   * @return draft
   */
  public Policy create(PolicyRequest r) {
    Product product = products.requireActive(r.companyId(), r.productId());
    PolicyTerms terms = terms(r, product);
    String no = numbers.policy(product.getCode(), r.branchId(), r.issueDate());
    return createDraft(no, terms, risks(r.risks()), null, null, r.commissionRate());
  }

  /**
   * Creates and saves a draft policy (shared by direct entry, quotation conversion and marine
   * certificates).
   *
   * @param no allocated number
   * @param terms terms
   * @param risks risks
   * @param openCover open cover (certificates), else null
   * @param quotationId source quotation, else null
   * @param commissionOverride commission % entered, may be null
   * @return saved draft
   */
  Policy createDraft(
      String no,
      PolicyTerms terms,
      List<RiskValues> risks,
      OpenCover openCover,
      Long quotationId,
      BigDecimal commissionOverride) {
    Policy policy = new Policy(no, terms, openCover, quotationId);
    policy.replaceRisks(risks);
    policy.applyPremium(calculator.forPolicy(policy, commission(policy, commissionOverride)));
    Policy saved = policies.save(policy);
    audit.record(
        ENTITY,
        saved.getPolicyNo(),
        AuditAction.CREATE,
        "Created policy for " + saved.getInsuredName());
    return saved;
  }

  /**
   * Updates a draft policy and recomputes its premium.
   *
   * @param id id
   * @param r request
   * @return policy
   */
  public Policy update(Long id, PolicyRequest r) {
    Policy policy = get(id);
    policy.updateTerms(terms(r, policy.getProduct()));
    policy.replaceRisks(risks(r.risks()));
    policy.applyPremium(calculator.forPolicy(policy, commission(policy, r.commissionRate())));
    audit.record(ENTITY, policy.getPolicyNo(), AuditAction.UPDATE, "Updated draft policy");
    return policy;
  }

  /**
   * Submits a draft for approval.
   *
   * @param id id
   * @return policy
   */
  public Policy submit(Long id) {
    Policy policy = get(id);
    policy.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, policy.getPolicyNo(), AuditAction.SUBMIT, "Submitted for approval");
    return policy;
  }

  /**
   * Discards a draft.
   *
   * @param id id
   * @return policy
   */
  public Policy discard(Long id) {
    Policy policy = get(id);
    policy.discard();
    audit.record(ENTITY, policy.getPolicyNo(), AuditAction.DEACTIVATE, "Discarded draft");
    return policy;
  }

  private PolicyTerms terms(PolicyRequest r, Product product) {
    organization.requireActiveBranch(r.branchId());
    currencies.requireActive(r.currency());
    return new PolicyTerms(
        r.branchId(),
        product,
        parties.client(r.companyId(), r.customerCode()),
        r.insuredName(),
        r.sourceType(),
        parties.intermediary(r.companyId(), r.intermediaryCode()),
        r.issueDate(),
        r.periodFrom(),
        r.periodTo(),
        r.currency(),
        r.businessType(),
        r.sharePct(),
        parties.coinsurer(r.companyId(), r.coinsurerCode()),
        r.coinsuranceLeader(),
        r.discountRate(),
        r.loadingRate());
  }

  private BigDecimal commission(Policy policy, BigDecimal override) {
    return calculator.commissionRate(policy.getProduct(), policy.getIntermediary(), override);
  }

  private static List<RiskValues> risks(List<RiskRequest> requests) {
    return requests.stream().map(RiskRequest::toValues).toList();
  }
}
