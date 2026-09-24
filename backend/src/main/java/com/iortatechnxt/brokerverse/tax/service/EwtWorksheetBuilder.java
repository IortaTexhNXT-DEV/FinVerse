package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.tax.domain.NormalBalance;
import com.iortatechnxt.brokerverse.tax.domain.ReturnFigures;
import com.iortatechnxt.brokerverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.brokerverse.tax.domain.ReturnStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturn;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturnRepository;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import com.iortatechnxt.brokerverse.tax.domain.Taxpayer;
import com.iortatechnxt.brokerverse.tax.domain.WithholdingEntry;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxSourceQueries.InvoiceTaxRow;
import com.iortatechnxt.brokerverse.tax.service.TaxpayerDirectory.AtcFacts;
import com.iortatechnxt.brokerverse.tax.service.TaxpayerDirectory.Lookup;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Expanded (creditable) withholding tax worksheet: 0619-E for a month, 1601-EQ for a quarter.
 *
 * <ul>
 *   <li>Income payments: approved supplier invoices with EWT (net amount, by invoice date) and
 *       commissions of approved policies and endorsements with withholding tax (by approval date).
 *       The liability to withhold arises when the income is payable (accrual), not when it is paid
 *       — the conservative BIR rule "whichever comes first" (assumption).
 *   <li>ATC: the payee's default ATC on the party tax profile; payees without one appear under
 *       "UNMAPPED". The tax withheld is the amount actually withheld on the document; when it
 *       differs from the ATC rate the worksheet lists a note so the rate can be corrected.
 *   <li>Quarter: tax still due = total withheld − remittances of the monthly EWT returns (0619-E)
 *       FILED or PAID for months of the quarter.
 * </ul>
 */
@Component
public class EwtWorksheetBuilder {

  private static final String SUPPLIER_INVOICE = "SUPPLIER_INVOICE";
  private static final String COMMISSION = "COMMISSION";
  private static final String ATC_PREFIX = "ATC:";
  private static final int RATE_SCALE = 8;
  private static final BigDecimal RATE_TOLERANCE = new BigDecimal("0.05");

  private final PremiumTaxSource premiums;
  private final TaxSourceQueries queries;
  private final TaxpayerDirectory directory;
  private final TaxReturnRepository returns;
  private final WorksheetSupport support;

  /**
   * Creates the builder.
   *
   * @param premiums premium documents (commissions)
   * @param queries supplier invoices
   * @param directory payee facts
   * @param returns monthly remittances of the quarter
   * @param support shared helpers
   */
  public EwtWorksheetBuilder(
      PremiumTaxSource premiums,
      TaxSourceQueries queries,
      TaxpayerDirectory directory,
      TaxReturnRepository returns,
      WorksheetSupport support) {
    this.premiums = premiums;
    this.queries = queries;
    this.directory = directory;
    this.returns = returns;
    this.support = support;
  }

  /**
   * Builds the worksheet.
   *
   * @param companyId company
   * @param period month or quarter
   * @return worksheet
   */
  public TaxWorksheet build(Long companyId, TaxPeriod period) {
    List<InvoiceTaxRow> invoices =
        queries.supplierInvoices(companyId, period.from(), period.to()).stream()
            .filter(i -> i.withholding().signum() != 0)
            .toList();
    List<PremiumDocument> commissions =
        premiums.documents(companyId, period).stream()
            .filter(d -> d.intermediaryCode() != null && d.withholding().signum() != 0)
            .toList();
    Set<String> parties = new TreeSet<>();
    invoices.forEach(i -> parties.add(i.partyCode()));
    commissions.forEach(c -> parties.add(c.intermediaryCode()));
    Lookup lookup = directory.lookup(companyId, parties);
    List<TaxDocumentLine> documents = new ArrayList<>();
    invoices.forEach(i -> documents.add(invoiceLine(i, lookup)));
    commissions.forEach(c -> documents.add(commissionLine(c, lookup)));

    BigDecimal base = WorksheetSupport.sum(documents, TaxDocumentLine::taxableAmount);
    BigDecimal withheld = WorksheetSupport.sum(documents, TaxDocumentLine::taxAmount);
    BigDecimal remitted = period.isQuarter() ? monthlyRemittances(companyId, period) : Money.zero();
    ReturnFigures figures = ReturnFigures.of(base, withheld, remitted);
    List<ReturnLineValues> lines = new ArrayList<>(atcLines(documents));
    lines.add(new ReturnLineValues("TOTAL_WITHHELD", "Total taxes withheld", base, withheld));
    if (period.isQuarter()) {
      lines.add(
          new ReturnLineValues(
              "REMITTED_MONTHLY",
              "Less: remitted with the monthly returns (0619-E) of the quarter",
              null,
              remitted));
    }
    lines.add(new ReturnLineValues("EWT_PAYABLE", "Tax still due", null, figures.amountPayable()));
    List<LedgerControl> controls =
        support
            .control(
                companyId,
                TaxType.EWT,
                "Expanded withholding tax payable",
                withheld,
                period,
                NormalBalance.CREDIT)
            .stream()
            .toList();
    return new TaxWorksheet(
        WorksheetKind.EWT, period, lines, figures, documents, controls, notes(documents, lookup));
  }

  /**
   * Income payments of a quarter as 2307 / QAP entries.
   *
   * @param worksheet EWT worksheet of the quarter
   * @return entries
   */
  public static List<WithholdingEntry> entries(TaxWorksheet worksheet) {
    return worksheet.section(TaxDocumentLine.WITHHOLDING).stream()
        .map(
            d ->
                new WithholdingEntry(
                    d.partyCode(),
                    d.taxCode(),
                    d.incomeNature(),
                    d.documentDate(),
                    d.taxableAmount(),
                    d.taxAmount()))
        .toList();
  }

  private BigDecimal monthlyRemittances(Long companyId, TaxPeriod quarter) {
    return returns
        .findInside(
            companyId,
            WorksheetKind.EWT,
            quarter.from(),
            quarter.to(),
            EnumSet.of(ReturnStatus.FILED, ReturnStatus.PAID))
        .stream()
        .filter(r -> !r.period().isQuarter())
        .map(TaxReturn::getAmountPayable)
        .reduce(Money.zero(), BigDecimal::add);
  }

  private static TaxDocumentLine invoiceLine(InvoiceTaxRow i, Lookup lookup) {
    BigDecimal rate = i.basePayable().divide(i.payable(), RATE_SCALE, RoundingMode.HALF_EVEN);
    return line(
        new Payment(
            SUPPLIER_INVOICE,
            i.id(),
            i.documentNo(),
            i.invoiceDate(),
            i.partyCode(),
            i.partyName(),
            Money.convert(i.net(), rate),
            Money.convert(i.withholding(), rate)),
        lookup);
  }

  private static TaxDocumentLine commissionLine(PremiumDocument c, Lookup lookup) {
    return line(
        new Payment(
            COMMISSION,
            c.policyId(),
            c.documentNo(),
            c.date(),
            c.intermediaryCode(),
            c.intermediaryName(),
            c.commission(),
            c.withholding()),
        lookup);
  }

  private static TaxDocumentLine line(Payment p, Lookup lookup) {
    Taxpayer t = lookup.taxpayer(p.partyCode(), p.partyName());
    AtcFacts atc = lookup.atc(p.partyCode());
    return new TaxDocumentLine(
        TaxDocumentLine.WITHHOLDING,
        p.sourceType(),
        p.sourceId(),
        p.documentNo(),
        p.date(),
        p.partyCode(),
        t.name(),
        t.formattedTin(),
        atc.atc(),
        atc.incomeNature(),
        null,
        p.income(),
        Money.zero(),
        Money.zero(),
        p.tax(),
        ReturnFigures.effectiveRate(p.income(), p.tax()));
  }

  private static List<ReturnLineValues> atcLines(List<TaxDocumentLine> documents) {
    Map<String, List<TaxDocumentLine>> byAtc = new TreeMap<>();
    documents.forEach(d -> byAtc.computeIfAbsent(d.taxCode(), k -> new ArrayList<>()).add(d));
    List<ReturnLineValues> lines = new ArrayList<>();
    byAtc.forEach(
        (atc, docs) ->
            lines.add(
                new ReturnLineValues(
                    ATC_PREFIX + atc,
                    atc + " - " + docs.get(0).incomeNature(),
                    WorksheetSupport.sum(docs, TaxDocumentLine::taxableAmount),
                    WorksheetSupport.sum(docs, TaxDocumentLine::taxAmount))));
    return lines;
  }

  private static List<String> notes(List<TaxDocumentLine> documents, Lookup lookup) {
    List<String> notes = new ArrayList<>();
    Set<String> unmapped = new TreeSet<>();
    Set<String> mismatched = new TreeSet<>();
    for (TaxDocumentLine d : documents) {
      AtcFacts atc = lookup.atc(d.partyCode());
      if (atc.rate() == null) {
        unmapped.add(d.partyCode());
      } else if (d.rate().subtract(atc.rate()).abs().compareTo(RATE_TOLERANCE) > 0) {
        mismatched.add(
            d.partyCode()
                + " ("
                + d.rate()
                + "% vs "
                + atc.atc()
                + " "
                + percent(atc.rate())
                + ")");
      }
    }
    if (!unmapped.isEmpty()) {
      notes.add("Payees without an authorized default ATC: " + String.join(", ", unmapped));
    }
    if (!mismatched.isEmpty()) {
      notes.add("Tax withheld at a rate different from the ATC: " + String.join(", ", mismatched));
    }
    return notes;
  }

  private static String percent(BigDecimal rate) {
    return rate.stripTrailingZeros().toPlainString() + "%";
  }

  /** An income payment before the payee facts are applied. */
  private record Payment(
      String sourceType,
      Long sourceId,
      String documentNo,
      LocalDate date,
      String partyCode,
      String partyName,
      BigDecimal income,
      BigDecimal tax) {}
}
