package com.iortatechnxt.brokerverse.closing.report;

import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationLine;
import com.iortatechnxt.brokerverse.closing.domain.RevaluationItem;
import com.iortatechnxt.brokerverse.closing.service.FxRevaluationService;
import com.iortatechnxt.brokerverse.closing.service.FxRevaluationService.Register;
import com.iortatechnxt.brokerverse.closing.service.OpenItemRevaluation.OpenItemLine;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * GL-FXREV FX Revaluation Register: foreign currency GL balances with booked and revalued base
 * amounts and the unrealized difference (posted run of the period, or a preview), followed by the
 * revaluation of foreign currency open items (information only).
 */
@Component
public class FxRevaluationRegisterReport implements ReportDefinition {

  private static final String SECTION = "section";
  private static final String GL_SECTION = "1 GL balances";
  private static final String OPEN_ITEM_SECTION = "2 Open items (information only)";

  private final FxRevaluationService revaluations;
  private final OrganizationService organization;

  /**
   * Creates the report.
   *
   * @param revaluations revaluation service
   * @param organization organization service
   */
  public FxRevaluationRegisterReport(
      FxRevaluationService revaluations, OrganizationService organization) {
    this.revaluations = revaluations;
    this.organization = organization;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "GL-FXREV",
        "FX Revaluation Register",
        ReportCategory.GENERAL_LEDGER,
        "Foreign currency balances revalued at the closing rate with unrealized gain or loss",
        List.of(GlReportSupport.companyParam(), GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Register register =
        revaluations.register(p.longValue(GlReportSupport.COMPANY), p.date(GlReportSupport.AS_OF));
    List<Map<String, Object>> rows = rows(register);
    String basis = basis(register);
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("who", "Branch / Party"),
            ReportColumn.text("what", "Account / Document"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amountNoTotal("fc", "FC Amount"),
            ReportColumn.amount("booked", "Booked Base"),
            ReportColumn.amountNoTotal("rate", "Closing Rate"),
            ReportColumn.amount("revalued", "Revalued Base"),
            ReportColumn.amount("difference", "Unrealized Gain / (Loss)"))
        .groupBy(SECTION, "Section")
        .presorted()
        .withoutGrandTotal()
        .rows(rows)
        .note(basis)
        .note("Difference = FC amount x closing rate - booked base amount.")
        .build();
  }

  private List<Map<String, Object>> rows(Register register) {
    Map<Long, String> branches = new HashMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    if (register.run() != null) {
      for (FxRevaluationLine l : register.run().getLines()) {
        rows.add(
            row(
                GL_SECTION,
                branchCode(branches, l.getBranchId()),
                l.getAccountCode(),
                l.getCurrency(),
                new Figures(
                    l.getFcBalance(),
                    l.getBookedBase(),
                    l.getClosingRate(),
                    l.getRevaluedBase(),
                    l.getDifference())));
      }
    }
    for (RevaluationItem i : register.preview()) {
      rows.add(
          row(
              GL_SECTION,
              branchCode(branches, i.branchId()),
              i.accountCode(),
              i.currency(),
              new Figures(
                  i.fcBalance(),
                  i.bookedBase(),
                  i.closingRate(),
                  i.revaluedBase(),
                  i.difference())));
    }
    for (OpenItemLine o : register.openItems()) {
      rows.add(
          row(
              OPEN_ITEM_SECTION,
              o.partyCode(),
              o.documentNo() + " (" + o.direction() + ")",
              o.currency(),
              new Figures(
                  o.outstanding(),
                  o.bookedBase(),
                  o.closingRate(),
                  o.revaluedBase(),
                  o.gainLoss())));
    }
    return rows;
  }

  private static String basis(Register register) {
    return register.run() == null
        ? "Preview as of " + register.date() + ": the period has not been revalued yet."
        : "Posted run "
            + register.run().getPeriodName()
            + ", journal "
            + register.run().getJournalBatchNo()
            + (register.run().getReversalBatchNo() == null
                ? ""
                : ", reversed by " + register.run().getReversalBatchNo());
  }

  private String branchCode(Map<Long, String> cache, Long branchId) {
    return cache.computeIfAbsent(branchId, id -> organization.getBranch(id).getCode());
  }

  private static Map<String, Object> row(
      String section, String who, String what, String currency, Figures f) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(SECTION, section);
    row.put("who", who);
    row.put("what", what);
    row.put("currency", currency);
    row.put("fc", f.fc());
    row.put("booked", f.booked());
    row.put("rate", f.rate());
    row.put("revalued", f.revalued());
    row.put("difference", f.difference());
    return row;
  }

  /** Amount columns of one register row. */
  private record Figures(
      BigDecimal fc,
      BigDecimal booked,
      BigDecimal rate,
      BigDecimal revalued,
      BigDecimal difference) {}
}
