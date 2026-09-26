package com.iortatechnxt.brokerverse.nbadmin.report;

import com.iortatechnxt.brokerverse.nbadmin.report.UserAccessHistory.Change;
import com.iortatechnxt.brokerverse.nbadmin.report.UserAccessHistory.Snapshot;
import com.iortatechnxt.brokerverse.nbadmin.report.UserAccessHistory.UserRow;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code UAM-USER-ACCESS} User Access Report (BRD 3.003.1, sample A; FR-UA-060): each user with the
 * group profiles held at the end of the as-of date, business unit and level, status, who created
 * and last modified the user (the approver and request number of the request, from the access
 * change log) and the last action. Sorted by user ID.
 */
@Component
public class UserAccessReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "UAM-USER-ACCESS";

  private static final String UNIT = "businessUnit";
  private static final String STATUS = "status";
  private static final String ACTIVE = "ACTIVE";
  private static final String DISABLED = "DISABLED";
  private static final String LOCKED = "LOCKED";

  private final UserAccessHistory history;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param history users and change log
   * @param clock clock
   */
  public UserAccessReport(UserAccessHistory history, Clock clock) {
    this.history = history;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return UamReportSupport.metadata(
        CODE,
        "User Access Report",
        "Users with their group profiles and who created, modified, deactivated or reactivated"
            + " them, as of a date (BRD 3.003.1)",
        List.of(
            UamReportSupport.asOfParam(),
            ParameterSpec.optional(UNIT, "Business Unit Group", ParameterType.TEXT),
            ParameterSpec.select(
                STATUS,
                "Status",
                List.of(UamReportSupport.ALL, ACTIVE, DISABLED, LOCKED),
                UamReportSupport.ALL),
            UamReportSupport.groupProfileParam()));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate asOf = UamReportSupport.asOf(p, clock);
    Instant end = UamReportSupport.endOf(asOf);
    boolean today = asOf.equals(LocalDate.now(clock.withZone(UamReportSupport.MANILA)));
    String unit = p.optionalText(UNIT).orElse(null);
    String status = p.text(STATUS);
    String profile = p.optionalText(UamReportSupport.GROUP_PROFILE).orElse(null);
    Snapshot snapshot = history.load();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (UserRow user : snapshot.users()) {
      if (!user.createdAt().isBefore(end) || !UamReportSupport.matches(unit, user.businessUnit())) {
        continue;
      }
      Set<String> roles = snapshot.rolesAt(user, end);
      String state = status(snapshot.enabledAt(user, end), today && user.locked());
      if ((profile == null
              || roles.stream().anyMatch(r -> UamReportSupport.same(profile.trim(), r)))
          && (UamReportSupport.ALL.equals(status) || status.equals(state))) {
        rows.add(row(snapshot, user, roles, state, end));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("fullName", "User Name"),
            ReportColumn.text("userId", "User ID / Windows ID"),
            ReportColumn.text("profiles", "User Group Profile"),
            ReportColumn.text("unit", "Business Unit / User Level"),
            ReportColumn.text(STATUS, "Status"),
            ReportColumn.text("createdBy", "Created by"),
            ReportColumn.date("createdAt", "Date Created"),
            ReportColumn.text("modifiedBy", "Modified by"),
            ReportColumn.date("modifiedAt", "Date Modified"),
            ReportColumn.text("lastAction", "Last Action / by"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note(
            "Group profiles and status as at the end of the as-of date (Philippine time), from the"
                + " access change log. Created / modified by: the approver and request number of"
                + " the access request, or the user who made a direct change.")
        .build();
  }

  private static String status(boolean enabled, boolean locked) {
    if (!enabled) {
      return DISABLED;
    }
    return locked ? LOCKED : ACTIVE;
  }

  private static Map<String, Object> row(
      Snapshot snapshot, UserRow user, Set<String> roles, String status, Instant end) {
    List<Change> changes = snapshot.before(user, end);
    Optional<Change> created =
        changes.stream().filter(c -> "CREATE_USER".equals(c.activity())).findFirst();
    Change last = changes.isEmpty() ? null : changes.get(changes.size() - 1);
    Change modified =
        changes.stream()
            .filter(c -> !"CREATE_USER".equals(c.activity()))
            .reduce((a, b) -> b)
            .orElse(null);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("fullName", user.fullName());
    m.put(
        "userId",
        user.windowsId() == null ? user.username() : user.username() + " / " + user.windowsId());
    m.put("profiles", UamReportSupport.roleNames(String.join(",", roles), snapshot.roleNames()));
    m.put(
        "unit",
        Objects.toString(user.businessUnit(), "-")
            + " / "
            + Objects.toString(user.userLevel(), "-"));
    m.put("status", UamReportSupport.words(status));
    m.put("createdBy", created.map(Change::actor).orElse(user.createdBy()));
    m.put("createdAt", UamReportSupport.date(created.map(Change::at).orElse(user.createdAt())));
    m.put("modifiedBy", modified == null ? null : modified.actor());
    m.put("modifiedAt", modified == null ? null : UamReportSupport.date(modified.at()));
    m.put(
        "lastAction",
        last == null
            ? null
            : UamReportSupport.activityName(last.activity()) + " / " + last.actor());
    return m;
  }
}
