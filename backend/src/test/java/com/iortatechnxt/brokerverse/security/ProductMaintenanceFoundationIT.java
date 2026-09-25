package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowDefinitions;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Product Maintenance foundation (V755, V998): roles and grants of the demo personas, workflow
 * PM_PACKAGE_REQUEST, lists of values, parameters and the job cron.
 */
@IntegrationTest
class ProductMaintenanceFoundationIT {

  private static final String WORKFLOW = "PM_PACKAGE_REQUEST";
  private static final String TYPE = "PmFoundationTestRequest";
  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000);

  @Autowired private UserDetailsService users;
  @Autowired private WorkflowDefinitions definitions;
  @Autowired private WorkflowService workflow;
  @Autowired private LovService lovs;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Environment environment;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private Set<String> authorities(String user) {
    return users.loadUserByUsername(user).getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  @Test
  void demoPersonasHoldTheirProductMaintenancePermissions() {
    assertThat(authorities("ao")).contains("PKG_REQUEST", "PRODUCT_VIEW");
    assertThat(authorities("mkttl")).contains("PKG_REQUEST_APPROVE", "PKG_REPORT_VIEW");
    assertThat(authorities("tsu")).contains("PKG_NEGOTIATE", "PKG_ADVISORY");
    assertThat(authorities("tsulead"))
        .contains("PKG_TSU_RECOMMEND", "PKG_QS_APPROVE", "TSU_APPROVE")
        .doesNotContain("PKG_TSU_APPROVE");
    assertThat(authorities("tsuhead"))
        .contains("PKG_TSU_APPROVE", "PRODUCT_VALIDATE", "PRODUCT_VIEW")
        .doesNotContain("PRODUCT_MAINTAIN");
    assertThat(authorities("mbs"))
        .contains("PRODUCT_MAINTAIN", "INCENTIVE_CRITERIA_MAINTAIN", "PRODUCT_ARCHIVE_VIEW")
        .doesNotContain("PRODUCT_VALIDATE");
    assertThat(authorities("mbs2")).contains("PRODUCT_MAINTAIN");
    assertThat(authorities("mancom")).contains("PKG_MANCOM_SIGNOFF", "PRODUCT_VIEW");
    assertThat(authorities("badmin")).contains("PRODUCT_VALIDATE", "PRODUCT_AUTHORIZE");
  }

  @Test
  void everyProductMaintenancePermissionIsGrantedAndClassified() {
    Set<String> granted =
        Set.copyOf(
            jdbc.queryForList("select distinct permission from sec_role_permission", String.class));
    Set<String> classified =
        Set.copyOf(jdbc.queryForList("select permission from sec_permission_action", String.class));
    Set<String> known =
        Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
    List<String> pm =
        Arrays.stream(Permission.values())
            .map(Enum::name)
            .filter(
                p ->
                    p.startsWith("PKG_")
                        || p.startsWith("PRODUCT_")
                        || p.startsWith("INCENTIVE_CRITERIA"))
            .toList();
    assertThat(pm).hasSize(15);
    assertThat(granted).containsAll(pm);
    assertThat(classified).containsAll(pm);
    assertThat(known).containsAll(classified);
  }

  @Test
  void workflowIsSeededWithOwnersAndValidTransitions() {
    assertThat(definitions.initialStage(WORKFLOW).getStageCode()).isEqualTo("DRAFT");
    assertThat(definitions.stageNames(WORKFLOW))
        .containsKeys(
            "FOR_MKT_APPROVAL",
            "NEGOTIATION",
            "FOR_MANCOM",
            "WITH_MBS",
            "FOR_VALIDATION",
            "RELEASED",
            "RETIRED",
            "NOT_PROCEEDED",
            "VOIDED");
    assertThat(definitions.stage(WORKFLOW, "FOR_MANCOM").getOwnerPermission())
        .isEqualTo("PKG_MANCOM_SIGNOFF");
    Set<String> known =
        Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
    List<String> permissions =
        jdbc.queryForList(
            "select permission from wf_transition where workflow_code = ?", String.class, WORKFLOW);
    assertThat(permissions).hasSizeGreaterThan(20);
    permissions.forEach(p -> assertThat(known).containsAll(Arrays.asList(p.split(","))));
  }

  @Test
  void requestCaseMovesThroughTheApprovalChain() {
    String id = String.valueOf(IDS.incrementAndGet());
    WorkCase c =
        as.run(
            "ao",
            () ->
                workflow.start(
                    new StartCase(
                        data.company().getId(),
                        WORKFLOW,
                        new CaseRecord(TYPE, id, "PKR-TEST-" + id, "Motor package", null, null),
                        null)));
    assertThat(c.getStageCode()).isEqualTo("DRAFT");
    as.run("ao", () -> workflow.transition(TYPE, id, "submit", TransitionNote.NONE));
    assertThatThrownBy(
            () -> as.run("ao", () -> workflow.transition(TYPE, id, "approve", TransitionNote.NONE)))
        .extracting("code")
        .isEqualTo("WORKFLOW_ACTION_NOT_PERMITTED");
    as.run("mkttl", () -> workflow.transition(TYPE, id, "approve", TransitionNote.NONE));
    as.run("tsulead", () -> workflow.transition(TYPE, id, "recommend", TransitionNote.NONE));
    as.run("tsuhead", () -> workflow.transition(TYPE, id, "approve", TransitionNote.NONE));
    as.run("tsu", () -> workflow.transition(TYPE, id, "revise_qs", TransitionNote.NONE));
    WorkCase terms =
        as.run("tsu", () -> workflow.transition(TYPE, id, "terms_final", TransitionNote.NONE));
    assertThat(terms.getStageCode()).isEqualTo("TERMS_REVIEW");
    as.run(
        "tsuhead",
        () -> workflow.transition(TYPE, id, "skip_marketing_review", TransitionNote.NONE));
    as.run("tsu", () -> workflow.transition(TYPE, id, "submit_requirements", TransitionNote.NONE));
    as.run("mancom", () -> workflow.transition(TYPE, id, "signoff", TransitionNote.NONE));
    as.run("mbs", () -> workflow.transition(TYPE, id, "setup", TransitionNote.NONE));
    WorkCase released =
        as.run(
            "mbs",
            () -> workflow.systemTransition(TYPE, id, "version_released", TransitionNote.NONE));
    assertThat(released.getStageCode()).isEqualTo("RELEASED");
    assertThat(released.isClosed()).isTrue();
  }

  @Test
  void listsParametersAndJobCronAreConfigured() {
    LocalDate today = LocalDate.of(2026, 9, 1);
    assertThat(lovs.activeValues("PKG_RESPONSE_OUTCOME", today))
        .extracting(LovValue::getCode)
        .contains("APPROVED_WITH_CHANGES", "COUNTER_PROPOSAL", "DECLINED");
    assertThat(lovs.activeValues("PKG_REQUEST_TYPE", today))
        .extracting(LovValue::getCode)
        .containsExactly("NEW", "AMEND", "UPDATE", "RENEW", "RETIRE", "REACTIVATE");
    for (String type :
        List.of(
            "PKG_REQUEST_REASON",
            "PKG_ADVISORY_GROUP",
            "PKG_NOT_PROCEEDED_REASON",
            "INCENTIVE_TYPE",
            "COVERAGE_KIND",
            "CLAUSE_KIND")) {
      assertThat(lovs.activeValues(type, today)).as(type).isNotEmpty();
    }
    assertThat(parameters.intValue("PACKAGE_EXPIRY_NOTICE_DAYS", 0)).isEqualTo(60);
    assertThat(parameters.intValue("PKG_SLA_NEGOTIATION", 0)).isEqualTo(120);
    assertThat(parameters.text("PKG_REQUEST_PREFIX", "")).isEqualTo("PKR-");
    assertThat(environment.getProperty("brokerverse.jobs.package-expiry-cron"))
        .isEqualTo("0 0 17 * * *");
  }
}
