package com.iortatechnxt.brokerverse.brokerclaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

/**
 * The Claims Handling reports of design section 10 (BRCLM.026-034/038/040-043; FR-CL-060-066):
 * every report runs and exports to Excel, PDF and CSV; the ageing buckets, past due threshold,
 * settled outcome, loss figures, loss ratio, claims-prone flag, extract rows and the view / export
 * rights follow the FRS.
 */
@IntegrationTest
class BrokerClaimsReportsIT {

  private static final String UH = "clmuh";
  private static final String OFFICER = "clmofficer";
  private static final String TL = "clmtl";
  private static final List<String> CODES =
      List.of(
          "BCL-OUTSTANDING",
          "BCL-OUTSTANDING-PAST-DUE",
          "BCL-SETTLED",
          "BCL-AGEING",
          "BCL-AGEING-STATUS",
          "BCL-LOSS-EXPERIENCE",
          "BCL-LOSS-RATIO",
          "BCL-PENDING-ACTIONS",
          "BCL-PRONE-LOCATIONS",
          "BCL-INSURER-CLAIMS",
          "BCL-ACTIVITY-LOG",
          "BCL-DATA-EXTRACT");

  @Autowired private ReportService reports;
  @Autowired private BrokerClaimFixtures fixtures;
  @Autowired private ClaimClosureService closures;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ClaimsFixtures ledger;

  private Map<String, String> params(String handler) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", String.valueOf(fixtures.company()));
    p.put("grouping", "CLIENT");
    p.put("periodFrom", BrokerClaimFixtures.today().minusYears(1).toString());
    p.put("settledFrom", BrokerClaimFixtures.today().minusDays(40).toString());
    if (handler != null) {
      p.put("handler", handler);
    }
    return p;
  }

  private static List<Map<String, Object>> details(ReportResult result) {
    return result.rows().stream()
        .filter(r -> r.kind() == RowKind.DETAIL)
        .map(ReportRow::cells)
        .toList();
  }

  private static List<Object> column(ReportResult result, String key) {
    return details(result).stream().map(r -> r.get(key)).toList();
  }

  @Test
  void everyClaimsReportRunsAndExports() {
    Long claim =
        fixtures.recorded(
            fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(20)), "NEW_COMPLETE_DOCS");
    fixtures.insurerLine(
        claim,
        "INS-A",
        new BigDecimal("60"),
        "R-" + BrokerClaimFixtures.unique(),
        BigDecimal.TEN,
        null);
    fixtures.location(claim, 1, "LOC-" + BrokerClaimFixtures.unique(), "Makati");
    List<String> catalogue =
        as.run(UH, () -> reports.catalogue()).stream()
            .filter(m -> m.category() == ReportCategory.CLAIMS_HANDLING)
            .map(ReportMetadata::code)
            .toList();
    assertThat(catalogue).containsAll(CODES);
    for (String code : CODES) {
      assertThat(as.run(UH, () -> reports.run(code, params(null)))).as(code).isNotNull();
      for (ExportFormat format : List.of(ExportFormat.XLSX, ExportFormat.PDF, ExportFormat.CSV)) {
        assertThat(as.run(UH, () -> reports.export(code, params(null), format)).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
  }

  @Test
  void ageingOutstandingPastDueAndSettledFollowTheRules() {
    String handler = "clmofficer2";
    Long c45 =
        fixtures.recorded(
            fixtures.spec(handler, BrokerClaimFixtures.today().minusDays(45)), "NEW_COMPLETE_DOCS");
    Long c80 =
        fixtures.recorded(
            fixtures.spec(handler, BrokerClaimFixtures.today().minusDays(80)), "NEW_COMPLETE_DOCS");
    Long c95 =
        fixtures.recorded(
            fixtures.spec(handler, BrokerClaimFixtures.today().minusDays(95)),
            "TEMP_CLOSED_WITH_OFFER");
    Long settled =
        fixtures.recorded(
            fixtures.spec(handler, BrokerClaimFixtures.today().minusDays(10)), "NEW_COMPLETE_DOCS");
    Long denied =
        fixtures.recorded(
            fixtures.spec(handler, BrokerClaimFixtures.today().minusDays(10)), "NEW_COMPLETE_DOCS");
    Long company = fixtures.company();
    as.run(
        TL,
        () ->
            closures.settle(
                company,
                settled,
                new ClaimClosureService.Settlement(
                    "SETTLED_RELEASE_PAPERS",
                    new BigDecimal("85000"),
                    BrokerClaimFixtures.today(),
                    null)));
    as.run(
        TL,
        () ->
            closures.settle(
                company,
                denied,
                new ClaimClosureService.Settlement("CLOSED_DENIED", null, null, null)));

    ReportResult outstanding = as.run(UH, () -> reports.run("BCL-OUTSTANDING", params(handler)));
    assertThat(column(outstanding, "claim_no"))
        .contains(claimNo(c45), claimNo(c95))
        .doesNotContain(claimNo(settled));

    ReportResult ageing = as.run(UH, () -> reports.run("BCL-AGEING", params(handler)));
    assertThat(details(ageing))
        .filteredOn(r -> r.get("claim_no").equals(claimNo(c45)))
        .singleElement()
        .satisfies(r -> assertThat(r.get("bucket")).isEqualTo("31-60"));
    assertThat(ageing.rows()).anyMatch(r -> r.kind() == RowKind.SUBTOTAL);

    ReportResult pastDue =
        as.run(UH, () -> reports.run("BCL-OUTSTANDING-PAST-DUE", params(handler)));
    assertThat(column(pastDue, "claim_no")).contains(claimNo(c95)).doesNotContain(claimNo(c80));

    ReportResult settledList = as.run(UH, () -> reports.run("BCL-SETTLED", params(handler)));
    assertThat(column(settledList, "claim_no"))
        .contains(claimNo(settled))
        .doesNotContain(claimNo(denied));

    ReportResult byStatus = as.run(UH, () -> reports.run("BCL-AGEING-STATUS", params(handler)));
    assertThat(byStatus.rows())
        .anyMatch(r -> r.kind() == RowKind.GROUP_HEADER && r.label().contains("Temporary Closed"));

    Map<String, String> future = params(handler);
    future.put("asOf", BrokerClaimFixtures.today().plusDays(1).toString());
    assertThatThrownBy(() -> as.run(UH, () -> reports.run("BCL-AGEING", future)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("The as-of date cannot be in the future");
    Map<String, String> reversed = params(handler);
    reversed.put("settledTo", BrokerClaimFixtures.today().minusDays(60).toString());
    assertThatThrownBy(() -> as.run(UH, () -> reports.run("BCL-SETTLED", reversed)))
        .hasMessageContaining("Date settled to must not be before Date settled from");

    ReportResult pending = as.run(UH, () -> reports.run("BCL-PENDING-ACTIONS", params(handler)));
    assertThat(pending).isNotNull();
    assertThat(c80).isNotNull();
  }

  private String claimNo(Long id) {
    return fixtures.column(id, "claim_no", String.class);
  }

  @Test
  void lossExperienceAndLossRatio() {
    BrokerClaimFixtures.Spec spec =
        fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(30));
    Long open = fixtures.recorded(spec, "NEW_COMPLETE_DOCS");
    fixtures.insurerLine(
        open,
        "INS-A",
        new BigDecimal("100"),
        "N-" + BrokerClaimFixtures.unique(),
        new BigDecimal("100000"),
        new BigDecimal("60000"));
    Long over = fixtures.recorded(spec.another(), "NEW_COMPLETE_DOCS");
    fixtures.insurerLine(
        over,
        "INS-B",
        new BigDecimal("100"),
        "O-" + BrokerClaimFixtures.unique(),
        new BigDecimal("100000"),
        new BigDecimal("110000"));
    OpsInvoice booked = ledger.motorInvoice();
    jdbc.update(
        "update bcl_claim set arn = ?, policy_year = ? where id in (?, ?)",
        booked.getArn(),
        booked.getPolicyYear(),
        open,
        over);

    Map<String, String> p = params(null);
    p.put("clientCode", spec.client());
    ReportResult experience = as.run(UH, () -> reports.run("BCL-LOSS-EXPERIENCE", p));
    assertThat(details(experience))
        .filteredOn(r -> r.get("claim_no").equals(spec.claimNo()))
        .singleElement()
        .satisfies(
            r -> {
              assertThat((BigDecimal) r.get("outstanding")).isEqualByComparingTo("40000");
              assertThat((BigDecimal) r.get("total")).isEqualByComparingTo("100000");
            });
    assertThat(details(experience))
        .filteredOn(r -> r.get("insurer_code").equals("INS-B"))
        .singleElement()
        .satisfies(
            r -> {
              assertThat((BigDecimal) r.get("outstanding")).isEqualByComparingTo("0");
              assertThat((BigDecimal) r.get("total")).isEqualByComparingTo("110000");
            });

    ReportResult ratio = as.run(UH, () -> reports.run("BCL-LOSS-RATIO", p));
    BigDecimal premium = booked.getGrossPremium();
    assertThat(details(ratio))
        .singleElement()
        .satisfies(
            r -> {
              assertThat((BigDecimal) r.get("premium")).isEqualByComparingTo(premium);
              assertThat((BigDecimal) r.get("losses")).isEqualByComparingTo("210000");
              assertThat((BigDecimal) r.get("ratio"))
                  .isEqualByComparingTo(
                      new BigDecimal("21000000").divide(premium, 2, RoundingMode.HALF_UP));
            });
    Map<String, String> noGrouping = new HashMap<>(p);
    noGrouping.remove("grouping");
    assertThatThrownBy(() -> as.run(UH, () -> reports.run("BCL-LOSS-RATIO", noGrouping)))
        .hasMessage("Select the grouping");

    assertThat(as.run("ao", () -> reports.run("BCL-LOSS-EXPERIENCE", p))).isNotNull();
    assertThatThrownBy(
            () -> as.run("ao", () -> reports.export("BCL-LOSS-EXPERIENCE", p, ExportFormat.XLSX)))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(
            as.run("mkttl", () -> reports.export("BCL-LOSS-EXPERIENCE", p, ExportFormat.XLSX))
                .content())
        .isNotEmpty();
  }

  @Test
  void pronLocationsInsurerNumbersExtractAndActivityLog() {
    String key = "LOC-" + BrokerClaimFixtures.unique();
    Long first = null;
    for (int i = 0; i < 3; i++) {
      BrokerClaimFixtures.Spec spec =
          fixtures
              .spec(OFFICER, BrokerClaimFixtures.today().minusDays(100L + i))
              .withCatastrophe(i == 0 ? "FLOOD" : null);
      Long id = fixtures.recorded(spec, "NEW_COMPLETE_DOCS");
      fixtures.location(id, 1, key, "Pasig");
      first = first == null ? id : first;
    }
    Map<String, String> p = params(null);
    p.put("periodFrom", BrokerClaimFixtures.today().minusYears(3).toString());
    ReportResult prone = as.run("clmrisk", () -> reports.run("BCL-PRONE-LOCATIONS", p));
    assertThat(details(prone))
        .filteredOn(r -> key.equals(r.get("location_key")))
        .singleElement()
        .satisfies(r -> assertThat(r.get("prone")).isEqualTo("Yes"));
    Map<String, String> flood = new HashMap<>(p);
    flood.put("catastropheCode", "FLOOD");
    assertThat(details(as.run("clmrisk", () -> reports.run("BCL-PRONE-LOCATIONS", flood))))
        .filteredOn(r -> key.equals(r.get("location_key")))
        .singleElement()
        .satisfies(r -> assertThat(((Number) r.get("claims")).intValue()).isEqualTo(1));
    Map<String, String> drill = new HashMap<>(p);
    drill.put("locationKey", key);
    assertThat(details(as.run("clmrisk", () -> reports.run("BCL-PRONE-LOCATIONS", drill))))
        .hasSize(3);

    Long claim = first;
    String a = "C-INSA-" + BrokerClaimFixtures.unique();
    String b = "C-INSB-" + BrokerClaimFixtures.unique();
    fixtures.insurerLine(claim, "INS-A", new BigDecimal("60"), a, null, null);
    fixtures.insurerLine(claim, "INS-B", new BigDecimal("40"), b, null, null);
    fixtures.location(claim, 2, key + "-2", "Makati");
    fixtures.location(claim, 3, key + "-3", "Cebu");
    ReportResult numbers = as.run(UH, () -> reports.run("BCL-INSURER-CLAIMS", params(null)));
    String claimNo = claimNo(claim);
    assertThat(details(numbers))
        .filteredOn(r -> claimNo.equals(r.get("claim_no")))
        .extracting(r -> r.get("insurer_claim_no"))
        .containsExactlyInAnyOrder(a, b);

    Map<String, String> extract = params(null);
    extract.put("handler", OFFICER);
    ReportResult rows = as.run(UH, () -> reports.run("BCL-DATA-EXTRACT", extract));
    assertThat(details(rows)).filteredOn(r -> claimNo.equals(r.get("claim_no"))).hasSize(6);
    assertThatThrownBy(() -> as.run(OFFICER, () -> reports.run("BCL-DATA-EXTRACT", extract)))
        .isInstanceOf(AccessDeniedException.class);

    Map<String, String> log = params(null);
    log.put("claimNo", claimNo);
    log.put("periodFrom", BrokerClaimFixtures.today().minusDays(1).toString());
    ReportResult activity = as.run(UH, () -> reports.run("BCL-ACTIVITY-LOG", log));
    assertThat(column(activity, "activity")).contains("Status");
  }
}
