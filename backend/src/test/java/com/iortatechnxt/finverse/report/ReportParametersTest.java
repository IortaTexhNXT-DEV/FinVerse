package com.iortatechnxt.finverse.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportParametersTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-23T02:00:00Z"), ZoneOffset.UTC);

  private static final ReportMetadata PL =
      metadata(
          ParameterSpec.required("fromDate", "From Date", ParameterType.DATE)
              .withDefault("MONTH_START"),
          ParameterSpec.required("toDate", "To Date", ParameterType.DATE).withDefault("TODAY"),
          ParameterSpec.optional("expiryFrom", "Expiry Date From", ParameterType.DATE),
          ParameterSpec.optional("expiryTo", "Expiry Date To", ParameterType.DATE),
          ParameterSpec.optional("uwYearFrom", "UW Year From", ParameterType.NUMBER),
          ParameterSpec.optional("uwYearTo", "UW Year To", ParameterType.NUMBER),
          ParameterSpec.optional("partyFrom", "Party Code From", ParameterType.TEXT),
          ParameterSpec.optional("partyTo", "Party Code To", ParameterType.TEXT));

  private static ReportMetadata metadata(ParameterSpec... specs) {
    return new ReportMetadata(
        "T", "Test", ReportCategory.CONTROL, "", List.of(specs), Permission.REPORT_VIEW);
  }

  @Test
  void omittedParametersTakeTheirDefaults() {
    ReportParameters params = ReportParameters.validate(PL, Map.of(), CLOCK);
    assertThat(params.date("fromDate")).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(params.date("toDate")).isEqualTo(LocalDate.of(2026, 9, 23));
  }

  @Test
  void requiredParameterSentBlankIsRejectedInsteadOfDefaulted() {
    Map<String, String> raw = new HashMap<>();
    raw.put("toDate", " ");
    assertThatThrownBy(() -> ReportParameters.validate(PL, raw, CLOCK))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("To Date is required");
    raw.put("toDate", null);
    assertThatThrownBy(() -> ReportParameters.validate(PL, raw, CLOCK))
        .hasMessage("To Date is required");
  }

  @Test
  void reversedDateAndNumberRangesAreRejected() {
    assertThatThrownBy(
            () ->
                ReportParameters.validate(
                    PL, Map.of("fromDate", "2026-09-01", "toDate", "2026-01-31"), CLOCK))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("To Date must not be before From Date")
        .extracting("code")
        .isEqualTo("INVALID_REPORT_PARAMETERS");
    assertThatThrownBy(
            () ->
                ReportParameters.validate(
                    PL,
                    Map.of(
                        "fromDate", "2026-09-01",
                        "toDate", "2026-01-31",
                        "expiryFrom", "2026-12-31",
                        "expiryTo", "2026-12-01",
                        "uwYearFrom", "2026",
                        "uwYearTo", "2025"),
                    CLOCK))
        .hasMessage(
            "To Date must not be before From Date; Expiry Date To must not be before Expiry Date"
                + " From; UW Year To must not be before UW Year From");
  }

  @Test
  void validRangesOpenRangesAndCodeRangesPass() {
    ReportParameters params =
        ReportParameters.validate(
            PL,
            Map.of(
                "fromDate", "2026-09-01",
                "toDate", "2026-09-01",
                "expiryTo", "2020-01-01",
                "partyFrom", "Z",
                "partyTo", "A"),
            CLOCK);
    assertThat(params.optionalDate("expiryTo")).contains(LocalDate.of(2020, 1, 1));
  }

  @Test
  void malformedValuesAreReportedOnceByTheTypeCheck() {
    assertThatThrownBy(
            () ->
                ReportParameters.validate(
                    PL, Map.of("fromDate", "2026-13-01", "toDate", "2026-01-01"), CLOCK))
        .hasMessage("From Date has an invalid value '2026-13-01'");
  }
}
