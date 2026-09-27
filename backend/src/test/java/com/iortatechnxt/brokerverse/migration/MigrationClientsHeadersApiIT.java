package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.migration.legacy.service.MigratedPolicyService;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Data Migration clients, policy headers and the renewal transition (wave DM1-C): legacy clients of
 * two systems matched and loaded as one migrated client with its origin and payout account, a
 * policy header imported as a booked account of origin MIGRATED, the renewal advice already sent,
 * the package map, the go-live renewal extraction served to Renewal and its check report, and the
 * Origin filter of the client search.
 */
@IntegrationTest
class MigrationClientsHeadersApiIT {

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MigratedPolicyService policies;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private MigrationTestSupport mig;
  private long company;

  @BeforeEach
  void setUp() throws Exception {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01", "R02", "R03", "R04", "R05");
  }

  private static Map<String, String> client(String no, String last, String tin) {
    Map<String, String> r = new HashMap<>();
    r.put("legacy_client_no", no);
    r.put("client_type", "I");
    r.put("last_name", last);
    r.put("first_name", "Ana");
    r.put("birth_date", "1985-04-15");
    r.put("tin", tin);
    r.put("market_segment", "CBG");
    r.put("email", no.toLowerCase() + "@example.ph");
    r.put("kyc_status", "COMPLETE");
    r.put("kyc_verified_date", "2026-05-02");
    r.put("kyc_review_due", "2029-05-02");
    r.put("client_status", "A");
    r.put("created_date", "2021-03-01");
    r.put("last_updated", "2027-10-01 09:00:00");
    return r;
  }

  /**
   * A second person with another first name and birth date: masking maps names to short lists and
   * shifts a birth date by an offset keyed on the date, so two rows sharing both would match on the
   * person key whenever their masked last names coincide.
   */
  private static Map<String, String> otherPerson(String no, String last) {
    Map<String, String> r = client(no, last, "");
    r.put("first_name", "Marco");
    r.put("birth_date", "1979-11-02");
    return r;
  }

  private static String token() {
    return LegacyInvoiceFixtures.token();
  }

  @Test
  void legacyClientsAndHeadersMigrateWithTheirOriginAndServeTheGoLiveExtraction() throws Exception {
    for (String o : List.of("C01", "C03", "P01", "P03")) {
      mig.signMapping(o);
    }
    String t = token();
    String tin = "9" + String.format("%011d", Long.parseLong(t));
    JsonNode qps =
        mig.upload(
            "C01",
            "C01",
            "C01_QPS_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(client("Q" + t, "Reyes" + t, tin), otherPerson("Q2" + t, "Cruz" + t)),
            List.of("legacy_client_no"));
    JsonNode ebix =
        mig.upload(
            "C01",
            "C01",
            "C01_EBIX_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(client("E" + t, "Reyes" + t, tin)),
            List.of("legacy_client_no"));
    assertThat(qps.get("status").asText()).isEqualTo("STAGED");
    assertThat(ebix.get("status").asText()).isEqualTo("STAGED");
    JsonNode clients =
        mig.load("C01", List.of(qps.get("extractNo").asText(), ebix.get("extractNo").asText()));
    assertThat(clients.get("counts").get("loaded").asInt())
        .as(mig.rows(clients.get("batchNo").asText()))
        .isEqualTo(3);
    mig.accept(clients.get("batchNo").asText());

    Map<String, Object> reyes =
        jdbc.queryForMap(
            "select c.client_code, c.status, c.origin, c.source_system, c.legacy_ref, c.migration_batch,"
                + " c.kyc_status from crm_client c where c.legacy_ref in (?, ?)",
            "Q" + t,
            "E" + t);
    assertThat(reyes.get("origin")).isEqualTo("MIGRATED");
    assertThat(reyes.get("status")).isEqualTo("CONFIRMED");
    assertThat(reyes.get("kyc_status")).isEqualTo("VERIFIED");
    assertThat(reyes.get("migration_batch")).isEqualTo(clients.get("batchNo").asText());
    String clientCode = (String) reyes.get("client_code");
    assertThat(
            jdbc.queryForList(
                "select target_code from mig_key_xref where legacy_key in (?, ?) and rolled_back_at is null",
                String.class,
                "Q" + t,
                "E" + t))
        .containsOnly(clientCode);

    // Personal data is masked outside production: the migrated client is found by its code.
    JsonNode found =
        mig.get(
            "ao",
            "/api/v1/crm/clients?companyId=" + company + "&origin=MIGRATED&code=" + clientCode);
    assertThat(found.get("content")).hasSize(1);
    assertThat(found.get("content").get(0).get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(found.get("content").get(0).get("migrationBatch").asText())
        .isEqualTo(clients.get("batchNo").asText());
    assertThat(
            mig.get(
                    "ao",
                    "/api/v1/crm/clients?companyId=" + company + "&origin=BIBS&code=" + clientCode)
                .get("content"))
        .isEmpty();

    JsonNode payout =
        mig.upload(
            "C03",
            "C03",
            "C03_EBIX_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(
                Map.of(
                    "legacy_client_no", "E" + t,
                    "mode", "CTA",
                    "payee_name", "Ana Reyes",
                    "bank_code", "",
                    "account_no", "001234567890",
                    "active_flag", "Y")),
            List.of("legacy_client_no", "account_no"));
    assertThat(
            mig.load("C03", List.of(payout.get("extractNo").asText()))
                .get("counts")
                .get("loaded")
                .asInt())
        .isEqualTo(1);

    String ref = "CV" + t + "-1";
    Map<String, String> header = new HashMap<>();
    header.put("legacy_policy_ref", ref);
    header.put("cover_no", "CV" + t);
    header.put("cover_version", "1");
    header.put("policy_no", "POL-" + t);
    header.put("legacy_client_no", "Q" + t);
    header.put("assured_name", "Ana Reyes");
    header.put("risk_code", "CAR-A");
    header.put("package_code", "PKG-CAR");
    header.put("package_version", "2");
    header.put("line_code", "ENG");
    header.put("business_type", "NB");
    header.put("policy_status", "IF");
    header.put("inception_date", "2027-01-20");
    header.put("expiry_date", "2028-01-20");
    header.put("currency", "PHP");
    header.put("sum_insured", "950000.00");
    header.put("net_premium", "21375.00");
    header.put("gross_premium", "25946.25");
    header.put("payment_arrangement", "VIA_BDOI");
    header.put("ao_user_id", "AO01");
    header.put("sales_unit_code", "U01");
    header.put("branch_code", "MKT");
    header.put("market_segment", "CBG");
    JsonNode p01 =
        mig.upload(
            "P01",
            "P01",
            "P01_QPS_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(header),
            List.of("legacy_policy_ref"));
    JsonNode p01s =
        mig.upload(
            "P01",
            "P01S",
            "P01S_QPS_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(
                Map.of(
                    "legacy_policy_ref", ref,
                    "share_seq", "1",
                    "insurer_code", "MGIC",
                    "share_pct", "100",
                    "lead_flag", "Y")),
            List.of("legacy_policy_ref", "share_seq"));
    JsonNode headers =
        mig.load("P01", List.of(p01.get("extractNo").asText(), p01s.get("extractNo").asText()));
    assertThat(headers.get("counts").get("loaded").asInt())
        .as(mig.rows(headers.get("batchNo").asText()))
        .isEqualTo(2);
    mig.accept(headers.get("batchNo").asText());
    Map<String, Object> account =
        jdbc.queryForMap(
            "select a.status, a.origin, a.client_code, a.insurer_code, a.gross_premium,"
                + " l.legacy_package_code, l.migration_batch from acc_account a"
                + " join acc_account_legacy l on l.account_id = a.id where l.legacy_ref = ?",
            ref);
    assertThat(account.get("status")).isEqualTo("BOOKED");
    assertThat(account.get("origin")).isEqualTo("MIGRATED");
    assertThat(account.get("client_code")).isEqualTo(clientCode);
    assertThat(account.get("insurer_code")).isEqualTo("INS-MGIC");
    assertThat(account.get("legacy_package_code")).isEqualTo("PKG-CAR");
    JsonNode byPolicy =
        mig.get("ao", "/api/v1/accounts?companyId=" + company + "&origin=MIGRATED&text=POL-" + t);
    assertThat(byPolicy.get("content")).hasSize(1);
    JsonNode summary = byPolicy.get("content").get(0);
    assertThat(summary.get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(summary.get("legacyRef").asText()).isEqualTo(ref);
    JsonNode record = mig.get("ao", "/api/v1/accounts/" + summary.get("id").asText());
    assertThat(record.get("legacy").get("legacyPackageCode").asText()).isEqualTo("PKG-CAR");
    assertThat(
            mig.get("ao", "/api/v1/accounts?companyId=" + company + "&origin=BIBS&text=POL-" + t)
                .get("content"))
        .isEmpty();

    Map<String, String> ra = new HashMap<>();
    ra.put("legacy_policy_ref", ref);
    ra.put("cover_no", "CV" + t);
    ra.put("expiry_date", "2028-01-20");
    ra.put("ra_sent_date", "2027-12-10");
    ra.put("ra_ref", "RA-" + t);
    ra.put("ra_channel", "EMAIL");
    ra.put("sent_to", "ana@example.ph");
    ra.put("tracker_name", "Retail tracker / Dec");
    JsonNode p03 =
        mig.upload(
            "P03",
            "P03",
            "P03_EXCEL_20271220_" + t.substring(t.length() - 2) + ".csv",
            List.of(ra),
            List.of("legacy_policy_ref"));
    JsonNode advices = mig.load("P03", List.of(p03.get("extractNo").asText()));
    assertThat(advices.get("counts").get("loaded").asInt()).isEqualTo(1);

    MigratedPolicyService.GoLive golive =
        policies.goLiveCandidates(company, LocalDate.of(2028, 1, 3), LocalDate.of(2028, 5, 31));
    MigratedPolicyService.Header candidate =
        golive.headers().stream().filter(h -> h.legacyRef().equals(ref)).findFirst().orElseThrow();
    assertThat(candidate.urgent()).isTrue();
    assertThat(candidate.raSent()).isNotNull();
    assertThat(candidate.raSent().reference()).isEqualTo("RA-" + t);
    assertThat(candidate.policy().legacyPackageCode()).isEqualTo("PKG-CAR");
    assertThat(candidate.parties().clientCode()).isEqualTo(clientCode);

    ReportResult check =
        as.run(
            "miglead",
            () -> reports.run("MIG-RENEWAL-GOLIVE", Map.of("companyId", String.valueOf(company))));
    assertThat(check.rows()).isNotEmpty();
  }

  @Test
  void thePackageMapIsLoadedForRenewalSanitation() throws Exception {
    mig.signMapping("R06");
    String t = token();
    Map<String, String> band = new HashMap<>();
    band.put("legacy_package_code", "PK" + t);
    band.put("legacy_package_version", "3");
    band.put("legacy_package_name", "Private car package");
    band.put("qualifier", "SI_BAND");
    band.put("qualifier_value", "0.00-2000000.00");
    band.put("action", "MAP");
    band.put("bibs_package_version", "MTR12 v1");
    Map<String, String> reject = new HashMap<>(band);
    reject.put("qualifier_value", "2000000.01-");
    reject.put("action", "REJECT");
    reject.put("bibs_package_version", "");
    JsonNode r06 =
        mig.upload(
            "R06",
            "R06",
            "R06_QPS_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(band, reject),
            List.of(
                "legacy_package_code", "legacy_package_version", "qualifier", "qualifier_value"));
    JsonNode batch = mig.load("R06", List.of(r06.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt()).isEqualTo(2);
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select action, product_code, product_version_no, si_from, si_to from mig_package_map"
                + " where legacy_package_code = ? order by action",
            "PK" + t);
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).get("product_code")).isEqualTo("MTR12");
    assertThat(rows.get(0).get("product_version_no")).isEqualTo(1);
    assertThat(rows.get(0).get("si_to")).isNotNull();
    assertThat(rows.get(1).get("action")).isEqualTo("REJECT");
    assertThat(rows.get(1).get("si_to")).isNull();
  }
}
