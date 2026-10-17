package com.iortatechnxt.brokerverse.brokerclaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimExtrasService;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.ClaimsHomeService;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistExport;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.WorklistCriteria;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.WorklistRow;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.LossAdviceService;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.HandlerSetupService;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandler;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.FollowUp;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService.StatusOption;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.DocumentChecklist;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.FollowUpRecorder;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Claims, BRD-scope delivery work of the FRS BRD-07 Claims v1.0 (DW-CLM-01 to 13). Every claim is
 * invented by the fixtures.
 */
@IntegrationTest
class ClaimsBrdScopeIT {

  private static final String OFFICER = "clmofficer";
  private static final String TL = "clmtl";
  private static final String TH = "clmth";
  private static final String UH = "clmuh";

  @Autowired private BrokerClaimFixtures fixtures;
  @Autowired private ClaimsFixtures documents;
  @Autowired private HandlerSetupService handlers;
  @Autowired private ClaimStatusService statuses;
  @Autowired private FollowUpRecorder followUps;
  @Autowired private DocumentChecklist checklist;
  @Autowired private ClaimExtrasService extras;
  @Autowired private LossAdviceService lossAdvice;
  @Autowired private WorklistQuery worklist;
  @Autowired private WorklistExport export;
  @Autowired private ClaimsHomeService home;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private Long claim(String status) {
    return fixtures.recorded(
        fixtures.spec(OFFICER, BrokerClaimFixtures.today().minusDays(40)), status);
  }

  @Test
  void handlersHaveTheirSupervisingTeamLeadAndTeamHead() {
    ClaimHandler h =
        as.run(
            UH,
            () ->
                handlers.save(
                    OFFICER,
                    "MOTOR_HO",
                    "Motor 1",
                    new HandlerSetupService.Supervisors(TL, TH),
                    true));
    assertThat(h.getTeamLead()).isEqualTo(TL);
    assertThat(h.getTeamHead()).isEqualTo(TH);
    assertThatThrownBy(
            () ->
                as.run(
                    UH,
                    () ->
                        handlers.save(
                            OFFICER,
                            "MOTOR_HO",
                            "Motor 1",
                            new HandlerSetupService.Supervisors(OFFICER, null),
                            true)))
        .extracting("code")
        .isEqualTo("BCL_SUPERVISOR_INVALID");
  }

  @Test
  void marketingReadsTheClaimRecordWithoutChangingIt() {
    List<String> held =
        jdbc.queryForList(
            "select p.permission from sec_role_permission p join sec_role r on r.id = p.role_id"
                + " where r.code = 'BDOI_MKT_OFFICER'",
            String.class);
    assertThat(held).contains("BCL_VIEW").doesNotContain("BCL_RECORD", "BCL_STATUS_UPDATE");
  }

  @Test
  void statusesComeInAlphabeticalOrderWithTheirStatusType() {
    Long id = claim("NEW_COMPLETE_DOCS");
    List<StatusOption> options = as.run(TL, () -> statuses.allowedStatuses(fixtures.company(), id));
    List<String> labels = options.stream().map(StatusOption::label).toList();
    assertThat(labels).isNotEmpty().isSortedAccordingTo(String::compareTo);
    assertThat(options).extracting(StatusOption::statusType).doesNotContainNull();
    assertThat(
            jdbc.queryForObject(
                "select value from bcl_lov_attribute where type_code = 'BCL_CLAIM_STATUS'"
                    + " and code = 'INSURER_LOA_ISSUANCE' and attribute = 'status_type'",
                String.class))
        .isEqualTo("SETTLEMENT_LOA");
    assertThat(
            jdbc.queryForList(
                "select code from lov_value where type_code = 'BCL_CLAIM_TYPE'", String.class))
        .contains("CG", "FL", "HP", "LC", "LF", "MC", "MR", "OT");
  }

  @Test
  void followUpsAreRecordedCountedAndShownOnTheWorklist() {
    Long id = claim("NEW_COMPLETE_DOCS");
    Long company = fixtures.company();
    FollowUp f =
        as.run(
            TL,
            () ->
                followUps.record(
                    company,
                    id,
                    new FollowUpRecorder.Request(
                        new FollowUp.Entry(
                            "APPOINT_ASSESSOR",
                            "Called the adjuster",
                            "Wait for the evaluation",
                            null),
                        "ADJUSTER_REVIEW",
                        new FollowUp.Amounts(
                            null, new BigDecimal("42000.00"), new BigDecimal("2000.00")))));
    assertThat(f.getStatusCode()).isEqualTo("ADJUSTER_REVIEW");
    assertThat(fixtures.column(id, "status_code", String.class)).isEqualTo("ADJUSTER_REVIEW");
    assertThat(fixtures.column(id, "next_action_plan", String.class))
        .isEqualTo("Wait for the evaluation");
    assertThat(as.run(OFFICER, () -> followUps.of(company, id))).hasSize(1);
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        followUps.record(
                            company,
                            id,
                            new FollowUpRecorder.Request(
                                new FollowUp.Entry("APPOINT_ASSESSOR", " ", null, null),
                                null,
                                null))))
        .extracting("code")
        .isEqualTo("BCL_ACTION_TAKEN_REQUIRED");

    jdbc.update("update bcl_claim set plate_no = 'ABC 1234' where id = ?", id);
    String claimNo = fixtures.column(id, "claim_no", String.class);
    WorklistRow row =
        as.run(
                OFFICER,
                () ->
                    worklist.search(
                        company,
                        new WorklistCriteria(
                            WorklistQuery.Tab.ALL,
                            null,
                            null,
                            claimNo,
                            WorklistQuery.Sort.OVERALL_AGE),
                        0,
                        20))
            .content()
            .get(0);
    assertThat(row.followUps().count()).isEqualTo(1);
    assertThat(row.followUps().lastDate()).isEqualTo(BrokerClaimFixtures.today());
    assertThat(row.followUps().daysSinceLast()).isZero();
    assertThat(row.followUps().plateNo()).isEqualTo("ABC 1234");

    WorklistCriteria criteria = new WorklistCriteria(WorklistQuery.Tab.ALL, null, null, "ABC 1234");
    assertThat(as.run(OFFICER, () -> export.rows(company, criteria)))
        .extracting(WorklistRow::claimNo)
        .contains(claimNo);
    byte[] pdf = as.run(OFFICER, () -> export.pdf(company, criteria));
    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    assertThat(as.run(OFFICER, () -> export.excel(company, criteria))).isNotEmpty();
  }

  @Test
  void aClaimIsFiledWithCompleteDocumentsOnlyWithTheRequiredDocuments() {
    Long company = fixtures.company();
    as.run(UH, () -> checklist.save("MOTOR_OWN_DAMAGE", "CLAIM_POLICE_REPORT", true, true));
    try {
      Long id = claim("NEW_INCOMPLETE_DOCS");
      assertThatThrownBy(
              () -> as.run(OFFICER, () -> statuses.change(company, id, "NEW_COMPLETE_DOCS", null)))
          .extracting("code")
          .isEqualTo("BCL_DOCUMENTS_MISSING");
      documents.attach("BrokerClaim", id, "CLAIM_POLICE_REPORT");
      assertThat(checklist.of(fixtures.load(id)))
          .filteredOn(i -> i.documentType().equals("CLAIM_POLICE_REPORT"))
          .singleElement()
          .satisfies(i -> assertThat(i.uploaded()).isTrue());
      as.run(OFFICER, () -> statuses.change(company, id, "NEW_COMPLETE_DOCS", null));
      assertThat(fixtures.column(id, "status_code", String.class)).isEqualTo("NEW_COMPLETE_DOCS");
    } finally {
      as.run(UH, () -> checklist.save("MOTOR_OWN_DAMAGE", "CLAIM_POLICE_REPORT", true, false));
    }
  }

  @Test
  void motorDetailsReturnToTheAccountOfficerAndTheLossAdvicePreview() {
    Long company = fixtures.company();
    Long id = claim("NEW_COMPLETE_DOCS");
    as.run(OFFICER, () -> extras.motor(company, id, "XYZ 987", "G-0001"));
    assertThat(fixtures.column(id, "preferred_shop_code", String.class)).isEqualTo("G-0001");
    assertThatThrownBy(() -> as.run(OFFICER, () -> extras.motor(company, id, null, "INS-MGIC")))
        .extracting("code")
        .isEqualTo("BCL_SHOP_INVALID");
    assertThat(extras.repairShops(company)).extracting(p -> p.getCode()).contains("G-0001");

    assertThatThrownBy(
            () -> as.run(OFFICER, () -> extras.returnToAccountOfficer(company, id, null)))
        .extracting("code")
        .isEqualTo("BCL_RETURN_NOT_UNPAID");
    jdbc.update("update bcl_claim set premium_status = 'UNPAID' where id = ?", id);
    as.run(OFFICER, () -> extras.returnToAccountOfficer(company, id, "Please collect"));
    assertThat(fixtures.column(id, "returned_to_ao_by", String.class)).isEqualTo(OFFICER);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = 'ao' and entity_id = ?",
                Integer.class,
                id.toString()))
        .isPositive();

    fixtures.insurerLine(id, "INS-MGIC", new BigDecimal("100"), null, BigDecimal.ZERO, null);
    byte[] pdf = as.run(OFFICER, () -> lossAdvice.preview(fixtures.load(id), "INS-MGIC"));
    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    assertThatThrownBy(() -> as.run(OFFICER, () -> lossAdvice.preview(fixtures.load(id), "INS-X")))
        .extracting("code")
        .isEqualTo("BCL_LOSS_ADVICE_INSURER");
  }

  @Test
  void theHomeTrendAndTheReportsPresentation() {
    Long company = fixtures.company();
    claim("NEW_COMPLETE_DOCS");
    ClaimsHomeService.Home h = as.run(OFFICER, () -> home.home(company));
    assertThat(h.trend()).hasSize(6);
    assertThat(h.trend().get(5).asOf()).isEqualTo(BrokerClaimFixtures.today());
    assertThat(h.trend().get(5).over30()).isPositive();

    Map<String, String> p = new HashMap<>();
    p.put("companyId", String.valueOf(company));
    p.put("presentation", "SUMMARY");
    p.put("groupBy", "STATUS");
    p.put("amountFrom", "1");
    p.put("reportTitle", "Aging of Outstanding Claims by Status");
    ReportResult summary = as.run(UH, () -> reports.run("BCL-OUTSTANDING", p));
    assertThat(summary.title()).isEqualTo("Aging of Outstanding Claims by Status");
    assertThat(summary.columns()).extracting(c -> c.key()).startsWith("status", "claims");
    assertThat(summary.parameterEcho().get(0)).endsWith("Claims Unit");

    Long located = claim("NEW_COMPLETE_DOCS");
    fixtures.location(located, 1, "KEY-" + BrokerClaimFixtures.unique(), "Makati");
    jdbc.update("update bcl_claim_location set barangay = 'Poblacion' where claim_id = ?", located);
    Map<String, String> prone = new HashMap<>();
    prone.put("companyId", String.valueOf(company));
    prone.put("cityNames", "makati, Pasig");
    prone.put("barangayNames", "Poblacion");
    ReportResult byPlace = as.run(UH, () -> reports.run("BCL-PRONE-LOCATIONS", prone));
    assertThat(byPlace.rows())
        .anySatisfy(r -> assertThat(r.cells().get("barangay")).isEqualTo("Poblacion"));
  }
}
