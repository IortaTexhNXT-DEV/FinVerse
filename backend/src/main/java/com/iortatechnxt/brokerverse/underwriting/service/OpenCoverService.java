package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.underwriting.api.dto.CertificateRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.OpenCoverRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.BusinessType;
import com.iortatechnxt.brokerverse.underwriting.domain.OpenCover;
import com.iortatechnxt.brokerverse.underwriting.domain.OpenCoverRepository;
import com.iortatechnxt.brokerverse.underwriting.domain.OpenCoverTerms;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyTerms;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.RiskValues;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marine open covers (maker-checker master contracts) and the shipment certificates declared under
 * them. A certificate is a policy linked to its cover, approved and accounted for like any policy.
 */
@Service
@Transactional
public class OpenCoverService {

  static final String ENTITY = "OpenCover";

  private static final int DEFAULT_TRANSIT_DAYS = 60;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int RATE_SCALE = 8;

  private final OpenCoverRepository covers;
  private final PolicyRepository policyRepository;
  private final PolicyService policies;
  private final ProductService products;
  private final UnderwritingParties parties;
  private final UnderwritingNumbers numbers;
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param covers repository
   * @param policyRepository policies (declared totals, certificate lists)
   * @param policies policy creation
   * @param products products
   * @param parties party resolution
   * @param numbers document numbers
   * @param organization branches
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public OpenCoverService(
      OpenCoverRepository covers,
      PolicyRepository policyRepository,
      PolicyService policies,
      ProductService products,
      UnderwritingParties parties,
      UnderwritingNumbers numbers,
      OrganizationService organization,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.covers = covers;
    this.policyRepository = policyRepository;
    this.policies = policies;
    this.products = products;
    this.parties = parties;
    this.numbers = numbers;
    this.organization = organization;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists open covers.
   *
   * @param companyId company
   * @return covers, newest first
   */
  @Transactional(readOnly = true)
  public List<OpenCover> list(Long companyId) {
    return covers.findByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * Gets an open cover.
   *
   * @param id id
   * @return cover
   */
  @Transactional(readOnly = true)
  public OpenCover get(Long id) {
    return covers
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Certificates declared under a cover.
   *
   * @param id open cover
   * @return certificates, oldest first
   */
  @Transactional(readOnly = true)
  public List<Policy> certificates(Long id) {
    get(id);
    return policyRepository.findByOpenCoverIdOrderById(id);
  }

  /**
   * Creates an open cover (pending authorization).
   *
   * @param r request
   * @return cover
   */
  public OpenCover create(OpenCoverRequest r) {
    Product product = products.requireActive(r.companyId(), r.productId());
    organization.requireActiveBranch(r.branchId());
    OpenCoverTerms terms =
        new OpenCoverTerms(
            r.branchId(),
            product,
            parties.client(r.companyId(), r.customerCode()),
            r.insuredName(),
            r.periodFrom(),
            r.periodTo(),
            r.currency(),
            r.limitPerShipment(),
            r.annualLimit(),
            r.rate(),
            r.cargoDescription());
    OpenCover saved =
        covers.save(new OpenCover(numbers.openCover(r.branchId(), r.periodFrom()), terms));
    audit.record(ENTITY, saved.getOpenCoverNo(), AuditAction.CREATE, "Open cover created");
    return saved;
  }

  /**
   * Authorizes an open cover (checker).
   *
   * @param id id
   * @return cover
   */
  public OpenCover authorize(Long id) {
    OpenCover cover = get(id);
    cover.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, cover.getOpenCoverNo(), AuditAction.AUTHORIZE, "Authorized");
    return cover;
  }

  /**
   * Declares a shipment: creates a draft certificate within the cover's period and limits.
   *
   * @param id open cover
   * @param r declaration
   * @return draft certificate
   */
  public Policy issueCertificate(Long id, CertificateRequest r) {
    OpenCover cover = get(id);
    RiskValues shipment = shipment(cover, r.shipment());
    LocalDate sailDate =
        shipment.marine() != null && shipment.marine().sailDate() != null
            ? shipment.marine().sailDate()
            : r.issueDate();
    BigDecimal declared =
        policyRepository.declaredSumInsured(
            id,
            EnumSet.of(PolicyStatus.DRAFT, PolicyStatus.PENDING_APPROVAL, PolicyStatus.APPROVED));
    cover.requireDeclarable(sailDate, shipment.sumInsured(), declared);
    Product product = products.requireActive(cover.getCompanyId(), cover.getProduct().getId());
    SourceType source = r.sourceType() != null ? r.sourceType() : SourceType.DIRECT;
    int transit = r.transitDays() != null ? r.transitDays() : DEFAULT_TRANSIT_DAYS;
    PolicyTerms terms =
        new PolicyTerms(
            cover.getBranchId(),
            product,
            cover.getCustomer(),
            cover.getInsuredName(),
            source,
            parties.intermediary(cover.getCompanyId(), r.intermediaryCode()),
            r.issueDate(),
            sailDate,
            sailDate.plusDays(transit),
            cover.getCurrency(),
            BusinessType.DIRECT,
            HUNDRED,
            null,
            false,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    return policies.createDraft(
        numbers.certificate(cover.getBranchId(), r.issueDate()),
        terms,
        List.of(shipment),
        cover,
        null,
        null);
  }

  private static RiskValues shipment(OpenCover cover, RiskRequest r) {
    RiskValues v = r.toValues();
    if (r.premium() != null || r.rate() != null) {
      return v;
    }
    BigDecimal premium =
        Money.round(
            v.sumInsured()
                .multiply(cover.getRate())
                .divide(HUNDRED, RATE_SCALE, RoundingMode.HALF_EVEN));
    return new RiskValues(
        v.description(),
        v.sumInsured(),
        cover.getRate(),
        premium,
        v.occupation(),
        v.accumulationZone(),
        v.marine());
  }
}
