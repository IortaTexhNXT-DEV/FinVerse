package com.iortatechnxt.brokerverse.brokerclaims;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Claims of the CL1-B tests (BRD-7): claims are inserted with SQL (the recording service is wave
 * CL1-A's) with unique numbers and ARNs, then given their first status through the status engine.
 */
@Component
public class BrokerClaimFixtures {

  /** Business time zone. */
  public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000_000L);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final AsUser as;
  private final ClaimStatusService statuses;
  private final BrokerClaimRepository claims;

  BrokerClaimFixtures(
      JdbcTemplate jdbc,
      TransactionTemplate tx,
      AsUser as,
      ClaimStatusService statuses,
      BrokerClaimRepository claims) {
    this.jdbc = jdbc;
    this.tx = tx;
    this.as = as;
    this.statuses = statuses;
    this.claims = claims;
  }

  /** Today in Manila. */
  public static LocalDate today() {
    return LocalDate.now(MANILA);
  }

  /** Company FVI. */
  public Long company() {
    return jdbc.queryForObject("select id from org_company where code = 'FVI'", Long.class);
  }

  /** A unique suffix. */
  public static String unique() {
    return Long.toString(SEQ.incrementAndGet(), 36).toUpperCase(Locale.ROOT);
  }

  /**
   * Inserts a claim without status (phase NEW).
   *
   * @param spec cover and loss facts
   * @return claim id
   */
  public Long insert(Spec spec) {
    return jdbc.queryForObject(
        "insert into bcl_claim (company_id, claim_no, unit_code, handler, source, arn,"
            + " policy_year, policy_no, client_code, assured_name, line_code, lead_insurer_code,"
            + " sales_team, account_officer, currency, loss_date, reported_date, loss_nature,"
            + " claim_type, catastrophe_code, claim_amount, deductible, initial_reserve,"
            + " claimant_name, phase, created_at, created_by)"
            + " values (?, ?, 'MOTOR_HO', ?, 'BDOI_NOTICE', ?, 2026, ?, ?, ?, 'MOTOR', ?, 'RETAIL 1',"
            + " 'ao', 'PHP', ?, ?, 'MOTOR_OWN_DAMAGE', 'MOTOR_OWN_DAMAGE', ?, ?, 1000.00, ?, ?,"
            + " 'NEW', now(), 'TEST') returning id",
        Long.class,
        company(),
        spec.claimNo(),
        spec.handler(),
        spec.arn(),
        "POL-" + spec.arn(),
        spec.client(),
        "Assured " + spec.client(),
        spec.insurer(),
        spec.reported().minusDays(1),
        spec.reported(),
        spec.catastrophe(),
        spec.amount(),
        spec.reserve(),
        "Claimant " + spec.client());
  }

  /**
   * Inserts a claim and sets its first status as its handler (officers of the demo register).
   *
   * @param spec facts
   * @param status first status
   * @return claim id
   */
  public Long recorded(Spec spec, String status) {
    Long id = insert(spec);
    as.run(
        spec.handler(),
        () ->
            tx.execute(
                s ->
                    statuses.recordInitialStatus(claims.findById(id).orElseThrow(), status, null)));
    return id;
  }

  /** A default claim of an officer, reported on a date. */
  public Spec spec(String handler, LocalDate reported) {
    String key = unique();
    return new Spec(
        "BCL-T-" + key,
        "ARN-T-" + key,
        "CL" + key,
        handler,
        reported,
        "INS-" + key,
        null,
        new BigDecimal("50000.00"),
        new BigDecimal("100000.00"));
  }

  /** Adds an insurer line. */
  public void insurerLine(
      Long claimId,
      String insurer,
      BigDecimal share,
      String number,
      BigDecimal reserve,
      BigDecimal settled) {
    jdbc.update(
        "insert into bcl_insurer_claim (company_id, claim_id, insurer_code, share_pct,"
            + " insurer_claim_no, reported_to_insurer_on, reserve_amount, settled_amount,"
            + " created_at, created_by) values (?, ?, ?, ?, ?, current_date, ?, ?, now(), 'TEST')",
        company(),
        claimId,
        insurer,
        share,
        number,
        reserve,
        settled);
  }

  /** Adds a location of the cover. */
  public void location(Long claimId, int itemNo, String key, String city) {
    jdbc.update(
        "insert into bcl_claim_location (claim_id, account_item_no, address, city, province,"
            + " location_key, created_at, created_by) values (?, ?, ?, ?, 'Metro Manila', ?,"
            + " now(), 'TEST')",
        claimId,
        itemNo,
        "1 Test Street " + itemNo,
        city,
        key);
  }

  /** Moves the time the current status was set (ageing tests). */
  public void statusSince(Long claimId, LocalDate day) {
    jdbc.update(
        "update bcl_claim set status_since = ? where id = ?",
        Timestamp.from(day.atStartOfDay(MANILA).toInstant()),
        claimId);
  }

  /** Reads one column of a claim. */
  public <T> T column(Long claimId, String column, Class<T> type) {
    return jdbc.queryForObject("select " + column + " from bcl_claim where id = ?", type, claimId);
  }

  /** Loads a claim in a transaction. */
  public Claim load(Long claimId) {
    return tx.execute(s -> claims.findById(claimId).orElseThrow());
  }

  /** Now. */
  public static Instant now() {
    return Instant.now();
  }

  /**
   * Facts of a test claim.
   *
   * @param claimNo claim number
   * @param arn account
   * @param client client code
   * @param handler handler
   * @param reported reported date (loss the day before)
   * @param insurer lead insurer
   * @param catastrophe catastrophe code
   * @param amount claim amount
   * @param reserve initial reserve
   */
  public record Spec(
      String claimNo,
      String arn,
      String client,
      String handler,
      LocalDate reported,
      String insurer,
      String catastrophe,
      BigDecimal amount,
      BigDecimal reserve) {

    /** Another claim (new number) on the same cover. */
    public Spec another() {
      return new Spec(
          "BCL-T-" + unique(),
          arn,
          client,
          handler,
          reported,
          insurer,
          catastrophe,
          amount,
          reserve);
    }

    /** The same claim with a catastrophe code. */
    public Spec withCatastrophe(String code) {
      return new Spec(claimNo, arn, client, handler, reported, insurer, code, amount, reserve);
    }
  }
}
