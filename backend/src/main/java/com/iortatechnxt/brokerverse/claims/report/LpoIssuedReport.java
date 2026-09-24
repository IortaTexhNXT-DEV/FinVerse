package com.iortatechnxt.brokerverse.claims.report;

import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_CLAIM;
import static com.iortatechnxt.brokerverse.claims.report.ClaimReportSupport.K_PRODUCT;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.Lpo;
import com.iortatechnxt.brokerverse.claims.domain.LpoStatus;
import com.iortatechnxt.brokerverse.claims.service.LpoService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * PGIBR082 LPO Issued (OD and TP): issued local purchase orders of motor claims with a date of loss
 * in a period, one row per claim and garage with the number of LPOs, gross, discount and net (net =
 * gross − discount). Cancelled LPOs are excluded. Claim currency. Branch &gt; Garage &gt; Product.
 */
@Component
public class LpoIssuedReport implements ReportDefinition {

  private static final String BRANCH = "branchId";
  private static final String GARAGE = "garageCode";
  private static final String PRODUCT = "productCode";
  private static final String COVER = "cover";
  private static final String ALL = "ALL";
  private static final String K_GARAGE = "garage";

  private final LpoService lpos;
  private final ClaimReportSupport support;

  /**
   * Creates the report.
   *
   * @param lpos LPO register
   * @param support claims report support
   */
  public LpoIssuedReport(LpoService lpos, ClaimReportSupport support) {
    this.lpos = lpos;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(
        ParameterSpec.required(ClaimReportSupport.COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.optional(BRANCH, "Branch", ParameterType.BRANCH));
    params.add(ParameterSpec.optional(GARAGE, "Garage Code", ParameterType.TEXT));
    params.add(ParameterSpec.optional(PRODUCT, "Product Code", ParameterType.TEXT));
    params.add(ParameterSpec.select(COVER, "Cover", List.of(ALL, "OD", "TP"), ALL));
    params.addAll(UwReportSupport.dateRange("Loss Date From", "Loss Date To"));
    return new ReportMetadata(
        "PGIBR082",
        "LPO Issued (OD and TP)",
        ReportCategory.CLAIMS,
        "Local purchase orders issued to garages for motor claims",
        params,
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ClaimReportSupport.COMPANY);
    LocalDate from = p.date(ClaimReportSupport.FROM);
    LocalDate to = p.date(ClaimReportSupport.TO);
    Map<Long, String> branches = support.branchCodes(companyId);
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    for (Lpo l : lpos.forCompany(companyId)) {
      Claim c = l.getClaim();
      LocalDate loss = c.getLoss().getLossDate();
      if (l.getStatus() != LpoStatus.ISSUED
          || loss.isBefore(from)
          || loss.isAfter(to)
          || !matches(p, c, l)) {
        continue;
      }
      Map<String, Object> m =
          rows.computeIfAbsent(
              c.getClaimNo() + "|" + l.getGarage().getCode(), k -> row(c, l, branches));
      m.merge("lpos", 1, (a, b) -> (Integer) a + (Integer) b);
      m.merge("gross", l.getGrossAmount(), UwReportSupport::add);
      m.merge("discount", l.getDiscountAmount(), UwReportSupport::add);
      m.merge("net", l.getNetAmount(), UwReportSupport::add);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_CLAIM, "Claim No"),
            ReportColumn.count("lpos", "LPOs Issued"),
            ReportColumn.amount("gross", "Gross LPO"),
            ReportColumn.amount("discount", "Discount"),
            ReportColumn.amount("net", "Net LPO"))
        .groupBy(K_BRANCH, ClaimReportSupport.BRANCH_LABEL)
        .groupBy(K_GARAGE, "Garage")
        .groupBy(K_PRODUCT, ClaimReportSupport.PRODUCT_LABEL)
        .rows(new ArrayList<>(rows.values()))
        .note("Net LPO = gross - discount; cancelled LPOs excluded; claim currency.")
        .build();
  }

  private static boolean matches(ReportParameters p, Claim c, Lpo l) {
    Optional<Long> branch = p.optionalLong(BRANCH);
    String cover = p.optionalText(COVER).orElse(ALL);
    return branch.map(b -> b.equals(c.getBranchId())).orElse(true)
        && p.optionalText(GARAGE).map(g -> g.equals(l.getGarage().getCode())).orElse(true)
        && p.optionalText(PRODUCT).map(pr -> pr.equals(c.getPolicy().getProductCode())).orElse(true)
        && (ALL.equals(cover) || cover.equals(l.getCoverType().name()));
  }

  private static Map<String, Object> row(Claim c, Lpo l, Map<Long, String> branches) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(K_BRANCH, branches.getOrDefault(c.getBranchId(), String.valueOf(c.getBranchId())));
    m.put(K_GARAGE, l.getGarage().getCode() + " " + l.getGarage().getName());
    m.put(K_PRODUCT, c.getPolicy().getProductCode() + " " + c.getPolicy().getProductName());
    m.put(K_CLAIM, c.getClaimNo());
    m.put("lpos", 0);
    m.put("gross", Money.zero());
    m.put("discount", Money.zero());
    m.put("net", Money.zero());
    return m;
  }
}
