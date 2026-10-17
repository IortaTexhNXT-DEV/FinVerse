package com.iortatechnxt.brokerverse.migration.quality.service.rules;

import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.quality.service.FindingSink;
import com.iortatechnxt.brokerverse.migration.quality.service.ObjectRules;
import com.iortatechnxt.brokerverse.migration.quality.service.ValidationScope;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Rules of the renewal advices already sent (layout P03; DATA_MIGRATION_DESIGN section 15.1): the
 * expiring term is a migrated header (DQ-018) whose expiry the row repeats and that expires from
 * the cut-over date to the end of the go-live renewal window (DQ-019); the advice was sent by the
 * last legacy business day (DQ-047) and not more than the lead time before the expiry (DQ-048,
 * warning); the cover was not already renewed in legacy (DQ-049).
 */
@Component
public class RaSentRules implements ObjectRules {

  private static final String REF = "legacy_policy_ref";
  private static final String EXPIRY = "expiry_date";
  private static final String SENT = "ra_sent_date";

  private final JdbcTemplate jdbc;

  /**
   * Creates the rules.
   *
   * @param jdbc JDBC (migrated headers)
   */
  public RaSentRules(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Set<String> layouts() {
    return Set.of("P03");
  }

  @Override
  public void check(ValidationScope scope, FindingSink sink) {
    Long companyId = scope.batch().getCompanyId();
    MigrationParameters p = scope.parameters();
    for (StageRow row : scope.rows("P03")) {
      Map<String, String> v = row.getRawPayload();
      String ref = Values.text(v.get(REF));
      Optional<Header> header = header(companyId, ref);
      if (header.isEmpty()) {
        sink.error(row, "DQ-018", REF, ref, "Policy " + ref + " is not among the migrated headers");
      } else {
        checkHeader(row, v, header.get(), p, sink);
      }
      checkSent(row, v, p, sink);
    }
  }

  private void checkHeader(
      StageRow row, Map<String, String> v, Header h, MigrationParameters p, FindingSink sink) {
    Optional<LocalDate> expiry = Values.date(v.get(EXPIRY));
    boolean inWindow =
        !h.expiry().isBefore(p.cutoverDate()) && !h.expiry().isAfter(p.goLiveRenewalTo());
    if (expiry.isEmpty() || !expiry.get().equals(h.expiry()) || !inWindow) {
      sink.error(
          row,
          "DQ-019",
          EXPIRY,
          v.get(EXPIRY),
          "The expiry must be "
              + h.expiry()
              + " and fall from "
              + p.cutoverDate()
              + " to "
              + p.goLiveRenewalTo());
    }
    if (renewedInLegacy(h)) {
      sink.error(
          row, "DQ-049", REF, h.ref(), "Policy " + h.ref() + " is already renewed in legacy");
    }
  }

  private static void checkSent(
      StageRow row, Map<String, String> v, MigrationParameters p, FindingSink sink) {
    Optional<LocalDate> sent = Values.date(v.get(SENT));
    Optional<LocalDate> expiry = Values.date(v.get(EXPIRY));
    if (sent.isEmpty()) {
      return;
    }
    if (sent.get().isAfter(p.lastLegacyBusinessDay())) {
      sink.error(
          row,
          "DQ-047",
          SENT,
          v.get(SENT),
          "The advice date is after the last legacy business day " + p.lastLegacyBusinessDay());
    }
    if (expiry.isPresent() && sent.get().isBefore(expiry.get().minusDays(p.raMaxLeadDays()))) {
      sink.warning(
          row,
          "DQ-048",
          SENT,
          v.get(SENT),
          "The advice was sent more than " + p.raMaxLeadDays() + " days before the expiry");
    }
  }

  private Optional<Header> header(Long companyId, String ref) {
    if (ref == null) {
      return Optional.empty();
    }
    List<Header> found =
        jdbc.query(
            "select l.legacy_ref, l.cover_no, a.period_to from acc_account_legacy l"
                + " join acc_account a on a.id = l.account_id"
                + " where l.company_id = ? and l.legacy_ref = ? and l.rolled_back_at is null",
            (rs, i) ->
                new Header(
                    rs.getString("legacy_ref"),
                    rs.getString("cover_no"),
                    rs.getObject("period_to", LocalDate.class)),
            companyId,
            ref);
    return found.stream().filter(h -> h.expiry() != null).findFirst();
  }

  private boolean renewedInLegacy(Header h) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from acc_account_legacy l join acc_account a on a.id = l.account_id"
                + " where l.cover_no = ? and l.rolled_back_at is null and a.period_from = ?",
            Integer.class,
            h.cover(),
            h.expiry());
    return n != null && n > 0;
  }

  private record Header(String ref, String cover, LocalDate expiry) {}
}
