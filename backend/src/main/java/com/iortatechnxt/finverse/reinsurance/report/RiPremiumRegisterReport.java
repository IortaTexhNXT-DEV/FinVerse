package com.iortatechnxt.finverse.reinsurance.report;

import static com.iortatechnxt.finverse.reinsurance.report.RiReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.reinsurance.report.RiReportSupport.K_CLASS;
import static com.iortatechnxt.finverse.reinsurance.report.RiReportSupport.K_UW_YEAR;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.finverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIR0637 Reinsurance Premium Register: one row per risk of every ceded transaction with an RI
 * accounting date in the period: sum insured (100 % and ours), PML (FinVerse rule: the sum insured
 * when no PML is captured), premium, and the premium and SI % of the retention, quota share,
 * surplus and facultative layers. UW Year &gt; Branch &gt; Class. Only policies with an RI
 * allocation.
 */
@Component
public class RiPremiumRegisterReport implements ReportDefinition {

  private static final String TREATY_TYPE = "treatyType";
  private static final String RETENTION = "retention";
  private static final String QUOTA_SHARE = "qs";
  private static final String SURPLUS = "surplus";

  private final CessionRepository cessions;
  private final RiReportSupport support;

  /**
   * Creates the report.
   *
   * @param cessions cessions
   * @param support shared support
   */
  public RiPremiumRegisterReport(CessionRepository cessions, RiReportSupport support) {
    this.cessions = cessions;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = RiReportSupport.rangeParams();
    params.add(
        ParameterSpec.select(
            TREATY_TYPE,
            "Treaty Type",
            List.of(RiReportSupport.ALL, "QUOTA_SHARE", "SURPLUS", "FAC"),
            RiReportSupport.ALL));
    params.addAll(RiReportSupport.dateRange("RI Accounting Date From", "RI Accounting Date To"));
    params.add(RiReportSupport.statusParam());
    return new ReportMetadata(
        "PGIR0637",
        "Reinsurance Premium Register",
        ReportCategory.REINSURANCE,
        "Sum insured and premium split per risk: retention, quota share, surplus and FAC",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    List<Cession> list =
        cessions.findByCompanyIdAndRiDateBetweenOrderByRiDateAscCessionNoAsc(
            companyId, p.date(RiReportSupport.FROM), p.date(RiReportSupport.TO));
    Map<Long, FacPlacement> placements = support.placementsOf(list);
    Map<Long, String> branches = support.branchCodes(companyId);
    String type = p.optionalText(TREATY_TYPE).orElse(RiReportSupport.ALL);
    String status = p.optionalText(RiReportSupport.STATUS).orElse(RiReportSupport.ALL);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Cession c : list) {
      if (!RiReportSupport.inRange(p, c.getBranchId(), c.getBusinessLine())) {
        continue;
      }
      for (List<CessionLine> risk : byRisk(c).values()) {
        boolean provisional = hasProvisionalFac(risk, placements);
        if (matches(type, risk) && matchesStatus(status, provisional)) {
          rows.add(row(rows.size() + 1, c, risk, branches));
        }
      }
    }
    return TabularReportBuilder.of(p)
        .columns(columns())
        .groupBy(K_UW_YEAR, "UW Year")
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .rows(rows)
        .note("Premium amounts in base currency; % columns are shares of the company sum insured.")
        .note("PML = sum insured when no probable maximum loss is recorded (FinVerse rule).")
        .build();
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.text("serial", "S.No"),
        ReportColumn.text("policyNo", "Policy No"),
        ReportColumn.text("endtNo", "Endt No"),
        ReportColumn.text("product", "Product"),
        ReportColumn.text("risk", "Risk"),
        ReportColumn.amount("si", "SI (100%)"),
        ReportColumn.amount("ourSi", "Our SI"),
        ReportColumn.amount("pml", "PML"),
        ReportColumn.amount("ourPml", "Our PML"),
        ReportColumn.amount("gross", "Gross (100%)"),
        ReportColumn.amount("net", "Net (Our)"),
        ReportColumn.percent("sharePct", "Our Share %"),
        ReportColumn.amount("facSi", "FAC SI"),
        ReportColumn.amount("facPremium", "FAC Premium"),
        ReportColumn.amount(RETENTION, "Retention"),
        ReportColumn.percent("retentionPct", "Retention %"),
        ReportColumn.amount(QUOTA_SHARE, "QS"),
        ReportColumn.percent("qsPct", "QS %"),
        ReportColumn.amount(SURPLUS, "Surplus"),
        ReportColumn.percent("surplusPct", "Surplus %"),
        ReportColumn.text("cessionNo", "Cession No"));
  }

  private static Map<Long, List<CessionLine>> byRisk(Cession c) {
    Map<Long, List<CessionLine>> out = new LinkedHashMap<>();
    c.getLines().forEach(l -> out.computeIfAbsent(l.getRiskId(), k -> new ArrayList<>()).add(l));
    return out;
  }

  private static boolean hasProvisionalFac(List<CessionLine> risk, Map<Long, FacPlacement> facs) {
    return risk.stream()
        .filter(l -> l.getLayer() == RiLayer.FAC && l.getPartyId() == null)
        .map(l -> facs.get(l.getPlacementId()))
        .anyMatch(f -> f == null || !f.getStatus().isPlaced());
  }

  private static boolean matches(String type, List<CessionLine> risk) {
    return RiReportSupport.ALL.equals(type)
        || risk.stream()
            .anyMatch(l -> l.getLayer().name().equals(type) && l.getPremium().signum() != 0);
  }

  private static boolean matchesStatus(String status, boolean provisional) {
    return switch (status) {
      case RiReportSupport.ALLOCATED -> !provisional;
      case RiReportSupport.PROVISIONAL -> provisional;
      default -> true;
    };
  }

  private static Map<String, Object> row(
      int serial, Cession c, List<CessionLine> risk, Map<Long, String> branches) {
    CessionLine first = risk.get(0);
    BigDecimal ourSi = first.getRiskSi();
    Map<String, Object> m = new LinkedHashMap<>();
    RiReportSupport.putGroups(
        m, c.getTreatyYear(), branches.get(c.getBranchId()), c.getBusinessLine());
    m.put("serial", String.valueOf(serial));
    m.put("policyNo", c.getPolicyNo());
    m.put("endtNo", c.getEndorsementNo() == 0 ? "" : String.valueOf(c.getEndorsementNo()));
    m.put("product", c.getProductCode());
    m.put("risk", first.getRiskLineNo() + " " + first.getRiskDescription());
    BigDecimal si = c.toBase(RiReportSupport.full(ourSi, c.getSharePct()));
    m.put("si", si);
    m.put("ourSi", c.toBase(ourSi));
    m.put("pml", si);
    m.put("ourPml", c.toBase(ourSi));
    m.put("gross", c.toBase(RiReportSupport.full(first.getRiskPremium(), c.getSharePct())));
    m.put("net", c.toBase(first.getRiskPremium()));
    m.put("sharePct", c.getSharePct());
    m.put("facSi", c.toBase(sum(risk, RiLayer.FAC, true)));
    m.put("facPremium", c.toBase(sum(risk, RiLayer.FAC, false)));
    putLayer(m, RETENTION, "retentionPct", c, risk, RiLayer.RETENTION);
    putLayer(m, QUOTA_SHARE, "qsPct", c, risk, RiLayer.QUOTA_SHARE);
    putLayer(m, SURPLUS, "surplusPct", c, risk, RiLayer.SURPLUS);
    m.put("cessionNo", c.getCessionNo());
    return m;
  }

  private static void putLayer(
      Map<String, Object> m,
      String key,
      String pctKey,
      Cession c,
      List<CessionLine> risk,
      RiLayer layer) {
    m.put(key, c.toBase(sum(risk, layer, false)));
    m.put(pctKey, RiReportSupport.pct(sum(risk, layer, true), risk.get(0).getRiskSi()));
  }

  private static BigDecimal sum(List<CessionLine> risk, RiLayer layer, boolean sumInsured) {
    return risk.stream()
        .filter(l -> l.getLayer() == layer)
        .map(l -> sumInsured ? l.getSumInsured() : l.getPremium())
        .reduce(Money.zero(), BigDecimal::add);
  }
}
