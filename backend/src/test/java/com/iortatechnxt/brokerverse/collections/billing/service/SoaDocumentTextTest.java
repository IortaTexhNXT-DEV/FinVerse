package com.iortatechnxt.brokerverse.collections.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The statement of account shows its dates as the screens do (17-Oct-2026), never 2026-10-17. */
class SoaDocumentTextTest {

  private static final String ISO_DATE = ".*\\d{4}-\\d{2}-\\d{2}.*";

  private static BillingStatement statement() {
    BillingStatementLine line = mock(BillingStatementLine.class);
    when(line.getInvoiceNo()).thenReturn("BI-HO-2026-000007");
    when(line.getPolicyYear()).thenReturn(1);
    when(line.getKind()).thenReturn(BillingStatementLine.LineKind.CURRENT);
    when(line.getCoverageFrom()).thenReturn(LocalDate.of(2026, 10, 17));
    when(line.getCoverageTo()).thenReturn(LocalDate.of(2027, 1, 16));
    when(line.getDueDate()).thenReturn(LocalDate.of(2026, 10, 17));
    when(line.getAmount()).thenReturn(new BigDecimal("5567.18"));
    when(line.getPaid()).thenReturn(BigDecimal.ZERO);
    when(line.getBalance()).thenReturn(new BigDecimal("5567.18"));
    BillingStatement soa = mock(BillingStatement.class);
    when(soa.getArn()).thenReturn("ARN-2026-940007");
    when(soa.getCycleFrom()).thenReturn(LocalDate.of(2026, 10, 17));
    when(soa.getCycleTo()).thenReturn(LocalDate.of(2027, 1, 16));
    when(soa.getDueDate()).thenReturn(LocalDate.of(2026, 10, 17));
    when(soa.getFrequency()).thenReturn("QUARTERLY");
    when(soa.getCurrency()).thenReturn("PHP");
    when(soa.getTotal()).thenReturn(new BigDecimal("5567.18"));
    when(soa.getPaid()).thenReturn(BigDecimal.ZERO);
    when(soa.getBalance()).thenReturn(new BigDecimal("5567.18"));
    when(soa.getLines()).thenReturn(List.of(line));
    return soa;
  }

  @Test
  void templateTextGetsTheDatesAsOnTheScreens() {
    var values = SoaDocument.mergeValues(statement());
    assertThat(values)
        .containsEntry("cycleFrom", "17-Oct-2026")
        .containsEntry("cycleTo", "16-Jan-2027")
        .containsEntry("dueDate", "17-Oct-2026")
        .containsEntry("balance", "5,567.18");
    assertThat(values.values()).allSatisfy(v -> assertThat(v).doesNotMatch(ISO_DATE));
  }

  @Test
  void installmentRowsShowCoverageAndDueDateAsOnTheScreens() {
    List<List<String>> rows = SoaDocument.rows(statement());
    assertThat(rows.get(0)).contains("17-Oct-2026 to 16-Jan-2027", "17-Oct-2026");
    assertThat(rows)
        .allSatisfy(r -> assertThat(r).allSatisfy(c -> assertThat(c).doesNotMatch(ISO_DATE)));
    assertThat(SoaDocument.period(LocalDate.of(2026, 10, 17), LocalDate.of(2027, 1, 16)))
        .isEqualTo("17-Oct-2026 to 16-Jan-2027");
  }
}
