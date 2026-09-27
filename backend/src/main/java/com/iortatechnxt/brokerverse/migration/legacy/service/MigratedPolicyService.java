package com.iortatechnxt.brokerverse.migration.legacy.service;

import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The migrated policy headers served to Renewal (DATA_MIGRATION_DESIGN sections 15 and 15.1): the
 * headers expiring in a window for the daily extraction, and the go-live extraction of every header
 * expiring from the go-live date to the end of the go-live renewal window that was not renewed in
 * legacy, with the January expiries flagged URGENT and the renewal advices already sent (P03).
 *
 * <p>The records mirror {@code renewal.service.port.LegacyPolicySource}; the Renewal adapter maps
 * them one to one when the Renewal module is merged (seam until then).
 */
@Service
@Transactional(readOnly = true)
public class MigratedPolicyService {

  private static final String HEADERS =
      "select l.legacy_ref, l.source_system, a.arn, l.policy_no, l.cover_no, a.product_code,"
          + " a.line_code, l.legacy_package_code, l.legacy_package_version, a.period_from,"
          + " a.period_to, a.total_sum_insured, a.gross_premium, a.currency,"
          + " (select string_agg(p.pn_number, ';') from acc_account_pn p where p.account_id = a.id)"
          + " as pn_nos, a.client_code, a.client_name, l.assured_name, a.insurer_code,"
          + " a.account_officer, a.sales_team, a.market_segment, a.mortgagee_bank,"
          + " r.ra_sent_date, r.ra_ref, r.ra_channel, r.sent_to"
          + " from acc_account_legacy l join acc_account a on a.id = l.account_id"
          + " left join mig_ra_sent r on r.company_id = l.company_id"
          + " and r.legacy_policy_ref = l.legacy_ref and not r.rolled_back"
          + " where l.company_id = ? and l.rolled_back_at is null and a.status <> 'CANCELLED'"
          + " and a.period_to between ? and ?";

  private static final String NOT_RENEWED =
      " and not exists (select 1 from acc_account_legacy n join acc_account b on b.id = n.account_id"
          + " where n.company_id = l.company_id and n.cover_no = l.cover_no"
          + " and n.rolled_back_at is null and b.period_from = a.period_to)";

  private final JdbcTemplate jdbc;
  private final MigrationParameters parameters;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC
   * @param parameters migration parameters (urgent window)
   */
  public MigratedPolicyService(JdbcTemplate jdbc, MigrationParameters parameters) {
    this.jdbc = jdbc;
    this.parameters = parameters;
  }

  /**
   * Whether migrated headers can be served (always, once P01 is loaded).
   *
   * @return true
   */
  public boolean connected() {
    return true;
  }

  /**
   * Migrated headers expiring in a window, not renewed in legacy (daily extraction).
   *
   * @param companyId company
   * @param from first expiry
   * @param to last expiry
   * @return headers, earliest expiry first
   */
  public List<Header> expiringHeaders(Long companyId, LocalDate from, LocalDate to) {
    LocalDate urgentTo = parameters.renewalUrgentTo();
    return jdbc.query(
        HEADERS + NOT_RENEWED + " order by a.period_to, l.legacy_ref",
        (rs, i) -> header(rs, urgentTo),
        companyId,
        from,
        to);
  }

  /**
   * The go-live extraction: headers expiring from the go-live date to {@code to} not renewed in
   * legacy, and the number renewed in legacy.
   *
   * @param companyId company
   * @param goLive go-live date T
   * @param to last expiry of the window
   * @return candidates and renewed count
   */
  public GoLive goLiveCandidates(Long companyId, LocalDate goLive, LocalDate to) {
    List<Header> headers = expiringHeaders(companyId, goLive, to);
    Integer all =
        jdbc.queryForObject(
            "select count(*) from (" + HEADERS + ") x", Integer.class, companyId, goLive, to);
    return new GoLive(headers, (all == null ? 0 : all) - headers.size());
  }

  private static Header header(ResultSet rs, LocalDate urgentTo) throws SQLException {
    LocalDate expiry = rs.getObject("period_to", LocalDate.class);
    LocalDate sent = rs.getObject("ra_sent_date", LocalDate.class);
    return new Header(
        rs.getString("legacy_ref"),
        rs.getString("source_system"),
        rs.getString("arn"),
        new Policy(
            rs.getString("policy_no"),
            rs.getString("cover_no"),
            rs.getString("product_code"),
            rs.getString("line_code"),
            rs.getString("legacy_package_code"),
            rs.getObject("legacy_package_version") == null
                ? null
                : String.valueOf(rs.getInt("legacy_package_version")),
            rs.getObject("period_from", LocalDate.class),
            expiry,
            rs.getBigDecimal("total_sum_insured"),
            rs.getBigDecimal("gross_premium"),
            rs.getString("currency"),
            rs.getString("pn_nos")),
        new Parties(
            rs.getString("client_code"),
            rs.getString("client_name"),
            rs.getString("assured_name"),
            rs.getString("insurer_code"),
            rs.getString("account_officer"),
            rs.getString("sales_team"),
            rs.getString("market_segment"),
            rs.getString("mortgagee_bank")),
        expiry != null && !expiry.isAfter(urgentTo),
        sent == null
            ? null
            : new RaSent(
                sent, rs.getString("ra_ref"), rs.getString("ra_channel"), rs.getString("sent_to")));
  }

  /**
   * A migrated header.
   *
   * @param legacyRef legacy policy reference (the candidate key)
   * @param sourceSystem legacy source system
   * @param arn ARN of the migrated account
   * @param policy policy data
   * @param parties client, insurer and sales
   * @param urgent expiring by the end of the urgent window
   * @param raSent renewal advice already sent, null when none
   */
  public record Header(
      String legacyRef,
      String sourceSystem,
      String arn,
      Policy policy,
      Parties parties,
      boolean urgent,
      RaSent raSent) {}

  /**
   * Policy data of a header.
   *
   * @param policyNo policy number
   * @param coverNo cover number
   * @param productCode BIBS risk code
   * @param lineCode line
   * @param legacyPackageCode legacy package as given
   * @param legacyPackageVersion legacy package version
   * @param inceptionDate inception
   * @param expiryDate expiry
   * @param sumInsured sum insured
   * @param grossPremium gross premium
   * @param currency currency
   * @param pnNos promissory notes
   */
  public record Policy(
      String policyNo,
      String coverNo,
      String productCode,
      String lineCode,
      String legacyPackageCode,
      String legacyPackageVersion,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      BigDecimal sumInsured,
      BigDecimal grossPremium,
      String currency,
      String pnNos) {}

  /**
   * Parties of a header.
   *
   * @param clientCode BIBS client code
   * @param clientName client name
   * @param assuredName assured
   * @param insurerCode lead insurer
   * @param accountOfficer account officer
   * @param salesUnit sales unit
   * @param segment market segment
   * @param mortgageeBank mortgagee bank
   */
  public record Parties(
      String clientCode,
      String clientName,
      String assuredName,
      String insurerCode,
      String accountOfficer,
      String salesUnit,
      String segment,
      String mortgageeBank) {}

  /**
   * A renewal advice already sent before go-live.
   *
   * @param sentOn date sent
   * @param reference advice reference
   * @param channel channel
   * @param recipient recipient
   */
  public record RaSent(LocalDate sentOn, String reference, String channel, String recipient) {}

  /**
   * The go-live extraction.
   *
   * @param headers candidates
   * @param renewedInLegacy headers of the window already renewed in legacy
   */
  public record GoLive(List<Header> headers, int renewedInLegacy) {

    /** Defensive copy. */
    public GoLive {
      headers = List.copyOf(headers);
    }
  }
}
