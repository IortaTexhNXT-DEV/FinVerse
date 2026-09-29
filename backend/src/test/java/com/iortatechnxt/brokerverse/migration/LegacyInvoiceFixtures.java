package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Seed extracts of the open legacy invoice tests: a migrated EBIX client and one open invoice with
 * its insurer share and component positions (basic premium partly paid in legacy).
 */
final class LegacyInvoiceFixtures {

  private static final AtomicLong SEQUENCE =
      new AtomicLong(System.currentTimeMillis() / 1000 % 900_000L);

  /** Birth dates of the fixture clients (1925 to 2008). */
  static final long BIRTH_DATE_SLOTS = 500L;

  /** Days between the birth dates of consecutive tokens: more than twice the masking shift. */
  static final long BIRTH_DATE_SPACING_DAYS = 61L;

  private final MigrationTestSupport mig;
  private final JdbcTemplate jdbc;

  LegacyInvoiceFixtures(MigrationTestSupport mig, JdbcTemplate jdbc) {
    this.mig = mig;
    this.jdbc = jdbc;
  }

  /** A six-digit token unique within the run (keys of the seed extracts). */
  static String token() {
    return Long.toString(100_000L + SEQUENCE.getAndIncrement() % 900_000L);
  }

  /**
   * The birth date of the client of a token. Masking replaces the names by entries of short lists
   * (the first name is always the same) and moves a birth date by at most 30 days either way, so
   * clients born less than 61 days apart could share the masked person key (K3) and the second
   * would merge into the first on load. Dates 61 days apart for consecutive tokens keep the masked
   * keys of any {@value #BIRTH_DATE_SLOTS} consecutive tokens distinct.
   */
  static LocalDate birthDate(String t) {
    return LocalDate.of(1925, 1, 1)
        .plusDays(Long.parseLong(t) % BIRTH_DATE_SLOTS * BIRTH_DATE_SPACING_DAYS);
  }

  /** The C01 extract row of the client of a token. */
  static Map<String, String> clientRow(String t) {
    Map<String, String> r = new HashMap<>();
    r.put("legacy_client_no", "E" + t);
    r.put("client_type", "I");
    r.put("last_name", "Santos" + t);
    r.put("first_name", "Lea");
    r.put("birth_date", birthDate(t).toString());
    r.put("tin", "");
    r.put("market_segment", "CBG");
    r.put("email", "e" + t + "@example.ph");
    r.put("kyc_status", "COMPLETE");
    r.put("kyc_verified_date", "2026-05-02");
    r.put("kyc_review_due", "2029-05-02");
    r.put("client_status", "A");
    r.put("created_date", "2021-03-01");
    r.put("last_updated", "2027-10-01 09:00:00");
    return r;
  }

  String client(String t) throws Exception {
    Map<String, String> r = clientRow(t);
    JsonNode extract =
        mig.upload(
            "C01",
            "C01",
            "C01_EBIX_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(r),
            List.of("legacy_client_no"));
    JsonNode batch = mig.load("C01", List.of(extract.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt())
        .as(mig.rows(batch.get("batchNo").asText()))
        .isEqualTo(1);
    mig.accept(batch.get("batchNo").asText());
    return jdbc.queryForObject(
        "select client_code from crm_client where legacy_ref = ?", String.class, "E" + t);
  }

  private static Map<String, String> header(String no, String t) {
    Map<String, String> h = new HashMap<>();
    h.put("legacy_invoice_no", no);
    h.put("invoice_kind", "BOOKING");
    h.put("policy_no", "MC-" + t);
    h.put("legacy_client_no", "E" + t);
    h.put("assured_name", "Lea Santos");
    h.put("currency", "PHP");
    h.put("booking_date", "2027-11-02");
    h.put("invoice_date", "2027-11-02");
    h.put("due_date", "2027-12-02");
    h.put("inception_date", "2027-11-01");
    h.put("expiry_date", "2028-11-01");
    h.put("risk_code", "CAR-A");
    h.put("line_code", "ENG");
    h.put("market_segment", "CBG");
    h.put("ao_user_id", "AO01");
    h.put("sales_unit_code", "U01");
    h.put("branch_code", "MKT");
    h.put("cost_center", "OPS");
    h.put("gross_premium", "11200.00");
    h.put("commission", "1500.00");
    h.put("vat_on_commission", "180.00");
    h.put("wtax_rate_pct", "10");
    h.put("commission_realised", "500.00");
    h.put("deferred_vat_open", "120.00");
    h.put("dp_flag", "N");
    h.put("cwt_flag", "N");
    return h;
  }

  private static Map<String, String> withPolicy(Map<String, String> h, String policyRef) {
    if (policyRef != null) {
      h.put("legacy_policy_ref", policyRef);
    }
    return h;
  }

  /** Loads and accepts an EBIX policy header of the client of the token; returns its reference. */
  String loadHeader(String t) throws Exception {
    String ref = "EP" + t;
    Map<String, String> h = new HashMap<>();
    h.put("legacy_policy_ref", ref);
    h.put("cover_no", "EC" + t);
    h.put("cover_version", "1");
    h.put("policy_no", "MC-" + t);
    h.put("legacy_client_no", "E" + t);
    h.put("assured_name", "Lea Santos");
    h.put("risk_code", "CAR-A");
    h.put("package_code", "PKG-CAR");
    h.put("package_version", "2");
    h.put("line_code", "ENG");
    h.put("business_type", "NB");
    h.put("policy_status", "IF");
    h.put("inception_date", "2027-11-01");
    h.put("expiry_date", "2028-11-01");
    h.put("currency", "PHP");
    h.put("sum_insured", "900000.00");
    h.put("net_premium", "10000.00");
    h.put("gross_premium", "11200.00");
    h.put("payment_arrangement", "VIA_BROKER");
    h.put("ao_user_id", "AO01");
    h.put("sales_unit_code", "U01");
    h.put("branch_code", "MKT");
    h.put("market_segment", "CBG");
    String seq = t.substring(t.length() - 2);
    JsonNode p01 =
        mig.upload(
            "P01",
            "P01",
            "P01_EBIX_20271231_" + seq + ".csv",
            List.of(h),
            List.of("legacy_policy_ref"));
    JsonNode p01s =
        mig.upload(
            "P01",
            "P01S",
            "P01S_EBIX_20271231_" + seq + ".csv",
            List.of(
                Map.of(
                    "legacy_policy_ref", ref,
                    "share_seq", "1",
                    "insurer_code", "MGIC",
                    "share_pct", "100",
                    "lead_flag", "Y")),
            List.of("legacy_policy_ref", "share_seq"));
    JsonNode batch =
        mig.load("P01", List.of(p01.get("extractNo").asText(), p01s.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt())
        .as(mig.rows(batch.get("batchNo").asText()))
        .isEqualTo(2);
    mig.accept(batch.get("batchNo").asText());
    return ref;
  }

  private static Map<String, String> position(
      String no, String component, String booked, String paid, String open) {
    Map<String, String> p = new HashMap<>();
    p.put("legacy_invoice_no", no);
    p.put("component", component);
    p.put("booked", booked);
    p.put("adjusted", "0.00");
    p.put("paid", paid);
    p.put("remitted", "0.00");
    p.put("written_off", "0.00");
    p.put("open_balance", open);
    return p;
  }

  /** Uploads and loads one invoice; returns the batch. */
  JsonNode loadInvoice(String no, String t) throws Exception {
    return loadInvoice(no, t, null);
  }

  /**
   * Uploads and loads one invoice of a migrated policy header (P01 reference); returns the batch.
   */
  JsonNode loadInvoice(String no, String t, String policyRef) throws Exception {
    String seq = t.substring(t.length() - 2);
    JsonNode f01 =
        mig.upload(
            "F01",
            "F01",
            "F01_EBIX_20271231_" + seq + ".csv",
            List.of(withPolicy(header(no, t), policyRef)),
            List.of("legacy_invoice_no"));
    JsonNode f01s =
        mig.upload(
            "F01",
            "F01S",
            "F01S_EBIX_20271231_" + seq + ".csv",
            List.of(
                Map.of(
                    "legacy_invoice_no", no,
                    "share_seq", "1",
                    "insurer_code", "MGIC",
                    "share_pct", "100",
                    "lead_flag", "Y")),
            List.of("legacy_invoice_no", "share_seq"));
    List<Map<String, String>> positions = new ArrayList<>();
    positions.add(position(no, "BASIC", "10000.00", "4000.00", "6000.00"));
    positions.add(position(no, "DST", "1200.00", "0.00", "1200.00"));
    positions.add(position(no, "DTIP", "11200.00", "0.00", "11200.00"));
    positions.add(position(no, "COMMISSION", "1500.00", "0.00", "1500.00"));
    positions.add(position(no, "COMMISSION_VAT", "180.00", "0.00", "180.00"));
    positions.add(position(no, "WTAX", "150.00", "0.00", "150.00"));
    JsonNode f01c =
        mig.upload(
            "F01",
            "F01C",
            "F01C_EBIX_20271231_" + seq + ".csv",
            positions,
            List.of("legacy_invoice_no", "component"));
    assertThat(f01.get("status").asText()).isEqualTo("STAGED");
    assertThat(f01c.get("status").asText()).isEqualTo("STAGED");
    JsonNode batch =
        mig.load(
            "F01",
            List.of(
                f01.get("extractNo").asText(),
                f01s.get("extractNo").asText(),
                f01c.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt())
        .as(mig.rows(batch.get("batchNo").asText()))
        .isEqualTo(8);
    return batch;
  }

  BigDecimal balance(String invoiceNo, String component) {
    return jdbc.queryForObject(
        "select c.balance from ops_invoice_component c join ops_invoice i on i.id = c.invoice_id"
            + " where i.invoice_no = ? and c.component = ?",
        BigDecimal.class,
        invoiceNo,
        component);
  }
}
