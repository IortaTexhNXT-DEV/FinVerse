package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.underwriting.api.dto.ConvertQuotationRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.IterationRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.QuotationRequest;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyTerms;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.Quotation;
import com.iortatechnxt.finverse.underwriting.domain.QuotationIteration;
import com.iortatechnxt.finverse.underwriting.domain.QuotationRepository;
import com.iortatechnxt.finverse.underwriting.domain.QuotationStatus;
import com.iortatechnxt.finverse.underwriting.domain.QuotationTerms;
import com.iortatechnxt.finverse.underwriting.domain.RiskValues;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quotations: creation, negotiation iterations, maker-checker approval, conversion into a draft
 * policy and expiry of lapsed offers.
 */
@Service
@Transactional
public class QuotationService {

  static final String ENTITY = "Quotation";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int RATE_SCALE = 8;
  private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
  private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

  private final QuotationRepository quotations;
  private final ProductService products;
  private final UnderwritingParties parties;
  private final PolicyService policies;
  private final UnderwritingNumbers numbers;
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param quotations repository
   * @param products products
   * @param parties party resolution
   * @param policies policy creation (conversion)
   * @param numbers document numbers
   * @param organization branches
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public QuotationService(
      QuotationRepository quotations,
      ProductService products,
      UnderwritingParties parties,
      PolicyService policies,
      UnderwritingNumbers numbers,
      OrganizationService organization,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.quotations = quotations;
    this.products = products;
    this.parties = parties;
    this.policies = policies;
    this.numbers = numbers;
    this.organization = organization;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists quotations.
   *
   * @param companyId company
   * @param status status, null for all
   * @param from issue date from, null for open
   * @param to issue date to, null for open
   * @return quotations, newest first
   */
  @Transactional(readOnly = true)
  public List<Quotation> search(
      Long companyId, QuotationStatus status, LocalDate from, LocalDate to) {
    Collection<QuotationStatus> statuses =
        status == null ? EnumSet.allOf(QuotationStatus.class) : EnumSet.of(status);
    return quotations.search(
        companyId, statuses, from == null ? MIN_DATE : from, to == null ? MAX_DATE : to);
  }

  /**
   * Gets a quotation with iterations.
   *
   * @param id id
   * @return quotation
   */
  @Transactional(readOnly = true)
  public Quotation get(Long id) {
    return quotations
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Creates a draft quotation with its first iteration.
   *
   * @param r request (iteration mandatory)
   * @return quotation
   */
  public Quotation create(QuotationRequest r) {
    if (r.iteration() == null) {
      throw new BusinessRuleException("ITERATION_REQUIRED", "Quotation figures are required");
    }
    Product product = products.requireActive(r.companyId(), r.productId());
    QuotationTerms terms = terms(r, product);
    Quotation q = new Quotation(numbers.quotation(r.branchId(), r.issueDate()), terms);
    q.addIteration(r.iteration().toValues(), currentUser.username(), clock.instant());
    Quotation saved = quotations.save(q);
    audit.record(ENTITY, saved.getQuotationNo(), AuditAction.CREATE, "Quotation created");
    return saved;
  }

  /**
   * Updates the terms of a draft quotation.
   *
   * @param id id
   * @param r request
   * @return quotation
   */
  public Quotation update(Long id, QuotationRequest r) {
    Quotation q = get(id);
    q.updateTerms(terms(r, q.getProduct()));
    audit.record(ENTITY, q.getQuotationNo(), AuditAction.UPDATE, "Terms updated");
    return q;
  }

  /**
   * Adds a negotiation iteration (the quotation returns to draft).
   *
   * @param id id
   * @param r figures
   * @return quotation
   */
  public Quotation iterate(Long id, IterationRequest r) {
    Quotation q = get(id);
    QuotationIteration it = q.addIteration(r.toValues(), currentUser.username(), clock.instant());
    audit.record(
        ENTITY, q.getQuotationNo(), AuditAction.UPDATE, "Iteration " + it.getIterationNo());
    return q;
  }

  /**
   * Submits for approval.
   *
   * @param id id
   * @return quotation
   */
  public Quotation submit(Long id) {
    Quotation q = get(id);
    q.submit(currentUser.username());
    audit.record(ENTITY, q.getQuotationNo(), AuditAction.SUBMIT, "Submitted for approval");
    return q;
  }

  /**
   * Approves (checker).
   *
   * @param id id
   * @return quotation
   */
  public Quotation approve(Long id) {
    Quotation q = get(id);
    q.approve(currentUser.username(), clock.instant());
    audit.record(ENTITY, q.getQuotationNo(), AuditAction.AUTHORIZE, "Approved");
    return q;
  }

  /**
   * Rejects (checker).
   *
   * @param id id
   * @param reason reason
   * @return quotation
   */
  public Quotation reject(Long id, String reason) {
    Quotation q = get(id);
    q.reject(currentUser.username(), clock.instant(), reason);
    audit.record(ENTITY, q.getQuotationNo(), AuditAction.REJECT, reason);
    return q;
  }

  /**
   * Converts an approved, still valid quotation into a draft policy with one risk carrying the
   * quoted sum insured and premium; discount and loading become rates on the policy.
   *
   * @param id id
   * @param r conversion options
   * @return the draft policy
   */
  public Policy convert(Long id, ConvertQuotationRequest r) {
    Quotation q = get(id);
    LocalDate issueDate = r.issueDate() != null ? r.issueDate() : LocalDate.now(clock);
    q.requireConvertible(issueDate);
    QuotationIteration it = q.current();
    Product product = products.requireActive(q.getCompanyId(), q.getProduct().getId());
    boolean coinsured = q.getSharePct().compareTo(HUNDRED) < 0;
    PolicyTerms terms =
        new PolicyTerms(
            q.getBranchId(),
            product,
            q.getCustomer(),
            q.getInsuredName(),
            q.getSourceType(),
            q.getIntermediary(),
            issueDate,
            q.getPeriodFrom(),
            q.getPeriodTo(),
            q.getCurrency(),
            coinsured ? BusinessType.DIRECT_WITH_COINSURANCE : BusinessType.DIRECT,
            q.getSharePct(),
            parties.coinsurer(q.getCompanyId(), r.coinsurerCode()),
            r.coinsuranceLeader(),
            ratio(it.getDiscount(), it.getGrossPremium()),
            ratio(it.getLoading(), it.getGrossPremium()));
    RiskValues risk =
        new RiskValues(
            r.riskDescription() != null && !r.riskDescription().isBlank()
                ? r.riskDescription()
                : "As per quotation " + q.getQuotationNo(),
            it.getSumInsured(),
            ratio(it.getGrossPremium(), it.getSumInsured()),
            it.getGrossPremium(),
            null,
            null,
            null);
    Policy policy =
        policies.createDraft(
            numbers.policy(product.getCode(), q.getBranchId(), issueDate),
            terms,
            List.of(risk),
            null,
            q.getId(),
            q.getCommissionRate());
    q.markConverted(policy.getId(), issueDate);
    audit.record(
        ENTITY, q.getQuotationNo(), AuditAction.UPDATE, "Converted to " + policy.getPolicyNo());
    return policy;
  }

  /**
   * Expires open quotations whose validity has lapsed.
   *
   * @param companyId company
   * @param asOf date
   * @return number of quotations expired
   */
  public int expireLapsed(Long companyId, LocalDate asOf) {
    int count = 0;
    for (Quotation q :
        quotations.findByCompanyIdAndStatusIn(
            companyId,
            EnumSet.of(
                QuotationStatus.DRAFT,
                QuotationStatus.PENDING_APPROVAL,
                QuotationStatus.APPROVED))) {
      if (q.expireIfLapsed(asOf)) {
        audit.record(ENTITY, q.getQuotationNo(), AuditAction.CLOSE, "Expired");
        count++;
      }
    }
    return count;
  }

  private QuotationTerms terms(QuotationRequest r, Product product) {
    organization.requireActiveBranch(r.branchId());
    return new QuotationTerms(
        r.branchId(),
        product,
        parties.client(r.companyId(), r.customerCode()),
        r.insuredName(),
        r.sourceType(),
        parties.intermediary(r.companyId(), r.intermediaryCode()),
        r.issueDate(),
        r.validityDays(),
        r.periodFrom(),
        r.periodTo(),
        r.currency(),
        r.sharePct(),
        r.commissionRate() != null ? r.commissionRate() : product.getDefaultCommissionRate());
  }

  private static BigDecimal ratio(BigDecimal part, BigDecimal whole) {
    if (whole.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return part.multiply(HUNDRED).divide(whole, RATE_SCALE, RoundingMode.HALF_EVEN);
  }
}
