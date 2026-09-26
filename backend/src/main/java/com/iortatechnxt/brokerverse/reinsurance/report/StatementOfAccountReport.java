package com.iortatechnxt.brokerverse.reinsurance.report;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.reinsurance.domain.Soa;
import com.iortatechnxt.brokerverse.reinsurance.domain.SoaFigures;
import com.iortatechnxt.brokerverse.reinsurance.domain.SoaPeriod;
import com.iortatechnxt.brokerverse.reinsurance.domain.SoaRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.Treaty;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.brokerverse.reinsurance.service.SoaCalculator;
import com.iortatechnxt.brokerverse.reinsurance.service.SoaLayout;
import com.iortatechnxt.brokerverse.reinsurance.service.TreatyService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * RI-SOA Statement of Account for Reinsurer: per treaty participant and quarter, the income and
 * outgo lines (premium ceded, commission, levy, losses paid, recoveries, premium reserve retained
 * and released, interest, outstanding loss reserve retained and released), the sub-totals, the
 * balance placed on the smaller side so that both columns total the same, and the balance in words.
 * Uses the stored statement when one was generated, else computes it on the fly.
 */
@Component
public class StatementOfAccountReport implements ReportDefinition {

  private static final String YEAR = "treatyYear";
  private static final String TREATY = "treatyCode";
  private static final String QUARTER = "quarter";
  private static final String DATE = "statementDate";
  private static final String REINSURER = "reinsurerCode";
  private static final String PARTICULARS = "particulars";
  private static final String INCOME = "income";
  private static final String OUTGO = "outgo";

  private final TreatyService treaties;
  private final SoaRepository statements;
  private final SoaCalculator calculator;
  private final CurrencyService currencies;

  /**
   * Creates the report.
   *
   * @param treaties treaties
   * @param statements stored statements
   * @param calculator statement lines
   * @param currencies currency names
   */
  public StatementOfAccountReport(
      TreatyService treaties,
      SoaRepository statements,
      SoaCalculator calculator,
      CurrencyService currencies) {
    this.treaties = treaties;
    this.statements = statements;
    this.calculator = calculator;
    this.currencies = currencies;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
            "RI-SOA",
            "Statement of Account for Reinsurer",
            ReportCategory.REINSURANCE,
            "Quarterly income / outgo statement per treaty and participant with balance in words",
            List.of(
                ParameterSpec.required(RiReportSupport.COMPANY, "Company", ParameterType.COMPANY),
                ParameterSpec.required(YEAR, "Treaty Year", ParameterType.NUMBER),
                ParameterSpec.required(TREATY, "Treaty Code", ParameterType.TEXT),
                ParameterSpec.select(QUARTER, "Quarter", List.of("1", "2", "3", "4"), "1"),
                ParameterSpec.required(DATE, "Statement Date", ParameterType.DATE)
                    .withDefault("TODAY"),
                ParameterSpec.optional(REINSURER, "Reinsurer Code", ParameterType.TEXT)),
            Permission.REINSURANCE_VIEW)
        .asDocument(); // a statement sent to the reinsurer: Word and PDF
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Treaty treaty = treaties.getByCode(p.longValue(RiReportSupport.COMPANY), p.text(TREATY));
    int year = new BigDecimal(p.text(YEAR)).intValue();
    if (treaty.getUwYear() != year) {
      throw new BusinessRuleException(
          "SOA_TREATY_YEAR",
          "Treaty " + treaty.getCode() + " belongs to year " + treaty.getUwYear());
    }
    SoaPeriod period = new SoaPeriod(year, Integer.parseInt(p.text(QUARTER)), p.date(DATE));
    String currencyName = currencies.requireActive(treaty.getCurrency()).getName();
    String reinsurer = p.optionalText(REINSURER).orElse(null);
    List<ReportRow> rows = new ArrayList<>();
    List<String> notes = new ArrayList<>();
    for (TreatyParticipant participant : treaty.getParticipants()) {
      String code = participant.getParty().getCode();
      if (reinsurer != null && !reinsurer.equals(code)) {
        continue;
      }
      SoaLayout layout = SoaLayout.of(figures(treaty, participant, period), currencyName);
      rows.add(
          ReportRow.section(
              code
                  + " "
                  + participant.getParty().getName()
                  + " - "
                  + treaty.getCode()
                  + " Q"
                  + period.quarter()
                  + " "
                  + year));
      rows.addAll(layoutRows(layout));
      notes.add(code + " - " + layout.balanceLabel() + ": " + layout.amountInWords());
    }
    notes.add(
        "Balance = income - outgo, placed on the smaller side. Amounts in "
            + treaty.getCurrency()
            + ".");
    return new ReportResult(
        "RI-SOA",
        "Statement of Account for Reinsurer",
        p.echo(),
        List.of(
            ReportColumn.text(PARTICULARS, "Particulars"),
            ReportColumn.amount(INCOME, "Income"),
            ReportColumn.amount(OUTGO, "Outgo")),
        rows,
        notes);
  }

  private SoaFigures figures(Treaty treaty, TreatyParticipant participant, SoaPeriod period) {
    return statements
        .findByTreatyIdAndPartyIdAndSoaYearAndQuarter(
            treaty.getId(), participant.getParty().getId(), period.year(), period.quarter())
        .map(Soa::figures)
        .orElseGet(() -> calculator.compute(treaty, participant, period));
  }

  private static Map<String, Object> cells(String label, BigDecimal income, BigDecimal outgo) {
    Map<String, Object> m = new LinkedHashMap<>();
    if (label != null) {
      m.put(PARTICULARS, label);
    }
    if (income != null) {
      m.put(INCOME, income);
    }
    if (outgo != null) {
      m.put(OUTGO, outgo);
    }
    return m;
  }

  private static List<ReportRow> layoutRows(SoaLayout layout) {
    List<ReportRow> rows = new ArrayList<>();
    layout
        .lines()
        .forEach(l -> rows.add(ReportRow.detail(cells(l.label(), l.income(), l.outgo()))));
    rows.add(
        new ReportRow(
            RowKind.SUBTOTAL,
            0,
            "Sub-total",
            cells(null, layout.incomeSubtotal(), layout.outgoSubtotal())));
    rows.add(
        ReportRow.detail(
            layout.balanceOnIncome()
                ? cells(layout.balanceLabel(), layout.balance(), null)
                : cells(layout.balanceLabel(), null, layout.balance())));
    rows.add(new ReportRow(RowKind.TOTAL, 0, "Total", cells(null, layout.total(), layout.total())));
    return rows;
  }
}
