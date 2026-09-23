package com.iortatechnxt.finverse.tax;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import com.iortatechnxt.finverse.tax.service.LedgerControl;
import com.iortatechnxt.finverse.tax.service.TaxDocumentLine;
import com.iortatechnxt.finverse.tax.service.TaxWorksheet;
import com.iortatechnxt.finverse.tax.service.TaxWorksheetService;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Worksheets built from real documents: a fire policy through a broker (VAT, DST, LGT, FST and
 * commission withholding) and a supplier invoice with input VAT and 2 % EWT, both in September
 * 2026. The shared test database may hold other documents of the month, so every assertion compares
 * the worksheet after the scenario with the worksheet before it.
 */
@IntegrationTest
class TaxWorksheetIT {

  private static final LocalDate DAY = LocalDate.of(2026, 9, 15);
  private static final TaxPeriod SEPTEMBER = TaxPeriod.month(YearMonth.of(2026, 9));

  @Autowired private TaxWorksheetService worksheets;
  @Autowired private TaxFixtures fixtures;

  @BeforeEach
  void masters() {
    fixtures.masters();
  }

  @Test
  void everyWorksheetPicksUpTheScenarioDocuments() {
    TaxWorksheet vatBefore = compute(WorksheetKind.VAT);
    TaxWorksheet ewtBefore = compute(WorksheetKind.EWT);
    TaxWorksheet dstBefore = compute(WorksheetKind.DST);
    TaxWorksheet lgtBefore = compute(WorksheetKind.LGT);
    TaxWorksheet fstBefore = compute(WorksheetKind.FST);
    TaxWorksheet ptBefore = compute(WorksheetKind.PREMIUM_TAX);

    Policy policy = fixtures.firePolicy(DAY, "100000");
    SupplierInvoice invoice = fixtures.supplierInvoice(DAY, "10000");
    PremiumBreakdown p = policy.getPremium();

    TaxWorksheet vat = compute(WorksheetKind.VAT);
    assertThat(delta(vat, vatBefore, "OUTPUT_VAT")).isEqualByComparingTo(p.getVat());
    assertThat(delta(vat, vatBefore, "SALES_TAXABLE")).isEqualByComparingTo(p.getOurNetPremium());
    assertThat(delta(vat, vatBefore, "INPUT_VAT")).isEqualByComparingTo("1200.00");
    assertThat(delta(vat, vatBefore, "PURCHASES_SERVICES")).isEqualByComparingTo("10000.00");
    assertThat(p.getVat()).isEqualByComparingTo("12000.00");
    TaxDocumentLine sale = document(vat, policy.getPolicyNo());
    assertThat(sale.taxCode()).isEqualTo("VATABLE");
    assertThat(sale.tin()).isEqualTo("301-222-333-000");
    assertThat(document(vat, invoice.getDocumentNo()).taxCode()).isEqualTo("SERVICES");
    assertControlsMoveWithDocuments(vat, vatBefore);

    TaxWorksheet ewt = compute(WorksheetKind.EWT);
    BigDecimal withheld = new BigDecimal("200.00").add(p.getWithholdingTax());
    assertThat(delta(ewt, ewtBefore, "TOTAL_WITHHELD")).isEqualByComparingTo(withheld);
    assertThat(delta(ewt, ewtBefore, "EWT_PAYABLE")).isEqualByComparingTo(withheld);
    TaxDocumentLine supplier = document(ewt, invoice.getDocumentNo());
    assertThat(supplier.taxCode()).isEqualTo("WC160");
    assertThat(supplier.taxableAmount()).isEqualByComparingTo("10000.00");
    assertThat(supplier.rate()).isEqualByComparingTo("2.00");
    TaxDocumentLine commission = document(ewt, policy.getPolicyNo());
    assertThat(commission.taxCode()).isEqualTo("WC515");
    assertThat(commission.sourceType()).isEqualTo("COMMISSION");
    assertThat(commission.taxAmount()).isEqualByComparingTo(p.getWithholdingTax());
    assertControlsMoveWithDocuments(ewt, ewtBefore);

    assertThat(delta(compute(WorksheetKind.DST), dstBefore, "TAX_DUE"))
        .isEqualByComparingTo(p.getDst());
    assertThat(delta(compute(WorksheetKind.LGT), lgtBefore, "TAX_DUE"))
        .isEqualByComparingTo(p.getLgt());
    TaxWorksheet fst = compute(WorksheetKind.FST);
    assertThat(delta(fst, fstBefore, "TAX_DUE")).isEqualByComparingTo(p.getFst());
    assertThat(delta(fst, fstBefore, "LOB:FIRE")).isEqualByComparingTo(p.getFst());
    assertControlsMoveWithDocuments(fst, fstBefore);
    assertThat(delta(compute(WorksheetKind.PREMIUM_TAX), ptBefore, "TAX_DUE"))
        .isEqualByComparingTo("0");
    assertThat(p.getDst()).isEqualByComparingTo("12500.00");
    assertThat(p.getFst()).isEqualByComparingTo("2000.00");
  }

  @Test
  void quarterWorksheetsSummariseEveryMonth() {
    TaxWorksheet quarter =
        worksheets.compute(fixtures.companyId(), WorksheetKind.EWT, TaxPeriod.quarter(2026, 3));
    assertThat(quarter.lines()).extracting(ReturnLineValues::code).contains("REMITTED_MONTHLY");
    assertThat(quarter.figures().taxDue())
        .isEqualByComparingTo(sum(quarter, TaxDocumentLine::taxAmount));
  }

  private TaxWorksheet compute(WorksheetKind kind) {
    return worksheets.compute(fixtures.companyId(), kind, SEPTEMBER);
  }

  private static BigDecimal delta(TaxWorksheet after, TaxWorksheet before, String code) {
    return line(after, code).subtract(line(before, code));
  }

  private static BigDecimal line(TaxWorksheet w, String code) {
    return w.lines().stream()
        .filter(l -> l.code().equals(code))
        .map(ReturnLineValues::amount)
        .findFirst()
        .orElse(BigDecimal.ZERO);
  }

  private static TaxDocumentLine document(TaxWorksheet w, String documentNo) {
    return w.documents().stream()
        .filter(d -> d.documentNo().equals(documentNo))
        .findFirst()
        .orElseThrow();
  }

  private static BigDecimal sum(TaxWorksheet w, Function<TaxDocumentLine, BigDecimal> field) {
    return w.documents().stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** The ledger moved exactly as much as the documents: the reconciliation difference holds. */
  private static void assertControlsMoveWithDocuments(TaxWorksheet after, TaxWorksheet before) {
    assertThat(after.controls()).hasSameSizeAs(before.controls()).isNotEmpty();
    for (int i = 0; i < after.controls().size(); i++) {
      LedgerControl a = after.controls().get(i);
      LedgerControl b = before.controls().get(i);
      assertThat(a.difference()).as(a.description()).isEqualByComparingTo(b.difference());
    }
  }
}
