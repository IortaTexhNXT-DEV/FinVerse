package com.iortatechnxt.finverse.underwriting;

import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.TestData;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.ProductRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import com.iortatechnxt.finverse.underwriting.domain.UprBasis;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import com.iortatechnxt.finverse.underwriting.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Builds underwriting test data through the services (maker "uw", checker "fmanager"). */
@Component
public class UwFixtures {

  public static final LocalDate ISSUE = LocalDate.of(2026, 3, 10);

  private final ProductService products;
  private final PolicyService policies;
  private final PolicyApprovalService approvals;
  private final AsUser as;
  private final TestData data;

  UwFixtures(
      ProductService products,
      PolicyService policies,
      PolicyApprovalService approvals,
      AsUser as,
      TestData data) {
    this.products = products;
    this.policies = policies;
    this.approvals = approvals;
    this.as = as;
    this.data = data;
  }

  public ProductService productService() {
    return products;
  }

  public Long companyId() {
    return data.company().getId();
  }

  public Long branchId() {
    return data.branch("HO").getId();
  }

  public static String uniqueCode(String prefix) {
    return prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
  }

  public ProductRequest productRequest(String code, String lob, boolean openCover) {
    return new ProductRequest(
        companyId(),
        code,
        "Test " + lob + " product",
        lob,
        new BigDecimal("15"),
        UprBasis.DAYS_365,
        new BigDecimal("12.5"),
        new BigDecimal("12"),
        new BigDecimal("0.75"),
        "FIRE".equals(lob) ? new BigDecimal("2") : BigDecimal.ZERO,
        BigDecimal.ZERO,
        new BigDecimal("250"),
        openCover);
  }

  /** Creates and authorizes a product. */
  public Product product(String lob, boolean openCover) {
    Product created =
        as.run("uw", () -> products.create(productRequest(uniqueCode("T"), lob, openCover)));
    return as.run("fmanager", () -> products.authorize(created.getId()));
  }

  public RiskRequest risk(String si, String premium, String zone) {
    return new RiskRequest(
        "Warehouse building and stocks",
        new BigDecimal(si),
        null,
        new BigDecimal(premium),
        "Warehouse",
        zone,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public PolicyRequest request(
      Product product, SourceType source, String intermediary, List<RiskRequest> risks) {
    return new PolicyRequest(
        companyId(),
        branchId(),
        product.getId(),
        "C-000201",
        "Luzon Steel Manufacturing Corp.",
        source,
        intermediary,
        ISSUE,
        ISSUE,
        ISSUE.plusYears(1).minusDays(1),
        "PHP",
        BusinessType.DIRECT,
        new BigDecimal("100"),
        null,
        false,
        new BigDecimal("10"),
        new BigDecimal("5"),
        null,
        risks);
  }

  public PolicyRequest brokerRequest(Product product) {
    return request(
        product, SourceType.BROKER, "B-0001", List.of(risk("10000000", "100000", "NCR-1")));
  }

  /** Creates (uw), submits (uw) and approves (fmanager) a policy. */
  public Policy issue(PolicyRequest request, LocalDate accountingDate) {
    Policy draft = as.run("uw", () -> policies.create(request));
    as.run("uw", () -> policies.submit(draft.getId()));
    return as.run("fmanager", () -> approvals.approvePolicy(draft.getId(), accountingDate));
  }
}
