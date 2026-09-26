package com.iortatechnxt.brokerverse.brokerclaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.diary.domain.DiaryEntry;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryQuery;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryService;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryService.DiaryInput;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.ClaimAssignmentService;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.ClaimsHomeService;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.Flag;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.Tab;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.WorklistCriteria;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.WorklistRow;
import com.iortatechnxt.brokerverse.brokerclaims.service.ClaimExperienceQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.AgeingAlertsJob;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.FollowUpDueJob;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Follow-up and work of wave CL1-B (BRCLM.019/022/025/034/043, FR-CL-052/054/055/062): the diary
 * and My Diary, the worklist tabs and search, reassignment, the Claims home, the follow-up and
 * ageing jobs, and the loss experience served to Renewal and the account page.
 */
@IntegrationTest
class ClaimsWorkIT {

  private static final String OFFICER = "clmofficer";
  private static final String TL = "clmtl";

  @Autowired private BrokerClaimFixtures fixtures;
  @Autowired private DiaryService diary;
  @Autowired private DiaryQuery myDiary;
  @Autowired private WorklistQuery worklist;
  @Autowired private ClaimAssignmentService assignments;
  @Autowired private ClaimsHomeService home;
  @Autowired private ClaimClosureService closures;
  @Autowired private ClaimExperienceQueryService experience;
  @Autowired private FollowUpDueJob followUpJob;
  @Autowired private AgeingAlertsJob ageingJob;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private int notifications(String user, String text) {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from msg_notification where lower(recipient) = ? and title like ?",
            Integer.class,
            user,
            "%" + text + "%");
    return count == null ? 0 : count;
  }

  private int alerts(String code, Long claimId) {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from alt_alert where dedup_key = ?",
            Integer.class,
            code + ":" + claimId);
    return count == null ? 0 : count;
  }

  @Test
  void theDiaryIsKeptPerClaimAndPerAssignee() {
    Long company = fixtures.company();
    BrokerClaimFixtures.Spec spec =
        fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(4));
    Long id = fixtures.recorded(spec, "NEW_COMPLETE_DOCS");
    LocalDate today = BrokerClaimFixtures.today();
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () -> diary.add(company, id, new DiaryInput("NOTE", today, null, null, " "))))
        .hasMessage("Enter the diary text");
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        diary.add(
                            company,
                            id,
                            new DiaryInput("FOLLOW_UP", today, today.minusDays(1), null, "Call"))))
        .hasMessage("The due date must be on or after the entry date");
    DiaryEntry entry =
        as.run(
            OFFICER,
            () ->
                diary.add(
                    company,
                    id,
                    new DiaryInput("FOLLOW_UP", today, today.plusDays(1), TL, "Chase the LOA")));
    assertThat(notifications(TL, spec.claimNo())).isEqualTo(1);
    assertThat(as.run(TL, () -> myDiary.mine(company, false, 0, 200)).content())
        .anyMatch(d -> d.id().equals(entry.getId()) && d.claimNo().equals(spec.claimNo()));
    assertThatThrownBy(
            () -> as.run("clmofficer2", () -> diary.markDone(company, entry.getId(), null)))
        .hasMessage("Only the assignee or the author can complete this entry");
    as.run(TL, () -> diary.markDone(company, entry.getId(), "Called"));
    assertThat(as.run(TL, () -> myDiary.mine(company, false, 0, 200)).content())
        .noneMatch(d -> d.id().equals(entry.getId()));
    assertThat(as.run(TL, () -> myDiary.mine(company, true, 0, 200)).content())
        .anyMatch(d -> d.id().equals(entry.getId()) && d.doneBy().equals(TL));
    as.run(
        TL,
        () ->
            closures.settle(
                company,
                id,
                new ClaimClosureService.Settlement("CLOSED_DENIED", null, null, null)));
    as.run(
        OFFICER, () -> diary.add(company, id, new DiaryInput("NOTE", null, null, null, "Filed")));
    assertThat(as.run(OFFICER, () -> diary.entries(company, id))).hasSize(2);
  }

  @Test
  void theWorklistTabsSearchAndReassignment() {
    Long company = fixtures.company();
    BrokerClaimFixtures.Spec spec =
        fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(6));
    Long open = fixtures.recorded(spec, "NEW_COMPLETE_DOCS");
    String insurerNo = "C-INSA-" + BrokerClaimFixtures.unique();
    fixtures.insurerLine(open, "INS-A", new BigDecimal("60"), insurerNo, null, null);
    Long temp =
        fixtures.recorded(
            fixtures.spec(OFFICER, BrokerClaimFixtures.today()), "TEMP_CLOSED_NO_DOCS");

    List<WorklistRow> found =
        as.run(
                OFFICER,
                () ->
                    worklist.search(
                        company, new WorklistCriteria(Tab.ALL, null, null, insurerNo), 0, 20))
            .content();
    assertThat(found)
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.id()).isEqualTo(open);
              assertThat(r.cover().insurerClaimNos()).isEqualTo(insurerNo);
              assertThat(r.dates().ageOverall()).isEqualTo(6);
            });
    assertThat(search(Tab.TEMP_CLOSED, spec.claimNo())).isEmpty();
    assertThat(search(Tab.MINE, spec.claimNo())).extracting(WorklistRow::id).containsExactly(open);
    assertThat(
            as.run(
                    OFFICER,
                    () ->
                        worklist.search(
                            company,
                            new WorklistCriteria(Tab.TEMP_CLOSED, null, null, null),
                            0,
                            200))
                .content())
        .extracting(WorklistRow::id)
        .contains(temp);

    assertThatThrownBy(
            () -> as.run(TL, () -> assignments.reassign(company, List.of(open, temp), " ", null)))
        .hasMessage("Select the new handler");
    assertThat(as.run(TL, () -> assignments.assignees()))
        .extracting(ClaimAssignmentService.Assignee::username)
        .contains("clmofficer2");
    int moved =
        as.run(
            TL, () -> assignments.reassign(company, List.of(open, temp), "clmofficer2", "Leave"));
    assertThat(moved).isEqualTo(2);
    assertThat(fixtures.column(open, "handler", String.class)).isEqualTo("clmofficer2");
    assertThat(fixtures.column(open, "unit_code", String.class)).isEqualTo("NON_MOTOR_HO");
    assertThat(
            jdbc.queryForObject(
                "select assignee from wf_case where entity_type = 'BrokerClaim' and entity_id = ?",
                String.class,
                String.valueOf(open)))
        .isEqualTo("clmofficer2");
    assertThat(notifications("clmofficer2", "2 claim(s) assigned")).isPositive();
  }

  private List<WorklistRow> search(Tab tab, String text) {
    return as.run(
            OFFICER,
            () ->
                worklist.search(
                    fixtures.company(), new WorklistCriteria(tab, null, null, text), 0, 20))
        .content();
  }

  @Test
  void theHomeTilesAndTheJobs() {
    Long company = fixtures.company();
    Long due =
        fixtures.recorded(
            fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(2)), "NEW_COMPLETE_DOCS");
    jdbc.update(
        "update bcl_claim set next_follow_up_date = ? where id = ?",
        BrokerClaimFixtures.today(),
        due);
    Long overdue =
        fixtures.recorded(
            fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(95)), "NEW_COMPLETE_DOCS");
    jdbc.update(
        "update bcl_claim set next_follow_up_date = ?, premium_status = 'UNPAID' where id = ?",
        BrokerClaimFixtures.today().minusDays(2),
        overdue);

    ClaimsHomeService.Home tiles = as.run(OFFICER, () -> home.home(company));
    assertThat(tiles.tiles())
        .extracting(ClaimsHomeService.Tile::key)
        .contains("mine", "overdue", "awaiting");
    assertThat(tiles.tiles())
        .filteredOn(t -> t.key().equals("overdue"))
        .singleElement()
        .satisfies(t -> assertThat(t.value()).isPositive());
    assertThat(tiles.ageing())
        .extracting(ClaimsHomeService.BucketCount::bucket)
        .containsExactly("0-30", "31-60", "61-90", "91-180", "181+");
    assertThat(
            as.run(
                    OFFICER,
                    () ->
                        worklist.search(
                            company,
                            new WorklistCriteria(Tab.ALL, Flag.UNPAID_PREMIUM, null, null),
                            0,
                            200))
                .content())
        .extracting(WorklistRow::id)
        .contains(overdue);
    assertThat(
            as.run(
                    OFFICER,
                    () ->
                        worklist.search(
                            company,
                            new WorklistCriteria(Tab.FOLLOW_UPS_DUE, null, null, null),
                            0,
                            200))
                .content())
        .extracting(WorklistRow::id)
        .contains(due, overdue);

    followUpJob.execute(BrokerClaimFixtures.today());
    followUpJob.execute(BrokerClaimFixtures.today());
    assertThat(alerts("BCL_FOLLOW_UP_OVERDUE", overdue)).isEqualTo(1);
    assertThat(alerts("BCL_FOLLOW_UP_OVERDUE", due)).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = 'clmofficer'"
                    + " and entity_id = ? and title like '%due today%'",
                Integer.class, String.valueOf(due)))
        .isPositive();
    ageingJob.execute(BrokerClaimFixtures.today());
    assertThat(alerts("BCL_CLAIM_PAST_DUE", overdue)).isEqualTo(1);
    assertThat(alerts("BCL_CLAIM_PAST_DUE", due)).isZero();
  }

  @Test
  void theLossExperienceOfACoverForRenewal() {
    Long company = fixtures.company();
    BrokerClaimFixtures.Spec spec =
        fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(30));
    Long open = fixtures.recorded(spec, "NEW_COMPLETE_DOCS");
    fixtures.insurerLine(
        open,
        "INS-A",
        new BigDecimal("100"),
        "N1",
        new BigDecimal("100000"),
        new BigDecimal("60000"));
    Long closed = fixtures.recorded(spec.another(), "NEW_COMPLETE_DOCS");
    as.run(
        TL,
        () ->
            closures.settle(
                company,
                closed,
                new ClaimClosureService.Settlement(
                    "SETTLED", new BigDecimal("25000"), BrokerClaimFixtures.today(), null)));
    ClaimExperienceQueryService.ClaimExperience summary = experience.summary(spec.arn(), 2026);
    assertThat(summary.claimCount()).isEqualTo(2);
    assertThat(summary.openCount()).isEqualTo(1);
    assertThat(summary.paid()).isEqualByComparingTo("85000");
    assertThat(summary.outstanding()).isEqualByComparingTo("40000");
    assertThat(summary.total()).isEqualByComparingTo("125000");
    assertThat(summary.withClaim()).isTrue();
    assertThat(experience.summary("ARN-NONE-" + BrokerClaimFixtures.unique(), null).withClaim())
        .isFalse();
  }
}
