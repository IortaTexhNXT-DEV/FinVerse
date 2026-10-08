package com.iortatechnxt.brokerverse.configpromo.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UploadCellsTest {

  @Test
  void splitsTheCellsOfSeveralValues() {
    assertThat(UploadCells.parts("50; Y", 2)).containsExactly("50", "Y");
    assertThat(UploadCells.parts("INS-MGIC; Panel; -; 1.2000; -", 5))
        .containsExactly("INS-MGIC", "Panel", null, "1.2000", null);
    assertThat(UploadCells.parts(null, 2)).containsExactly(null, null);
    assertThat(UploadCells.count("a;b;c")).isEqualTo(3);
  }

  @Test
  void readsPeriodsInTheFormatsOfTheWorkbook() {
    assertThat(UploadCells.period("01-Jan-2028"))
        .contains(new UploadCells.Period(LocalDate.of(2028, 1, 1), null));
    assertThat(UploadCells.period("01-Jan-2028; 31-Dec-2028"))
        .contains(new UploadCells.Period(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31)));
    assertThat(UploadCells.period("2027-12-01 00:00:00 / 2028-01-31"))
        .contains(new UploadCells.Period(LocalDate.of(2027, 12, 1), LocalDate.of(2028, 1, 31)));
    assertThat(UploadCells.period("31-Dec-2028; 01-Jan-2028")).isEmpty();
    assertThat(UploadCells.period("next year")).isEmpty();
    assertThat(UploadCells.period("01-Jan-2028; someday")).isEmpty();
  }

  @Test
  void readsNumbersAndYesNo() {
    assertThat(UploadCells.number("5,000.00")).isEqualByComparingTo(new BigDecimal("5000"));
    assertThat(UploadCells.number("-")).isNull();
    assertThat(UploadCells.yes("yes")).contains(true);
    assertThat(UploadCells.yes("N")).contains(false);
    assertThat(UploadCells.yes("maybe")).isEmpty();
  }

  @Test
  void writesCellsAsTheWorkbookShowsThem() {
    assertThat(UploadCells.join("BI", new BigDecimal("100000.00"), null))
        .isEqualTo("BI; 100000; -");
    assertThat(UploadCells.range(java.sql.Date.valueOf("2028-01-01"), null))
        .isEqualTo("1-Jan-2028");
    assertThat(UploadCells.join(true, false)).isEqualTo("Y; N");
  }
}
