package com.iortatechnxt.brokerverse.nbadmin.report;

import com.iortatechnxt.brokerverse.nbadmin.report.UserAccessHistory.Change;
import com.iortatechnxt.brokerverse.nbadmin.report.UserAccessHistory.Snapshot;
import com.iortatechnxt.brokerverse.nbadmin.report.UserAccessHistory.UserRow;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@code UAM-GROUP-MEMBERS} Group Profile Membership (BRD 3.003.3, sample C; FR-UA-062): the
 * members of each group profile at the end of the as-of date, with who added each member and when,
 * and the last change to the member's profiles. A user removed from the profile before the as-of
 * date is not listed.
 */
@Component
public class GroupMembersReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "UAM-GROUP-MEMBERS";

  private static final String PROFILE = "profile";
  private static final String USERNAME = "username";

  private final UserAccessHistory history;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param history users and change log
   * @param clock clock
   */
  public GroupMembersReport(UserAccessHistory history, Clock clock) {
    this.history = history;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return UamReportSupport.metadata(
        CODE,
        "Group Profile Membership",
        "Members of each group profile with who added them and when (BRD 3.003.3)",
        List.of(UamReportSupport.groupProfileParam(), UamReportSupport.asOfParam()));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Instant end = UamReportSupport.endOf(UamReportSupport.asOf(p, clock));
    String profile = p.optionalText(UamReportSupport.GROUP_PROFILE).orElse(null);
    Snapshot snapshot = history.load();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (UserRow user : snapshot.users()) {
      if (!user.createdAt().isBefore(end)) {
        continue;
      }
      for (String role : snapshot.rolesAt(user, end)) {
        if (UamReportSupport.matches(profile, role)) {
          rows.add(row(snapshot, user, role, end));
        }
      }
    }
    rows.sort(
        Comparator.comparing((Map<String, Object> r) -> (String) r.get(PROFILE))
            .thenComparing(r -> (String) r.get(USERNAME)));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("fullName", "Member Username"),
            ReportColumn.text(USERNAME, "Member User ID"),
            ReportColumn.text("addedBy", "Created by"),
            ReportColumn.date("addedAt", "Date Created"),
            ReportColumn.text("modifiedBy", "Modified by"),
            ReportColumn.date("modifiedAt", "Date Modified"))
        .groupBy(PROFILE, "Group Profile")
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .note(
            "Membership at the end of the as-of date (Philippine time), from the access change"
                + " log; a member added before the log existed shows the creator of the user.")
        .build();
  }

  private static Map<String, Object> row(
      Snapshot snapshot, UserRow user, String role, Instant end) {
    Optional<Change> added = snapshot.addition(user, role, end);
    Change modified =
        snapshot.before(user, end).stream()
            .filter(c -> UserAccessHistory.ROLES.equals(c.attribute()))
            .reduce((a, b) -> b)
            .filter(c -> added.map(a -> c.at().isAfter(a.at())).orElse(true))
            .orElse(null);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(PROFILE, snapshot.roleNames().getOrDefault(role, role) + " (" + role + ")");
    m.put("fullName", user.fullName());
    m.put(USERNAME, user.username());
    m.put("addedBy", added.map(Change::actor).orElse(user.createdBy()));
    m.put("addedAt", UamReportSupport.date(added.map(Change::at).orElse(user.createdAt())));
    m.put("modifiedBy", modified == null ? null : modified.actor());
    m.put("modifiedAt", modified == null ? null : UamReportSupport.date(modified.at()));
    return m;
  }
}
