package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowDefinitions;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Foundations of Sanction Screening (BRD-10, V1050) and User Access Maintenance (BRD-11, V1060):
 * permissions granted and classified, roles, workflow SCR_CASE, lists of values, parameters with
 * today's behaviour as the default, alerts, notification events, retention rules and job crons.
 */
@IntegrationTest
class ScreeningUserAccessFoundationIT {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);
  private static final String SCR_CASE = "SCR_CASE";

  @Autowired private UserDetailsService users;
  @Autowired private WorkflowDefinitions definitions;
  @Autowired private LovService lovs;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Environment environment;

  private Set<String> authorities(String user) {
    return users.loadUserByUsername(user).getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  private List<String> rolePermissions(String role) {
    return jdbc.queryForList(
        "select p.permission from sec_role_permission p join sec_role r on r.id = p.role_id"
            + " where r.code = ?",
        String.class,
        role);
  }

  private static List<String> permissionsStartingWith(String prefix) {
    return Arrays.stream(Permission.values())
        .map(Enum::name)
        .filter(p -> p.startsWith(prefix))
        .toList();
  }

  @Test
  void everyNewPermissionIsGrantedAndClassified() {
    Set<String> granted =
        Set.copyOf(
            jdbc.queryForList("select distinct permission from sec_role_permission", String.class));
    List<String> screening = permissionsStartingWith("SCR_");
    List<String> userAccess = permissionsStartingWith("UAM_");
    assertThat(screening).hasSize(14);
    assertThat(userAccess).hasSize(10);
    assertThat(granted).containsAll(screening).containsAll(userAccess);
    assertThat(
            jdbc.queryForList(
                "select distinct permission from sec_permission_action where area = 'SCREENING'",
                String.class))
        .containsExactlyInAnyOrderElementsOf(screening);
    assertThat(
            jdbc.queryForList(
                "select distinct permission from sec_permission_action where area = 'USER_ACCESS'",
                String.class))
        .containsAll(userAccess)
        .contains("ACCESS_REQUEST", "ACCESS_APPROVE", "USER_MANAGE", "ROLE_MANAGE");
    assertThat(
            jdbc.queryForList(
                "select distinct permission from sec_permission_action where area = 'ADMINISTRATION'",
                String.class))
        .contains("AUDIT_VIEW", "SYSTEM_PARAMETER_MANAGE", "ALERT_MANAGE");
  }

  @Test
  void rolesCarryTheDesignedPermissions() {
    assertThat(rolePermissions("COMPLIANCE_OFFICER"))
        .contains("SCR_CONFIG_MAINTAIN", "SCR_LIST_MAINTAIN", "SCR_STR_EXTRACT", "SCR_AUDIT_VIEW")
        .doesNotContain("SCR_CONFIG_APPROVE", "SCR_LIST_APPROVE");
    assertThat(rolePermissions("COMPLIANCE_CHECKER"))
        .contains("SCR_CONFIG_APPROVE", "SCR_LIST_APPROVE")
        .doesNotContain("SCR_CONFIG_MAINTAIN");
    assertThat(rolePermissions("SCR_INVESTIGATOR")).contains("SCR_INVESTIGATE", "SCR_RISK_TAG");
    assertThat(rolePermissions("SCR_APPROVER")).contains("SCR_CASE_APPROVE", "SCR_CASE_ASSIGN");
    assertThat(rolePermissions("AML_COMMITTEE")).contains("SCR_COMMITTEE");
    assertThat(rolePermissions("UNIT_COMPLIANCE_COORD")).contains("SCR_CASE_ASSIGN");
    assertThat(rolePermissions("UAM_REQUESTOR"))
        .containsExactlyInAnyOrder(
            "UAM_ENROLL",
            "UAM_MODIFY",
            "UAM_DEACTIVATE",
            "UAM_REACTIVATE",
            "UAM_CORRECT",
            "UAM_CANCEL",
            "UAM_VIEW",
            // V1062 (U1-A): bulk request files go through the bulk upload framework (BRD 1.009).
            "BULK_PROCESS");
    assertThat(rolePermissions("UAM_APPROVER")).contains("ACCESS_APPROVE", "UAM_REPORT_VIEW");
    assertThat(rolePermissions("UAM_SECOND_APPROVER")).contains("UAM_SECOND_APPROVE");
    assertThat(authorities("badmin"))
        .contains("ACCESS_REQUEST", "UAM_ENROLL", "UAM_GROUP_REQUEST", "UAM_REPORT_VIEW");
    assertThat(authorities("approver")).contains("ACCESS_APPROVE", "UAM_VIEW");
    assertThat(authorities("auditor")).contains("SCR_AUDIT_VIEW", "UAM_REPORT_VIEW");
    assertThat(authorities("admin")).contains("SCR_VIEW", "UAM_VIEW", "ROLE_MANAGE");
    assertThat(authorities("ao")).doesNotContain("SCR_VIEW", "UAM_VIEW");
  }

  @Test
  void screeningCaseWorkflowUsesKnownPermissionsAndReasons() {
    assertThat(definitions.initialStage(SCR_CASE).getStageCode()).isEqualTo("NEW");
    Set<String> known =
        Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
    jdbc.queryForList(
            "select permission from wf_transition where workflow_code = ?", String.class, SCR_CASE)
        .forEach(p -> assertThat(known).containsAll(Arrays.asList(p.split(","))));
    assertThat(
            jdbc.queryForList(
                "select owner_permission from wf_stage where workflow_code = ?"
                    + " and owner_permission is not null",
                String.class,
                SCR_CASE))
        .isSubsetOf(known)
        .contains("SCR_INVESTIGATE", "SCR_CASE_APPROVE", "SCR_COMMITTEE", "SCR_STR_EXTRACT");
    assertThat(
            jdbc.queryForList(
                "select stage_code from wf_stage where workflow_code = ? and terminal",
                String.class,
                SCR_CASE))
        .containsExactly("CLOSED");
    assertThat(
            jdbc.queryForList(
                "select action from wf_transition where workflow_code = ?"
                    + " and reason_lov = 'RETURN_REASON'",
                String.class,
                SCR_CASE))
        .containsExactlyInAnyOrder("disapprove", "return_for_rework", "reopen");
  }

  @Test
  void listsAreSeededWithDispositionsPerStage() {
    for (String type :
        List.of(
            "SCR_DISPOSITION",
            "SCR_CASE_TYPE",
            "SCR_LIST_TYPE",
            "SCR_REASSIGN_REASON",
            "SCR_FORM_TYPE",
            "SCR_DOCUMENT_TYPE",
            "UAM_DEACTIVATION_REASON")) {
      assertThat(lovs.activeValues(type, TODAY)).as(type).isNotEmpty();
    }
    assertThat(lovs.activeValues("SCR_DISPOSITION", TODAY))
        .filteredOn(v -> "INVESTIGATION".equals(v.getParentCode()))
        .extracting(LovValue::getCode)
        .containsExactly(
            "FALSE_POSITIVE", "TRUE_MATCH_REVIEW", "POSSIBLE_MATCH_EDD", "NEED_MORE_INFO");
    // SQ09: no AMLC reason code yet; the seed profile seeds placeholder codes RSN01-03 (V1952).
    assertThat(lovs.activeValues("SCR_STR_REASON", TODAY))
        .extracting(LovValue::getCode)
        .allMatch(code -> code.startsWith("RSN"));
    assertThat(lovs.activeValues("UAM_BUSINESS_UNIT", TODAY)).isEmpty();
  }

  @Test
  void parametersKeepTodaysBehaviourUntilAnswered() {
    assertThat(parameters.text("SCR_BLOCK_ON_OPEN_MATCH", "")).isEqualTo("false");
    assertThat(parameters.text("SCR_COMMITTEE_RULE", "")).isEqualTo("MAJORITY");
    assertThat(parameters.intValue("SCR_COMMITTEE_SIZE", 0)).isEqualTo(5);
    assertThat(parameters.items("SCR_SCREENING_SCOPE")).containsExactly("PROSPECT", "CONFIRMED");
    assertThat(parameters.text("AUTH_MODE", "")).isEqualTo("LOCAL");
    // U1-A (V1062) closed both switches once the implement-request flow was live.
    assertThat(parameters.text("UAM_DIRECT_ROLE_EDIT", "")).isEqualTo("false");
    assertThat(parameters.text("UAM_ROLE_APPLY_ON_APPROVAL", "")).isEqualTo("false");
    assertThat(parameters.text("UAM_ANY_APPROVER", "")).isEqualTo("false");
    assertThat(parameters.intValue("PASSWORD_HISTORY_COUNT", 0)).isEqualTo(8);
    assertThat("a123456789").matches(parameters.text("USER_ID_PATTERN", ""));
    assertThat(parameters.get("LOGIN_MAX_FAILED_ATTEMPTS").getDescription()).contains("all users");
  }

  @Test
  void alertsEventsRetentionAndCronsAreConfigured() {
    assertThat(
            jdbc.queryForList(
                "select code from alt_exception_code where module in ('SCREENING', 'USER_ACCESS')",
                String.class))
        .contains("SCR_INGEST_FAILED", "SCR_SLA_BREACH", "UAM_DIRECT_ROLE_EDIT")
        .hasSize(7);
    assertThat(
            jdbc.queryForList(
                "select code from msg_notification_event where module in ('SCREENING',"
                    + " 'USER_ACCESS')",
                String.class))
        .contains("SCR_CASE_ASSIGNED", "SCR_NO_POLICY_HIT", "UAM_REQUEST_TO_APPROVE")
        // + PASSWORD_EXPIRY_NOTICE (U1-B, V1063)
        .hasSize(18);
    assertThat(
            jdbc.queryForList(
                "select record_type from nba_retention_rule where years_online = 5"
                    + " and years_archive = 5",
                String.class))
        .contains("SCREENING_CASE", "WATCHLIST_ENTRY");
    assertThat(environment.getProperty("brokerverse.jobs.scr-watchlist-ingest-cron"))
        .isEqualTo("0 0 17 * * *");
    assertThat(environment.getProperty("brokerverse.jobs.scr-ingest-error-digest-cron"))
        .isEqualTo("0 0 23 * * SUN-THU");
    assertThat(environment.getProperty("brokerverse.jobs.uam-effective-changes-cron"))
        .isEqualTo("0 5 16 * * *");
    ReportMetadata compliance = ReportMetadata.compliance("SCR-TEST", "Test", "Test", List.of());
    assertThat(compliance.category()).isEqualTo(ReportCategory.COMPLIANCE);
    assertThat(compliance.category().label()).isEqualTo("Compliance");
  }
}
