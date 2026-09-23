package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.tax.domain.NormalBalance;
import com.iortatechnxt.finverse.tax.domain.ReturnFigures;
import com.iortatechnxt.finverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.finverse.tax.domain.ReturnStatus;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.TaxReturn;
import com.iortatechnxt.finverse.tax.domain.TaxReturnRepository;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import com.iortatechnxt.finverse.tax.domain.Taxpayer;
import com.iortatechnxt.finverse.tax.domain.VatTreatment;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import com.iortatechnxt.finverse.tax.service.TaxSourceQueries.InvoiceTaxRow;
import com.iortatechnxt.finverse.tax.service.TaxpayerDirectory.Lookup;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * VAT worksheet (BIR Form 2550Q) with the documents of the Summary Lists of Sales and Purchases.
 *
 * <ul>
 *   <li>Sales: approved premium transactions. With VAT charged the company's net premium is a
 *       VATable sale; without VAT it is zero-rated when the customer's tax profile says so,
 *       otherwise exempt (e.g. business subject to premium tax). Policy fees are not VATable here
 *       because the premium calculator does not charge VAT on them (assumption).
 *   <li>Purchases: approved supplier invoices by invoice date. With VAT the net is a purchase of
 *       capital goods when a line is booked to an asset account, else a purchase of services;
 *       without VAT it is zero-rated when the supplier's profile says so, otherwise exempt. All
 *       input VAT is treated as creditable (no apportionment to exempt sales — assumption).
 *   <li>Credits = input VAT + excess input VAT carried over from the previous quarter's FILED or
 *       PAID 2550Q. VAT payable = output VAT − credits; a negative result is carried over.
 * </ul>
 */
@Component
public class VatWorksheetBuilder {

  private static final String VATABLE = "VATABLE";
  private static final String ZERO_RATED = "ZERO_RATED";
  private static final String EXEMPT = "EXEMPT";
  private static final String SERVICES = "SERVICES";
  private static final String CAPITAL_GOODS = "CAPITAL_GOODS";
  private static final String SUPPLIER_INVOICE = "SUPPLIER_INVOICE";
  private static final int RATE_SCALE = 8;

  private final PremiumTaxSource premiums;
  private final TaxSourceQueries queries;
  private final TaxpayerDirectory directory;
  private final TaxReturnRepository returns;
  private final WorksheetSupport support;

  /**
   * Creates the builder.
   *
   * @param premiums premium documents
   * @param queries supplier invoices
   * @param directory party tax facts
   * @param returns previous returns (carry-over)
   * @param support shared helpers
   */
  public VatWorksheetBuilder(
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
   * @param period period (normally a quarter)
   * @return worksheet
   */
  public TaxWorksheet build(Long companyId, TaxPeriod period) {
    List<PremiumDocument> sales = premiums.documents(companyId, period);
    List<InvoiceTaxRow> invoices = queries.supplierInvoices(companyId, period.from(), period.to());
    Set<String> parties = new TreeSet<>();
    sales.forEach(s -> parties.add(s.customerCode()));
    invoices.forEach(i -> parties.add(i.partyCode()));
    Lookup lookup = directory.lookup(companyId, parties);
    List<TaxDocumentLine> documents = new ArrayList<>();
    sales.forEach(s -> documents.add(sale(s, lookup)));
    invoices.forEach(i -> documents.add(purchase(i, lookup)));

    List<TaxDocumentLine> salesLines = documents.stream().filter(d -> isSection(d, true)).toList();
    List<TaxDocumentLine> buyLines = documents.stream().filter(d -> isSection(d, false)).toList();
    BigDecimal taxable = WorksheetSupport.sum(salesLines, TaxDocumentLine::taxableAmount);
    BigDecimal outputVat = WorksheetSupport.sum(salesLines, TaxDocumentLine::taxAmount);
    BigDecimal inputVat = WorksheetSupport.sum(buyLines, TaxDocumentLine::taxAmount);
    List<String> notes = new ArrayList<>();
    BigDecimal carryOver = carryOver(companyId, period, notes);
    ReturnFigures figures = ReturnFigures.of(taxable, outputVat, inputVat.add(carryOver));

    List<ReturnLineValues> lines = new ArrayList<>();
    lines.add(line("SALES_TAXABLE", "VATable sales (premiums)", null, taxable));
    lines.add(line("SALES_ZERO_RATED", "Zero-rated sales", null, zeroRated(salesLines)));
    lines.add(line("SALES_EXEMPT", "Exempt sales", null, exempt(salesLines)));
    lines.add(line("OUTPUT_VAT", "Output VAT", taxable, outputVat));
    lines.add(
        line(
            "PURCHASES_SERVICES", "Domestic purchases of services", null, net(buyLines, SERVICES)));
    lines.add(
        line(
            "PURCHASES_CAPITAL", "Purchases of capital goods", null, net(buyLines, CAPITAL_GOODS)));
    lines.add(line("PURCHASES_ZERO_RATED", "Zero-rated purchases", null, zeroRated(buyLines)));
    lines.add(line("PURCHASES_EXEMPT", "Purchases not subject to VAT", null, exempt(buyLines)));
    lines.add(line("INPUT_VAT", "Input VAT on purchases", null, inputVat));
    lines.add(
        line("CARRY_OVER", "Excess input VAT carried over from previous quarter", null, carryOver));
    lines.add(line("TOTAL_CREDITS", "Total allowable input VAT", null, figures.taxCredits()));
    lines.add(line("VAT_PAYABLE", "VAT payable", null, figures.amountPayable()));
    lines.add(
        line(
            "EXCESS_CREDIT",
            "Excess input VAT carried over to next quarter",
            null,
            figures.excessCredit()));

    List<LedgerControl> controls = new ArrayList<>();
    support
        .control(
            companyId, TaxType.VAT_OUTPUT, "Output VAT", outputVat, period, NormalBalance.CREDIT)
        .ifPresent(controls::add);
    support
        .control(companyId, TaxType.VAT_INPUT, "Input VAT", inputVat, period, NormalBalance.DEBIT)
        .ifPresent(controls::add);
    return new TaxWorksheet(WorksheetKind.VAT, period, lines, figures, documents, controls, notes);
  }

  private BigDecimal carryOver(Long companyId, TaxPeriod period, List<String> notes) {
    Optional<TaxReturn> previous =
        returns.findFirstByCompanyIdAndWorksheetAndPeriodEndAndStatusNot(
            companyId, WorksheetKind.VAT, period.from().minusDays(1), ReturnStatus.CANCELLED);
    if (previous.isEmpty()) {
      return Money.zero();
    }
    TaxReturn r = previous.get();
    if (r.getStatus() == ReturnStatus.DRAFT) {
      notes.add(
          "Previous VAT return "
              + r.getReturnNo()
              + " is still a draft: its excess input VAT is not carried over until it is filed.");
      return Money.zero();
    }
    return r.getExcessCredit();
  }

  private static TaxDocumentLine sale(PremiumDocument s, Lookup lookup) {
    Taxpayer t = lookup.taxpayer(s.customerCode(), s.customerName());
    String vatClass = classify(s.vat(), lookup.vatTreatment(s.customerCode()), VATABLE);
    return new TaxDocumentLine(
        TaxDocumentLine.SALES,
        s.sourceType(),
        s.policyId(),
        s.documentNo(),
        s.date(),
        s.customerCode(),
        t.name(),
        t.formattedTin(),
        vatClass,
        null,
        s.businessLine(),
        VATABLE.equals(vatClass) ? s.premium() : Money.zero(),
        EXEMPT.equals(vatClass) ? s.premium() : Money.zero(),
        ZERO_RATED.equals(vatClass) ? s.premium() : Money.zero(),
        s.vat(),
        ReturnFigures.effectiveRate(s.premium(), s.vat()));
  }

  private static TaxDocumentLine purchase(InvoiceTaxRow i, Lookup lookup) {
    Taxpayer t = lookup.taxpayer(i.partyCode(), i.partyName());
    BigDecimal rate = i.basePayable().divide(i.payable(), RATE_SCALE, RoundingMode.HALF_EVEN);
    BigDecimal net = Money.convert(i.net(), rate);
    BigDecimal vat = Money.convert(i.vat(), rate);
    String vatClass =
        classify(
            i.vat(),
            lookup.vatTreatment(i.partyCode()),
            i.capitalGoods() ? CAPITAL_GOODS : SERVICES);
    boolean taxable = !EXEMPT.equals(vatClass) && !ZERO_RATED.equals(vatClass);
    return new TaxDocumentLine(
        TaxDocumentLine.PURCHASES,
        SUPPLIER_INVOICE,
        i.id(),
        i.documentNo(),
        i.invoiceDate(),
        i.partyCode(),
        t.name(),
        t.formattedTin(),
        vatClass,
        null,
        null,
        taxable ? net : Money.zero(),
        EXEMPT.equals(vatClass) ? net : Money.zero(),
        ZERO_RATED.equals(vatClass) ? net : Money.zero(),
        vat,
        ReturnFigures.effectiveRate(net, vat));
  }

  private static String classify(BigDecimal vat, VatTreatment treatment, String taxableClass) {
    if (vat.signum() != 0) {
      return taxableClass;
    }
    return treatment == VatTreatment.ZERO_RATED ? ZERO_RATED : EXEMPT;
  }

  private static boolean isSection(TaxDocumentLine d, boolean sales) {
    return d.section().equals(sales ? TaxDocumentLine.SALES : TaxDocumentLine.PURCHASES);
  }

  private static BigDecimal net(List<TaxDocumentLine> lines, String vatClass) {
    return WorksheetSupport.sum(
        lines.stream().filter(l -> vatClass.equals(l.taxCode())).toList(),
        TaxDocumentLine::taxableAmount);
  }

  private static BigDecimal zeroRated(List<TaxDocumentLine> lines) {
    return WorksheetSupport.sum(lines, TaxDocumentLine::zeroRatedAmount);
  }

  private static BigDecimal exempt(List<TaxDocumentLine> lines) {
    return WorksheetSupport.sum(lines, TaxDocumentLine::exemptAmount);
  }

  private static ReturnLineValues line(
      String code, String description, BigDecimal base, BigDecimal amount) {
    return new ReturnLineValues(code, description, base, amount);
  }
}
