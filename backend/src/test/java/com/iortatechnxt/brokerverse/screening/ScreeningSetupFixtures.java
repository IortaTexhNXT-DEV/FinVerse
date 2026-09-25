package com.iortatechnxt.brokerverse.screening;

import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.config.service.MatchCriteria;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Test data of the screening configuration and watchlists: an isolated company per test (the
 * configuration is per company) and test list sources, so tests never touch the demo versions and
 * sources and pass in any order.
 */
@Component
public class ScreeningSetupFixtures {

  private static final AtomicInteger SEQ = new AtomicInteger();

  private final JdbcTemplate jdbc;

  ScreeningSetupFixtures(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * A company without screening configuration.
   *
   * @return company id
   */
  public Long company() {
    String code = "S" + ((System.nanoTime() + SEQ.incrementAndGet()) % 100_000_000L);
    jdbc.update(
        """
        insert into org_company (code, name, base_currency, fiscal_year_start_month, back_value_days,
            forward_value_days, retained_earnings_account, record_status, authorized_by,
            authorized_at, created_at, created_by)
        values (?, ?, 'PHP', 1, 45, 5, '3500', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM')
        """,
        code,
        "Screening test " + code);
    return jdbc.queryForObject("select id from org_company where code = ?", Long.class, code);
  }

  /**
   * A FILE source of its own.
   *
   * @param listType list type
   * @param fullFile whether a file replaces the list
   * @return source code
   */
  public String source(String listType, boolean fullFile) {
    String code = "T" + (System.nanoTime() % 1_000_000_000L) + SEQ.incrementAndGet();
    jdbc.update(
        """
        insert into scr_watchlist_source (code, name, list_type, transport, schedule, file_layout,
            full_file, ref_prefix, active, created_at, created_by)
        values (?, ?, ?, 'FILE', 'Daily 01:00', 'CSV', ?, 'TST', true, now(), 'SYSTEM')
        """,
        code,
        "Test source " + code,
        listType,
        fullFile);
    return code;
  }

  /**
   * A unique prefix for list references and names.
   *
   * @return prefix
   */
  public String unique() {
    return "U" + (System.nanoTime() % 1_000_000_000L) + SEQ.incrementAndGet();
  }

  /**
   * A list file in the SCR_WATCHLIST template.
   *
   * @param rows data lines
   * @return CSV bytes
   */
  public static byte[] csv(String... rows) {
    StringBuilder b =
        new StringBuilder(
            "Reference,Entity Type,Primary Name,First Name,Last Name,Aliases,Birth Date,"
                + "Nationality,ID Numbers,Listed On,List Type,Remarks\n");
    for (String row : rows) {
      b.append(row).append('\n');
    }
    return b.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * A matching rule.
   *
   * @param list list type
   * @param algorithm algorithm
   * @param threshold threshold
   * @param caseScore case threshold
   * @return rule
   */
  public static MatchCriteria.Rule rule(
      String list, MatchAlgorithm algorithm, String threshold, String caseScore) {
    return new MatchCriteria.Rule(
        null,
        list,
        SubjectType.INDIVIDUAL,
        algorithm,
        new BigDecimal(threshold),
        Set.of(MatchField.NAME, MatchField.ALIAS),
        new BigDecimal(caseScore));
  }
}
