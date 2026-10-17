package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.InsurerLine;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Scheme;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.productmaint.service.ManComRouting;
import com.iortatechnxt.brokerverse.productmaint.service.MarketingChain;
import com.iortatechnxt.brokerverse.productmaint.service.MasterTransfer;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequestService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequests;
import com.iortatechnxt.brokerverse.productmaint.service.RequestDetails;
import com.iortatechnxt.brokerverse.productmaint.service.RequestDetailsService;
import com.iortatechnxt.brokerverse.productmaint.service.RequestDraft;
import com.iortatechnxt.brokerverse.productmaint.service.RequirementsService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The routing of a package request in BDOI's FRS: the TSU Team Lead notice at TSU review
 * (FRPM.029.01), the Source with the TSU and Insurer routes and the Annex E checks (FRPM.011.02),
 * the three-level Marketing approval (setting), the ManCom approvers in parallel then the President
 * (FRPM.014.01), the Request for Deployment (FRPM.015.01), the notice to the original assignee of a
 * reassignment (FRPM.017.01) and the transfer of the product master changes to the simulated
 * receiving system (FRPM.029.01).
 */
@IntegrationTest
class PackageRoutingIT {

  private static final LocalDate START =
      BusinessClock.today(Clock.systemUTC()).plusMonths(2).withDayOfMonth(1);

  @Autowired private PackageRequestService requests;
  @Autowired private PackageRequests reader;
  @Autowired private RequestDetailsService details;
  @Autowired private RequirementsService requirements;
  @Autowired private ManComRouting mancom;
  @Autowired private MasterTransfer transfer;
  @Autowired private WorkAssignmentService assignments;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private Api api;

  private static RequestDraft draft(RequestType type, String product) {
    return new RequestDraft(
        type,
        RequestScope.GENERIC,
        "Routing package " + product,
        null,
        "MOTOR",
        "COMPREHENSIVE",
        product,
        null,
        List.of(),
        type == RequestType.NEW ? "NEW_PROGRAMME" : "PACKAGE_EXPIRY",
        null,
        null,
        new PackageTerms(
            List.of(new PackageTerms.Section("Target market", "Fleet owners")),
            List.of(),
            new Scheme(new BigDecimal("0.35"), new BigDecimal("15000"), BigDecimal.TEN, null, null),
            new Dates(START, START, START.plusYears(1).minusDays(1), null),
            List.of(InsurerLine.target("INS-MGIC"))));
  }

  private int notices(String user, String title) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from msg_notification where recipient = ? and title = ?",
            Integer.class,
            user,
            title);
    return n == null ? 0 : n;
  }

  private void set(String key, String value) {
    as.run("admin", () -> parameters.update(key, value));
  }

  private Long retireToManCom(String product) {
    PackageRequest p =
        as.run(
            "ao",
            () -> requests.create(data.company().getId(), draft(RequestType.RETIRE, product)));
    Long id = p.getId();
    as.run("ao", () -> requests.submit(id, null));
    as.run("mkttl", () -> requests.approve(id, null));
    as.run("tsulead", () -> requests.recommend(id, "Recommend the retirement"));
    as.run("tsuhead", () -> requests.approveTsu(id, null));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.FOR_MANCOM);
    return id;
  }

  @Test
  void theTsuTeamLeadIsToldWhenARequestEntersTsuReview() {
    PackageRequest p =
        as.run(
            "ao",
            () -> requests.create(data.company().getId(), draft(RequestType.RETIRE, "MTR23")));
    as.run("ao", () -> requests.submit(p.getId(), null));
    as.run("mkttl", () -> requests.approve(p.getId(), null));
    assertThat(reader.get(p.getId()).getStatus()).isEqualTo(RequestStage.FOR_TSU_REVIEW);
    assertThat(notices("tsulead", p.getRequestNo() + " is for TSU review")).isOne();
  }

  @Test
  void aTsuOrInsurerRequestGoesToTsuReviewAndAnnexEFieldsAreChecked() {
    RequestDetails tooSmall =
        new RequestDetails(
            "BANK_REFERRAL",
            "ao",
            null,
            "Fleet owners",
            10,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null);
    assertThatThrownBy(() -> details.check("TSU", tooSmall))
        .extracting("code")
        .isEqualTo("PKG_MIN_POLICIES");
    RequestDetails lowPremium =
        new RequestDetails(
            null,
            null,
            null,
            null,
            25,
            new BigDecimal("4000000"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null);
    assertThatThrownBy(() -> details.check("TSU", lowPremium))
        .extracting("code")
        .isEqualTo("PKG_MIN_PREMIUM");
    RequestDetails noIncentive =
        new RequestDetails(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            true, null, null);
    assertThatThrownBy(() -> details.check("TSU", noIncentive))
        .extracting("code")
        .isEqualTo("PKG_INCENTIVE_REQUIRED");
    assertThatThrownBy(() -> details.check("BROKER", null))
        .extracting("code")
        .isEqualTo("PKG_SOURCE");

    RequestDetails good =
        new RequestDetails(
            "INSURER_OFFER",
            "ao",
            "mkttl",
            "Fleet owners",
            25,
            new BigDecimal("6000000"),
            null,
            "Comprehensive motor",
            null,
            "Roadside assistance",
            "Standard clauses",
            null,
            null,
            null,
            true,
            null,
            new BigDecimal("2.5"));
    PackageRequest p =
        as.run("tsu", () -> requests.create(data.company().getId(), draft(RequestType.NEW, null)));
    as.run("tsu", () -> details.describe(p.getId(), "INSURER", good));
    assertThat(reader.get(p.getId()).isNegotiationRequired()).isFalse();
    as.run("tsu", () -> requests.submit(p.getId(), null));
    assertThat(reader.get(p.getId()).getStatus()).isEqualTo(RequestStage.FOR_TSU_REVIEW);
    assertThat(details.details(reader.get(p.getId())).estimatedPolicies()).isEqualTo(25);
  }

  @Test
  void theMarketingTeamLeaderTeamHeadAndUnitHeadApproveInTurn() throws Exception {
    set(MarketingChain.SETTING, MarketingChain.CHAIN);
    try {
      PackageRequest p =
          as.run(
              "ao",
              () -> requests.create(data.company().getId(), draft(RequestType.RETIRE, "MTR24")));
      Long id = p.getId();
      as.run("ao", () -> requests.submit(id, null));
      assertThatThrownBy(() -> as.run("mktuh", () -> requests.approve(id, null)))
          .extracting("code")
          .isEqualTo("PKG_MARKETING_LEVEL");
      as.run("mkttl", () -> requests.approve(id, "TL ok"));
      assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.FOR_MKT_APPROVAL);
      as.run("mktth", () -> requests.approve(id, "TH ok"));
      assertThat(notices("mktuh", p.getRequestNo() + " for Marketing Unit Head approval")).isOne();
      as.run("mktuh", () -> requests.approve(id, "UH ok"));
      assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.FOR_TSU_REVIEW);
      api.doGet("ao", "/api/v1/product-maintenance/requests/" + id + "/routing")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.marketing.chained").value(true))
          .andExpect(jsonPath("$.marketing.history.length()").value(3))
          .andExpect(jsonPath("$.marketing.history[2].approver").value("mktuh"));
    } finally {
      set(MarketingChain.SETTING, "ANY_ONE");
    }
  }

  @Test
  void theSelectedManComApproversApproveInParallelThenThePresident() throws Exception {
    Long id = retireToManCom("MTR25");
    String no = reader.get(id).getRequestNo();
    api.doPost("mancom", "/api/v1/product-maintenance/requests/" + id + "/signoff", Map.of())
        .andExpect(status().isUnprocessableEntity());
    as.run("tsu", () -> mancom.select(id, List.of("mancom", "mancompres")));
    assertThat(notices("mancom", no + " for your ManCom approval")).isOne();
    assertThat(notices("mancompres", no + " for your ManCom approval")).isZero();
    assertThatThrownBy(() -> as.run("mancompres", () -> mancom.decide(id, "APPROVE", null)))
        .extracting("code")
        .isEqualTo("PKG_MANCOM_NOT_YOURS");
    as.run("mancom", () -> mancom.decide(id, "APPROVE", "Agreed"));
    assertThat(notices("mancompres", no + " for your ManCom approval")).isOne();
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.FOR_MANCOM);
    as.run("mancompres", () -> mancom.decide(id, "APPROVE", "Approved"));
    PackageRequest done = reader.get(id);
    assertThat(done.getStatus()).isEqualTo(RequestStage.WITH_MBS);
    assertThat(done.getRouting().getDeploymentNo()).startsWith("PDR-");
    assertThat(notices("mbs", "Package deployment request " + done.getRouting().getDeploymentNo()))
        .isOne();
    assertThat(notices("ao", no + ": all approvals complete")).isOne();
  }

  @Test
  void aManComReturnNeedsRemarksAndSendsTheRequestBackToTsu() {
    Long id = retireToManCom("MTR26");
    as.run("tsu", () -> mancom.select(id, List.of("mancom")));
    assertThatThrownBy(() -> as.run("mancom", () -> mancom.decide(id, "RETURN", " ")))
        .extracting("code")
        .isEqualTo("PKG_MANCOM_REMARKS");
    as.run("mancom", () -> mancom.decide(id, "RETURN", "Attach the signed slip"));
    assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.REQUIREMENTS_PREP);
  }

  @Test
  void withTheManualSettingTheDeploymentIsRequestedByTheButton() throws Exception {
    set("PM_DEPLOYMENT_REQUEST", "MANUAL");
    set(ManComRouting.SETTING, "ANY_MEMBER");
    try {
      Long id = retireToManCom("MTR27");
      as.run("mancom", () -> requirements.signoff(id, "signed"));
      assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.MANCOM_APPROVED);
      assertThat(reader.get(id).getRouting().getDeploymentNo()).isNull();
      api.doPost(
              "tsu",
              "/api/v1/product-maintenance/requests/" + id + "/request-deployment",
              Map.of("text", "Deploy"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.deployment.mode").value("MANUAL"))
          .andExpect(
              jsonPath("$.deployment.number").value(org.hamcrest.Matchers.startsWith("PDR-")));
      assertThat(reader.get(id).getStatus()).isEqualTo(RequestStage.WITH_MBS);
    } finally {
      set("PM_DEPLOYMENT_REQUEST", "AUTO");
      set(ManComRouting.SETTING, ManComRouting.SELECTED);
    }
  }

  @Test
  void theOriginalAssigneeIsToldOfAReassignment() {
    PackageRequest p =
        as.run(
            "ao",
            () -> requests.create(data.company().getId(), draft(RequestType.RETIRE, "MTR28")));
    as.run("ao", () -> requests.submit(p.getId(), null));
    as.run("mkttl", () -> requests.approve(p.getId(), null));
    Long caseId =
        jdbc.queryForObject(
            "select id from wf_case where entity_type = 'PackageRequest' and entity_id = ?",
            Long.class,
            String.valueOf(p.getId()));
    as.run("tsuhead", () -> assignments.assign(caseId, "tsulead", List.of("tsulead", "tsu")));
    as.run("tsuhead", () -> assignments.assign(caseId, "tsu", List.of("tsulead", "tsu")));
    assertThat(notices("tsulead", p.getRequestNo() + " reassigned")).isOne();
  }

  @Test
  void theProductMasterChangesAreSentInOneFileToTheReceivingSystem() throws Exception {
    jdbc.update(
        "insert into pm_master_change (product_code, version_no, change_kind, effective_date,"
            + " status, attempts, created_at, created_by) values ('MTR10', 2, 'RELEASED', ?,"
            + " 'PENDING', 0, now(), 'SYSTEM')",
        java.sql.Date.valueOf(START));
    MasterTransfer.TransferRun run = transfer.transfer();
    assertThat(run.sent()).isPositive();
    assertThat(run.fileName()).matches("ProductMaster_\\d{14}\\.csv");
    String content =
        jdbc.queryForObject(
            "select content from pm_master_sim_inbox where file_name = ?",
            String.class,
            run.fileName());
    assertThat(content)
        .startsWith("H," + run.fileName())
        .contains("D,MTR10,", ",2,RELEASED," + START)
        .contains("T," + run.sent());
    api.doGet("mbs", "/api/v1/product-maintenance/master-changes?status=SENT")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(true))
        .andExpect(jsonPath("$.changes.content[0].fileName").value(run.fileName()));
    api.doGet("mbs", "/api/v1/product-maintenance/master-changes/simulator/files")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fileName").value(run.fileName()));
  }
}
