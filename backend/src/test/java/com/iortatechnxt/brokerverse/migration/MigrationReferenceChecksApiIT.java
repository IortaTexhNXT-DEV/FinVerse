package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
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
 * The remaining reference objects (wave DM1-C): branches and the sales organisation checked against
 * BIBS, a legacy insurer the INSURER map creates, commission rates and a receipt series in use,
 * each with origin MIGRATED, the Origin filter of their set-up lists, and the rollback per batch.
 */
@IntegrationTest
class MigrationReferenceChecksApiIT {

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private MigrationTestSupport mig;
  private long company;

  @BeforeEach
  void setUp() throws Exception {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01");
    for (String o : List.of("R02", "R03", "R04", "R05", "R07", "R11")) {
      mig.signMapping(o);
    }
  }

  private JsonNode load(String object, List<Map<String, String>> rows, List<String> keys)
      throws Exception {
    String t = LegacyInvoiceFixtures.token();
    JsonNode extract =
        mig.upload(
            object,
            object,
            object + "_EBIX_20271120_" + t.substring(t.length() - 2) + ".csv",
            rows,
            keys);
    return mig.load(object, List.of(extract.get("extractNo").asText()));
  }

  private static Map<String, String> row(String... kv) {
    Map<String, String> r = new HashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      r.put(kv[i], kv[i + 1]);
    }
    return r;
  }

  @Test
  void branchesAndTheSalesOrganisationAreCheckedAgainstBibs() throws Exception {
    JsonNode branches =
        load(
            "R02",
            List.of(
                row(
                    "branch_code",
                    "MKT",
                    "branch_name",
                    "Makati",
                    "invoicing_branch_flag",
                    "Y",
                    "active_flag",
                    "Y"),
                row(
                    "branch_code",
                    "ZZZ",
                    "branch_name",
                    "Closed",
                    "invoicing_branch_flag",
                    "N",
                    "active_flag",
                    "N")),
            List.of("branch_code"));
    String rows = mig.rows(branches.get("batchNo").asText());
    assertThat(branches.get("counts").get("loaded").asInt()).as(rows).isEqualTo(1);
    assertThat(rows).contains("ZZZ");
    JsonNode sales =
        load(
            "R03",
            List.of(
                row(
                    "record_type",
                    "UNIT",
                    "unit_code",
                    "U01",
                    "unit_name",
                    "Metro 1",
                    "active_flag",
                    "Y"),
                row(
                    "record_type",
                    "AO",
                    "unit_code",
                    "U01",
                    "ao_user_id",
                    "AO01",
                    "ao_code",
                    "AO01",
                    "ao_name",
                    "Officer",
                    "active_flag",
                    "Y")),
            List.of("record_type", "unit_code", "ao_user_id"));
    assertThat(sales.get("counts").get("loaded").asInt())
        .as(mig.rows(sales.get("batchNo").asText()))
        .isEqualTo(2);
  }

  @Test
  void insurersRatesAndSeriesAreLoadedWithTheirOrigin() throws Exception {
    JsonNode insurers =
        load(
            "R04",
            List.of(
                row(
                    "insurer_code",
                    "NWG",
                    "insurer_name",
                    "New World General",
                    "tin",
                    "123-456-789-000",
                    "status",
                    "A"),
                row("insurer_code", "MGIC", "insurer_name", "Mabuhay General", "status", "A")),
            List.of("insurer_code"));
    assertThat(insurers.get("counts").get("loaded").asInt())
        .as(mig.rows(insurers.get("batchNo").asText()))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select origin from cat_insurer where company_id = ? and party_code = 'INS-NWG'",
                String.class,
                company))
        .isEqualTo("MIGRATED");
    JsonNode migrated =
        mig.get("ao", "/api/v1/catalog/insurers?companyId=" + company + "&origin=MIGRATED");
    assertThat(migrated.findValuesAsText("partyCode"))
        .contains("INS-NWG")
        .doesNotContain("INS-MGIC");

    JsonNode rates =
        load(
            "R07",
            List.of(
                row(
                    "insurer_code",
                    "MGIC",
                    "risk_code",
                    "CAR-A",
                    "rate_pct",
                    "17.5",
                    "effective_from",
                    "2027-01-01")),
            List.of("insurer_code", "risk_code", "effective_from"));
    assertThat(rates.get("counts").get("loaded").asInt())
        .as(mig.rows(rates.get("batchNo").asText()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select rate from cat_commission_rate where insurer_code = 'INS-MGIC'"
                    + " and product_code = 'CAR01' and origin = 'MIGRATED'",
                BigDecimal.class))
        .isEqualByComparingTo("17.5");

    JsonNode series =
        load(
            "R11",
            List.of(
                row(
                    "branch_code",
                    "MKT",
                    "kind",
                    "AR",
                    "atp_no",
                    "ATP-2027-01",
                    "prefix",
                    "LAR-",
                    "from_no",
                    "1",
                    "to_no",
                    "9999",
                    "last_used_no",
                    "500",
                    "next_no",
                    "501")),
            List.of("branch_code", "kind", "prefix"));
    String batchNo = series.get("batchNo").asText();
    assertThat(series.get("counts").get("loaded").asInt()).as(mig.rows(batchNo)).isEqualTo(1);
    Map<String, Object> s =
        jdbc.queryForMap(
            "select next_no, origin, migration_batch from csh_receipt_series where prefix = 'LAR-'");
    assertThat(((Number) s.get("next_no")).longValue()).isEqualTo(501L);
    assertThat(s.get("origin")).isEqualTo("MIGRATED");
    assertThat(s.get("migration_batch")).isEqualTo(batchNo);
  }
}
