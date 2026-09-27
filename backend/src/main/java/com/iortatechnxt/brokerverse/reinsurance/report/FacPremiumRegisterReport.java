package com.iortatechnxt.brokerverse.reinsurance.report;

import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.reinsurance.report.RiReportSupport.K_CLASS;

import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionLineRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacParticipant;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIR0692 FAC Premium Register: facultative premium ceded to each participant: placements approved
 * in the period (the placement's share of the requirement) and endorsements ceded to placed slips.
 * Branch &gt; Class &gt; Product.
 */
@Component
public class FacPremiumRegisterReport implements ReportDefinition {

  private static final String K_PRODUCT = "product";

  private final FacPlacementRepository placements;
  private final CessionLineRepository lines;
  private final RiReportSupport support;

  /**
   * Creates the report.
   *
   * @param placements placements
   * @param lines ceded facultative lines of endorsements
   * @param support shared support
   */
  public FacPremiumRegisterReport(
      FacPlacementRepository placements, CessionLineRepository lines, RiReportSupport support) {
    this.placements = placements;
    this.lines = lines;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = RiReportSupport.rangeParams();
    params.addAll(RiReportSupport.dateRange("FAC Approval Date From", "FAC Approval Date To"));
    return new ReportMetadata(
        "PGIR0692",
        "FAC Premium Register",
        ReportCategory.REINSURANCE,
        "Facultative premium and commission per placement and participant",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    LocalDate from = p.date(RiReportSupport.FROM);
    LocalDate to = p.date(RiReportSupport.TO);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    Map<Long, FacPlacement> placed = new LinkedHashMap<>();
    for (FacPlacement f :
        placements.findByCompanyIdAndStatusInOrderByIdDesc(
            companyId, EnumSet.of(FacStatus.PLACED, FacStatus.CLOSED))) {
      placed.put(f.getId(), f);
      Cession c = f.getCession();
      boolean inPeriod = !f.getPlacedOn().isBefore(from) && !f.getPlacedOn().isAfter(to);
      if (inPeriod && RiReportSupport.inRange(p, c.getBranchId(), c.getBusinessLine())) {
        for (FacParticipant fp : f.getParticipants()) {
          rows.add(
              row(
                  f,
                  c,
                  branches,
                  new Share(
                      fp.getParty().getCode(),
                      fp.getSharePct(),
                      fp.getSumInsured(),
                      fp.getPremium(),
                      fp.getCommissionPct(),
                      fp.getCommission())));
        }
      }
    }
    for (CessionLine l : lines.cededFac(companyId, from, to)) {
      FacPlacement f = placed.get(l.getPlacementId());
      Cession c = l.getCession();
      if (f != null && RiReportSupport.inRange(p, c.getBranchId(), c.getBusinessLine())) {
        rows.add(row(f, c, branches, endorsementShare(f, l)));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(columns())
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy(K_PRODUCT, "Product")
        .rows(rows)
        .note("Amounts in base currency. Endorsement rows are ceded to placed slips pro-rata.")
        .build();
  }

  private static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.text("policyNo", "Policy No"),
        ReportColumn.text("endtNo", "Endt No"),
        ReportColumn.date("issueDate", "Issue Date"),
        ReportColumn.text("placementNo", "Placement No"),
        ReportColumn.text("participant", "Participant"),
        ReportColumn.percent("placementPct", "Placement %"),
        ReportColumn.amountNoTotal("placementSi", "Placement SI"),
        ReportColumn.amountNoTotal("placementPremium", "Placement Premium"),
        ReportColumn.percent("participantPct", "Participant %"),
        ReportColumn.amount("participantSi", "Participant SI"),
        ReportColumn.amount("participantPremium", "Participant Premium"),
        ReportColumn.percent("commissionPct", "Commission %"),
        ReportColumn.amount("commission", "Commission"));
  }

  private static Share endorsementShare(FacPlacement f, CessionLine l) {
    BigDecimal pct =
        f.getParticipants().stream()
            .filter(fp -> fp.getParty().getId().equals(l.getPartyId()))
            .map(FacParticipant::getSharePct)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    return new Share(
        l.getPartyCode(),
        pct,
        l.getSumInsured(),
        l.getPremium(),
        l.getCommissionPct(),
        l.getCommission());
  }

  private static Map<String, Object> row(
      FacPlacement f, Cession c, Map<Long, String> branches, Share s) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(K_BRANCH, branches.get(c.getBranchId()));
    m.put(K_CLASS, c.getBusinessLine());
    m.put(K_PRODUCT, c.getProductCode());
    m.put("policyNo", c.getPolicyNo());
    m.put("endtNo", c.getEndorsementNo() == 0 ? "" : String.valueOf(c.getEndorsementNo()));
    m.put("issueDate", c.getIssueDate());
    m.put("placementNo", f.getPlacementNo());
    m.put("participant", s.party());
    m.put("placementPct", f.placementPct());
    m.put("placementSi", c.toBase(f.getPlacedSi()));
    m.put("placementPremium", c.toBase(f.getPlacedPremium()));
    m.put("participantPct", s.sharePct());
    m.put("participantSi", c.toBase(s.sumInsured()));
    m.put("participantPremium", c.toBase(s.premium()));
    m.put("commissionPct", s.commissionPct());
    m.put("commission", c.toBase(s.commission()));
    return m;
  }

  /** A participant's share on a row. */
  private record Share(
      String party,
      BigDecimal sharePct,
      BigDecimal sumInsured,
      BigDecimal premium,
      BigDecimal commissionPct,
      BigDecimal commission) {}
}
