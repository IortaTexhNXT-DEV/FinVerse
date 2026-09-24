package com.iortatechnxt.brokerverse.underwriting.demo;

import static com.iortatechnxt.brokerverse.underwriting.demo.DemoPolicyGenerator.CHECKER;
import static com.iortatechnxt.brokerverse.underwriting.demo.DemoPolicyGenerator.MAKER;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.underwriting.demo.DemoCatalog.ProductProfile;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.service.EndorsementService;
import com.iortatechnxt.brokerverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyService;
import com.iortatechnxt.brokerverse.underwriting.service.ProductService;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * DEMO PROFILE ONLY: creates the underwriting demo portfolio of the demo company (FVI) at start-up
 * through the underwriting services, so journals, open items, debit notes and the accounting event
 * register are real. Products for eight lines of business, about 150 policies issued January to
 * September 2026 with endorsements, refunds and cancellations, quotations in every status and a
 * marine open cover with certificates. The underwriter "uw" is the maker and the finance manager
 * "fmanager" the approver.
 *
 * <p>Idempotent: nothing is created when the company already has policies.
 */
@Component
@Profile("demo")
@Order(10)
public class UnderwritingDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(UnderwritingDemoData.class);
  private static final String DEMO_COMPANY = "FVI";
  private static final String HEAD_OFFICE = "HO";

  private final OrganizationService organization;
  private final PolicyRepository policyRepository;
  private final ProductService products;
  private final DemoUserContext users;
  private final DemoPolicyGenerator policies;
  private final DemoQuotationsAndCovers quotationsAndCovers;

  /**
   * Creates the loader.
   *
   * @param organization companies and branches
   * @param policyRepository policies (idempotency check)
   * @param products products
   * @param policyService policies
   * @param approvals approvals
   * @param endorsements endorsements
   * @param quotations quotations
   * @param openCovers open covers
   * @param users demo user context
   */
  public UnderwritingDemoData(
      OrganizationService organization,
      PolicyRepository policyRepository,
      ProductService products,
      PolicyService policyService,
      PolicyApprovalService approvals,
      EndorsementService endorsements,
      QuotationService quotations,
      OpenCoverService openCovers,
      DemoUserContext users) {
    this.organization = organization;
    this.policyRepository = policyRepository;
    this.products = products;
    this.users = users;
    this.policies = new DemoPolicyGenerator(users, policyService, approvals, endorsements);
    this.quotationsAndCovers =
        new DemoQuotationsAndCovers(users, quotations, openCovers, policyService, approvals);
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> DEMO_COMPANY.equals(c.getCode()))
        .findFirst()
        .map(Company::getId)
        .ifPresent(this::loadIfEmpty);
  }

  private void loadIfEmpty(Long companyId) {
    if (policyRepository.countByCompanyId(companyId) > 0) {
      LOG.info("Underwriting demo data already present, skipped");
    } else {
      load(companyId);
    }
  }

  /**
   * Creates the demo portfolio (no idempotency check).
   *
   * @param companyId demo company
   * @return policies created directly (excluding converted quotations and certificates)
   */
  public List<Policy> load(Long companyId) {
    Map<String, Product> byCode = ensureProducts(companyId);
    Map<String, Long> branches =
        organization.listBranches(companyId).stream()
            .collect(Collectors.toMap(Branch::getCode, Branch::getId));
    List<Policy> created = policies.generate(companyId, branches, byCode);
    Long headOffice = branches.get(HEAD_OFFICE);
    int quotations = quotationsAndCovers.quotations(companyId, headOffice, byCode);
    quotationsAndCovers.openCover(companyId, headOffice, byCode.get("MARINE-CGO"));
    LOG.info(
        "Underwriting demo data created: {} products, {} policies, {} quotations, 1 open cover",
        byCode.size(),
        created.size(),
        quotations);
    return created;
  }

  private Map<String, Product> ensureProducts(Long companyId) {
    Map<String, Product> byCode = new HashMap<>();
    products.list(companyId).forEach(p -> byCode.put(p.getCode(), p));
    for (ProductProfile profile : DemoCatalog.PRODUCTS) {
      if (!byCode.containsKey(profile.code())) {
        Product created =
            users.runAs(
                MAKER, () -> products.create(DemoCatalog.productRequest(companyId, profile)));
        byCode.put(profile.code(), users.runAs(CHECKER, () -> products.authorize(created.getId())));
      }
    }
    return byCode;
  }
}
