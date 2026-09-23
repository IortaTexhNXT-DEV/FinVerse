package com.iortatechnxt.finverse.reinsurance.report;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLineRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovementRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShare;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.service.TreatyService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * RI-BDX Treaty Bordereau: the risks ceded to a treaty in a period (premium bordereau: policy,
 * risk, sum insured, premium and commission ceded at 100 % of the treaty) or the claims recovered
 * from it (claims bordereau: claim, policy, loss date, amount recovered). Grouped by participant.
 */
@Component
public class TreatyBordereauReport implements ReportDefinition {

  private static final String TREATY = "treatyCode";
  private static final String TYPE = "bordereau";
  private static final String PARTICIPANT = "participant";
  private static final String POLICY = "policyNo";
  private static final String SHARE_PCT = "sharePct";

  private final TreatyService treaties;
  private final CessionLineRepository lines;
  private final RiClaimMovementRepository movements;

  /**
   * Creates the report.
   *
   * @param treaties treaties
   * @param lines cession lines
   * @param movements claim movements
   */
  public TreatyBordereauReport(
      TreatyService treaties, CessionLineRepository lines, RiClaimMovementRepository movements) {
    this.treaties = treaties;
    this.lines = lines;
    this.movements = movements;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(RiReportSupport.COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.required(TREATY, "Treaty Code", ParameterType.TEXT));
    params.add(ParameterSpec.select(TYPE, "Bordereau", List.of("PREMIUM", "CLAIMS"), "PREMIUM"));
    params.addAll(RiReportSupport.dateRange("Date From", "Date To"));
    return new ReportMetadata(
        "RI-BDX",
        "Treaty Bordereau (Premium / Claims)",
        ReportCategory.REINSURANCE,
        "Risks ceded or claims recovered under a treaty, per participant",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    Treaty treaty = treaties.getByCode(companyId, p.text(TREATY));
    LocalDate from = p.date(RiReportSupport.FROM);
    LocalDate to = p.date(RiReportSupport.TO);
    boolean claims = "CLAIMS".equals(p.optionalText(TYPE).orElse("PREMIUM"));
    TabularReportBuilder builder =
        claims
            ? TabularReportBuilder.of(p)
                .columns(claimColumns())
                .rows(claimRows(companyId, treaty, from, to))
            : TabularReportBuilder.of(p)
                .columns(premiumColumns())
                .rows(premiumRows(treaty, from, to));
    return builder
        .groupBy(PARTICIPANT, "Participant")
        .note("Treaty " + treaty.getCode() + " " + treaty.getName() + ", base currency.")
        .build();
  }

  private List<Map<String, Object>> premiumRows(Treaty treaty, LocalDate from, LocalDate to) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (CessionLine l : lines.ofTreaty(treaty.getId(), from, to)) {
      Cession c = l.getCession();
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(PARTICIPANT, l.getPartyCode());
      row.put("riDate", c.getRiDate());
      row.put("cessionNo", c.getCessionNo());
      row.put(POLICY, c.getPolicyNo());
      row.put("endtNo", c.getEndorsementNo() == 0 ? "" : String.valueOf(c.getEndorsementNo()));
      row.put("risk", l.getRiskLineNo() + " " + l.getRiskDescription());
      row.put("riskSi", c.toBase(l.getRiskSi()));
      row.put(SHARE_PCT, l.getSharePct());
      row.put("si", c.toBase(l.getSumInsured()));
      row.put("premium", l.getBasePremium());
      row.put("commission", l.getBaseCommission());
      row.put("net", l.getBasePremium().subtract(l.getBaseCommission()));
      rows.add(row);
    }
    return rows;
  }

  private List<Map<String, Object>> claimRows(
      Long companyId, Treaty treaty, LocalDate from, LocalDate to) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (RiClaimMovement m :
        movements.findByCompanyIdAndMovementDateBetweenOrderByMovementDateAscIdAsc(
            companyId, from, to)) {
      for (RiClaimShare s : m.getShares()) {
        if (treaty.getId().equals(s.getTreatyId())) {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put(PARTICIPANT, s.getPartyCode());
          row.put("date", m.getMovementDate());
          row.put("claimNo", m.getClaimNo());
          row.put("lossDate", m.getLossDate());
          row.put("type", m.getMovementType().name());
          row.put("layer", s.getLayerNo() == null ? s.getLayer().name() : "XOL L" + s.getLayerNo());
          row.put("amount", m.signedLoss());
          row.put(SHARE_PCT, s.getSharePct());
          row.put("share", Money.round(s.getBaseAmount()));
          rows.add(row);
        }
      }
    }
    return rows;
  }

  private static List<ReportColumn> premiumColumns() {
    return List.of(
        ReportColumn.date("riDate", "RI Date"),
        ReportColumn.text("cessionNo", "Cession No"),
        ReportColumn.text(POLICY, "Policy No"),
        ReportColumn.text("endtNo", "Endt No"),
        ReportColumn.text("risk", "Risk"),
        ReportColumn.amountNoTotal("riskSi", "Our Risk SI"),
        ReportColumn.percent(SHARE_PCT, "Share %"),
        ReportColumn.amount("si", "SI Ceded"),
        ReportColumn.amount("premium", "Premium Ceded"),
        ReportColumn.amount("commission", "Commission"),
        ReportColumn.amount("net", "Net Premium"));
  }

  private static List<ReportColumn> claimColumns() {
    return List.of(
        ReportColumn.date("date", "Date"),
        ReportColumn.text("claimNo", "Claim No"),
        ReportColumn.date("lossDate", "Loss Date"),
        ReportColumn.text("type", "Movement"),
        ReportColumn.text("layer", "Layer"),
        ReportColumn.amountNoTotal("amount", "Claim Amount"),
        ReportColumn.percent(SHARE_PCT, "Share %"),
        ReportColumn.amount("share", "Reinsurer Share"));
  }
}
