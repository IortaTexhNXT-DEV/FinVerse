package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportRegistry;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.support.AsUser;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the seed profile (context and database shared with the other seed data tests) and runs the
 * standard reports that UAT checks on day one (conformance gap G03): each returns rows on a fresh
 * SIT/UAT environment, run by a seed user who holds the report's permission with the parameters a
 * user would choose.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "seed"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class StandardReportsSeedIT {

  static final List<String> CODES =
      List.of(
          "BCL-OUTSTANDING-PAST-DUE",
          "EB-PLACEMENT",
          "RNW-RA-DISPATCH",
          "ADJ-VALIDATION-LIST",
          "ADJ-MINBAL-FILE",
          "FIN-AR-SOO-FC",
          "FIN-BRS-PAYNOTIFY",
          "FIN-GL-ALLOCJV",
          "PRC-LEGACY-CHANGES",
          "TAX-MAP",
          "TAX-QAP");

  // TAX-SLS lists the sales documents of the VAT worksheet; the broker's sales are entered on the
  // return and not read from a source document, so it has no line to list (open in G03).

  /** The users who run a report in UAT, where the first holder of its permission sees less. */
  private static final Map<String, String> USERS = Map.of("RNW-RA-DISPATCH", "mkttl");

  @Autowired private ReportService reports;
  @Autowired private ReportRegistry registry;
  @Autowired private UserDirectory directory;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;

  /**
   * The parameters of the report's standard variant: the year to date (the month to date for the
   * bank payment notification, at most 31 days), every expiry for the renewal letters, today for an
   * as-of date, US dollars for the foreign currency statement, else the report's defaults.
   */
  private Map<String, String> params(ReportMetadata m) {
    LocalDate today = BusinessClock.today(clock);
    LocalDate start =
        "FIN-BRS-PAYNOTIFY".equals(m.code()) ? today.withDayOfMonth(1) : today.withDayOfYear(1);
    Map<String, String> p = new HashMap<>();
    for (ParameterSpec s : m.parameters()) {
      String name = s.name().toLowerCase(Locale.ROOT);
      String value =
          switch (s.type()) {
            case DATE -> date(name, start, today);
            case COMPANY ->
                String.valueOf(
                    jdbc.queryForObject(
                        "select id from org_company where code = 'FVI'", Long.class));
            case CURRENCY -> "USD";
            case SELECT -> s.defaultValue() != null ? s.defaultValue() : s.options().get(0);
            default -> s.defaultValue();
          };
      if (value != null) {
        p.put(s.name(), value);
      }
    }
    return p;
  }

  /** Every expiry for the renewal letters, else the period start or today. */
  private static String date(String name, LocalDate start, LocalDate today) {
    if (name.startsWith("expiry")) {
      return null;
    }
    return name.contains("from") ? start.toString() : today.toString();
  }

  @Test
  void everyStandardReportReturnsRowsOnDayOne() {
    Map<String, String> counts = new LinkedHashMap<>();
    List<String> empty = new ArrayList<>();
    for (String code : CODES) {
      ReportMetadata m = registry.get(code).metadata();
      String user =
          USERS.getOrDefault(code, directory.usersWithPermission(m.permission().name()).get(0));
      try {
        ReportResult r = as.run(user, () -> reports.run(code, params(m)));
        long rows = r.rows().stream().filter(x -> x.kind() == RowKind.DETAIL).count();
        counts.put(code, user + " " + params(m) + " -> " + rows);
        if (rows == 0) {
          empty.add(code);
        }
      } catch (RuntimeException e) {
        counts.put(code, user + " " + params(m) + " -> " + e.getMessage());
        empty.add(code);
      }
    }
    assertThat(empty).as(counts.toString()).isEmpty();
  }
}
