package com.iortatechnxt.brokerverse.migration.seed;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverPlan;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverTask;
import com.iortatechnxt.brokerverse.migration.cutover.domain.DecommissionItem;
import com.iortatechnxt.brokerverse.migration.cutover.service.CutoverService;
import com.iortatechnxt.brokerverse.migration.cutover.service.DecommissionService;
import com.iortatechnxt.brokerverse.migration.cutover.service.RunoffService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The seed storyline of the migration tabs after the loads (DATA_MIGRATION_DESIGN sections 18, 19
 * and 24): a mock cutover plan well under way with its go / no-go criteria measured, the production
 * plan not yet started, a run-off snapshot, the EBIX decommissioning checklist with its first
 * criteria met - so every tab of the console opens on records. Idempotent.
 */
@Component
public class MigrationTabsStoryline {

  private static final String LEAD = "miglead";
  private static final String SYSTEM = "EBIX";

  /** Name of the seeded mock plan (idempotency). */
  public static final String MOCK_PLAN = "Mock run 1";

  /** Name of the seeded production plan. */
  public static final String PRODUCTION_PLAN = "Production cutover";

  private static final int MOCK_TASKS_DONE = 6;
  private static final int CHECKLIST_MET = 2;

  private final CutoverService cutover;
  private final RunoffService runoff;
  private final DecommissionService decommission;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the storyline.
   *
   * @param cutover cutover plans
   * @param runoff run-off snapshots
   * @param decommission decommissioning checklists
   * @param jdbc JDBC (idempotency)
   * @param clock clock
   */
  public MigrationTabsStoryline(
      CutoverService cutover,
      RunoffService runoff,
      DecommissionService decommission,
      JdbcTemplate jdbc,
      Clock clock) {
    this.cutover = cutover;
    this.runoff = runoff;
    this.decommission = decommission;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Seeds the tabs of a company once.
   *
   * @param companyId company
   * @param as runs a step as a SIT/UAT user
   */
  public void run(Long companyId, MigrationStoryline.RunAs as) {
    if (none(
        jdbc.queryForObject(
            "select count(*) from mig_cutover_plan where company_id = ? and name = ?",
            Integer.class,
            companyId,
            MOCK_PLAN))) {
      plans(companyId, as);
    }
    if (none(
        jdbc.queryForObject(
            "select count(*) from mig_runoff_cohort where company_id = ? and snapshot_date = ?",
            Integer.class,
            companyId,
            BusinessClock.today(clock)))) {
      as.as(LEAD, () -> runoff.snapshot(companyId, BusinessClock.today(clock)));
    }
    if (none(
        jdbc.queryForObject(
            "select count(*) from mig_decommission_item where company_id = ? and system_code = ?",
            Integer.class,
            companyId,
            SYSTEM))) {
      checklist(companyId, as);
    }
  }

  private void plans(Long companyId, MigrationStoryline.RunAs as) {
    LocalDate mockGoLive = LocalDate.parse("2027-10-04");
    CutoverPlan mock =
        as.as(
            LEAD,
            () ->
                cutover.create(
                    companyId,
                    new CutoverPlan.Data(
                        MOCK_PLAN,
                        CutoverPlan.Kind.MOCK,
                        1,
                        "SIT",
                        mockGoLive,
                        LocalDateTime.parse("2027-10-01T18:00:00"),
                        LocalDateTime.parse("2027-10-04T06:00:00"))));
    for (int seq = 1; seq <= MOCK_TASKS_DONE; seq++) {
      int task = seq;
      as.as(
          LEAD,
          () -> cutover.progress(mock.getPlanNo(), task, CutoverTask.Status.DONE, "Done in SIT"));
    }
    as.as(
        LEAD,
        () ->
            cutover.progress(
                mock.getPlanNo(),
                MOCK_TASKS_DONE + 1,
                CutoverTask.Status.IN_PROGRESS,
                "Loads running"));
    as.as(LEAD, () -> cutover.measure(mock.getPlanNo()));
    as.as(
        LEAD,
        () ->
            cutover.create(
                companyId,
                new CutoverPlan.Data(
                    PRODUCTION_PLAN,
                    CutoverPlan.Kind.PRODUCTION,
                    null,
                    "PROD",
                    LocalDate.parse("2028-01-03"),
                    LocalDateTime.parse("2027-12-31T18:00:00"),
                    LocalDateTime.parse("2028-01-03T06:00:00"))));
  }

  private void checklist(Long companyId, MigrationStoryline.RunAs as) {
    List<DecommissionItem> items = as.as(LEAD, () -> decommission.open(companyId, SYSTEM));
    for (DecommissionItem item : items.subList(0, Math.min(CHECKLIST_MET, items.size()))) {
      as.as(
          LEAD,
          () ->
              decommission.update(
                  item.getId(), DecommissionItem.Status.MET, "Checked in the mock run"));
    }
  }

  private static boolean none(Integer count) {
    return count == null || count == 0;
  }
}
