package com.iortatechnxt.brokerverse.prodrecon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.Frequency;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem.InsurerRow;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSchedule;
import com.iortatechnxt.brokerverse.prodrecon.service.InsurerRowParser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Reading insurer production rows (PRCID.009/022) and schedule dates (PRCID.001). */
class InsurerRowParserTest {

  private static RawRow row(Map<String, String> values) {
    return new RawRow(2, values);
  }

  @Test
  void readsARowWhateverTheHeaderCaseAndSpacing() {
    RawRow row =
        row(
            Map.of(
                "invoice number", "BI-1",
                "POLICY NO", "POL-1",
                "Inception Date", "10/01/2026",
                "Expiry Date", "2027-10-01",
                "Gross Premium", "17,027.86",
                "Insurer", "ins-mgic",
                "Month of Production", "2026-09",
                "Remarks", "ok"));
    InsurerRow parsed = InsurerRowParser.parse(7L, row);
    assertThat(parsed.uploadId()).isEqualTo(7L);
    assertThat(parsed.side().referenceNo()).isEqualTo("BI-1");
    assertThat(parsed.side().policyNo()).isEqualTo("POL-1");
    assertThat(parsed.side().periodFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(parsed.side().periodTo()).isEqualTo(LocalDate.of(2027, 10, 1));
    assertThat(parsed.side().grossPremium()).isEqualByComparingTo("17027.86");
    assertThat(parsed.remarks()).isEqualTo("ok");
    assertThat(InsurerRowParser.insurer(row)).isEqualTo("INS-MGIC");
    assertThat(InsurerRowParser.month(row)).isEqualTo(LocalDate.of(2026, 9, 1));
  }

  @Test
  void acceptsAnyDateOfTheProductionMonth() {
    assertThat(InsurerRowParser.month(row(Map.of("Month of Production", "2026-09-15"))))
        .isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(InsurerRowParser.month(row(Map.of("Month of Production", "09/15/2026"))))
        .isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(InsurerRowParser.month(row(Map.of("Month of Production", "September")))).isNull();
    assertThat(InsurerRowParser.month(row(Map.of()))).isNull();
    assertThat(InsurerRowParser.insurer(row(Map.of()))).isNull();
  }

  @Test
  void refusesIncompleteOrUnreadableRows() {
    assertThatThrownBy(() -> InsurerRowParser.parse(1L, row(Map.of("Remarks", "x"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("neither invoice nor policy");
    assertThatThrownBy(
            () ->
                InsurerRowParser.parse(
                    1L, row(Map.of("Invoice Number", "BI-1", "Inception Date", "soon"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not a date");
    assertThatThrownBy(
            () ->
                InsurerRowParser.parse(
                    1L, row(Map.of("Invoice Number", "BI-1", "Gross Premium", "lots"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not an amount");
  }

  @Test
  void schedulesRunOnTheirDayAndMovePastEachRun() {
    LocalDate today = LocalDate.of(2026, 9, 24);
    ReconSchedule monthly =
        new ReconSchedule(
            1L, "INS", new ReconSchedule.Terms(Frequency.MONTHLY, 5, false, null, true), today);
    assertThat(monthly.getNextRunDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    monthly.ran("PRX-1", null, LocalDate.of(2026, 10, 5));
    assertThat(monthly.getNextRunDate()).isEqualTo(LocalDate.of(2026, 11, 5));
    assertThat(monthly.getLastExtractNo()).isEqualTo("PRX-1");
    ReconSchedule weekly =
        new ReconSchedule(
            1L, "INS", new ReconSchedule.Terms(Frequency.WEEKLY, 1, true, "a@b.ph", true), today);
    assertThat(weekly.getNextRunDate().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(weekly.getNextRunDate()).isAfterOrEqualTo(today);
    weekly.rollTo(LocalDate.of(2026, 10, 2));
    assertThat(weekly.getNextRunDate()).isEqualTo(LocalDate.of(2026, 10, 2));
  }
}
