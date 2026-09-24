package com.iortatechnxt.brokerverse.claims.report;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.MovementKind;
import com.iortatechnxt.brokerverse.claims.domain.MovementLine;
import com.iortatechnxt.brokerverse.claims.service.ClaimQueryService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * CLM-MOVEMENT Claim Movement Statement: every approved movement of one claim in date order,
 * company share in the claim currency: estimate changes (Reports Book type), amounts paid and
 * recovered, and the running payment outstanding (estimate − paid).
 */
@Component
public class ClaimMovementStatementReport implements ReportDefinition {

  private static final String CLAIM_NO = "claimNo";
  private static final String ESTIMATE = "estimate";
  private static final String PAID = "paid";
  private static final String RECOVERED = "recovered";

  private final ClaimQueryService query;

  /**
   * Creates the report.
   *
   * @param query claim ledger queries
   */
  public ClaimMovementStatementReport(ClaimQueryService query) {
    this.query = query;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "CLM-MOVEMENT",
        "Claim Movement Statement",
        ReportCategory.CLAIMS,
        "Estimate, payment and recovery history of one claim",
        List.of(
            ParameterSpec.required(ClaimReportSupport.COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(CLAIM_NO, "Claim No", ParameterType.TEXT)),
        Permission.CLAIM_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String claimNo = p.text(CLAIM_NO);
    Claim claim =
        query
            .findByNumber(p.longValue(ClaimReportSupport.COMPANY), claimNo)
            .orElseThrow(
                () -> new BusinessRuleException("CLAIM_NOT_FOUND", "Unknown claim " + claimNo));
    BigDecimal outstanding = Money.zero();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (MovementLine l : query.movements(claim.getId())) {
      if (l.getSide() == EstimateSide.PAYMENT) {
        outstanding =
            l.getKind() == MovementKind.ESTIMATE
                ? outstanding.add(l.getAmount())
                : outstanding.subtract(l.getAmount());
      }
      rows.add(row(l, outstanding));
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.date("date", "Date"),
            ReportColumn.text("reference", "Reference"),
            ReportColumn.text("movement", "Movement"),
            ReportColumn.text("type", "Type"),
            ReportColumn.text("cost", "Cost"),
            ReportColumn.amount(ESTIMATE, "Estimate Change"),
            ReportColumn.amount(PAID, "Paid"),
            ReportColumn.amount(RECOVERED, "Recovered"),
            ReportColumn.amountNoTotal("outstanding", "Payment O/S"),
            ReportColumn.text("journal", "Journal"))
        .rows(rows)
        .presorted()
        .note(
            "Claim "
                + claim.getClaimNo()
                + " - policy "
                + claim.getPolicy().getPolicyNo()
                + " - "
                + claim.getPolicy().getInsuredName()
                + " - loss "
                + claim.getLoss().getLossDate()
                + " - status "
                + claim.getStatus())
        .note(
            "Company share in "
                + claim.getCurrency()
                + "; types: 1 Payment, 2 Recovery, 3 / 4 reversals.")
        .build();
  }

  private static Map<String, Object> row(MovementLine l, BigDecimal outstanding) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("date", l.getMovementDate());
    m.put("reference", l.getReference());
    m.put("movement", l.getKind() + " " + l.getSide());
    m.put("type", String.valueOf(l.getEstimateType()));
    m.put("cost", l.getCostType().name());
    m.put(ESTIMATE, Money.zero());
    m.put(PAID, Money.zero());
    m.put(RECOVERED, Money.zero());
    m.put(column(l), l.getAmount());
    m.put("outstanding", outstanding);
    m.put("journal", l.getJournalBatchNo());
    return m;
  }

  private static String column(MovementLine l) {
    if (l.getKind() == MovementKind.ESTIMATE) {
      return ESTIMATE;
    }
    return l.getSide() == EstimateSide.PAYMENT ? PAID : RECOVERED;
  }
}
