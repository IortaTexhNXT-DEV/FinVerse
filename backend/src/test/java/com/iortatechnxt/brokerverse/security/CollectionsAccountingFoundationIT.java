package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Foundations of Collections (BRD-4, V1000) and Accounting / Disbursement / ACSL (BRD-5, V890,
 * V999): permissions granted and classified, seed personas, workflows, lists of values with their
 * attributes, event types with seed rules, parameters, alerts, feeds and job crons.
 */
@IntegrationTest
class CollectionsAccountingFoundationIT {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

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

  private static List<String> permissionsStartingWith(String... prefixes) {
    return Arrays.stream(Permission.values())
        .map(Enum::name)
        .filter(p -> Arrays.stream(prefixes).anyMatch(p::startsWith))
        .toList();
  }

  @Test
  void everyNewPermissionIsGrantedAndTheBrokingOnesAreClassified() {
    Set<String> granted =
        Set.copyOf(
            jdbc.queryForList("select distinct permission from sec_role_permission", String.class));
    Set<String> classified =
        Set.copyOf(jdbc.queryForList("select permission from sec_permission_action", String.class));
    List<String> collections = permissionsStartingWith("CLX_");
    List<String> accounting =
        permissionsStartingWith(
            "DISB_",
            "PRQ_",
            "ACSL_",
            "SERVICE_FEE_",
            "FRBS_",
            "JOURNAL_ASSIGN",
            "COA_UPLOAD",
            "GL_CLOSE",
            "REVALUATION_RATE",
            "REMIT_DEDUCTION",
            "EMPLOYEE_MAINTAIN");
    assertThat(collections).hasSize(12);
    assertThat(accounting).hasSize(42);
    assertThat(granted).containsAll(collections).containsAll(accounting);
    assertThat(classified)
        .containsAll(collections)
        .containsAll(permissionsStartingWith("DISB_", "PRQ_", "ACSL_"));
  }

  @Test
  void seedPersonasHoldTheirPermissions() {
    assertThat(authorities("ao")).contains("CLX_VIEW", "CLX_WORK", "CLX_EXPORT", "PRQ_CREATE");
    assertThat(authorities("mkttl")).contains("CLX_ESCALATION_HANDLE", "CLX_ASSIGN", "PRQ_REVIEW");
    assertThat(authorities("mktcoll"))
        .contains("CLX_BULK_UPDATE", "CLX_BILLING", "CLX_UNAPPLIED_WORK")
        .doesNotContain("CLX_SETUP");
    assertThat(authorities("proc")).contains("CLX_VIEW", "CLX_WORK").doesNotContain("CLX_EXPORT");
    assertThat(authorities("badmin")).contains("CLX_SETUP", "CLX_AUDIT_VIEW", "FLOWIN_MANAGE");
    assertThat(authorities("disb")).contains("DISB_PROCESS", "DISB_VIEW", "DISB_TAG", "CLX_VIEW");
    assertThat(authorities("disbtl"))
        .contains("DISB_REVIEW", "DISB_FUNDING_VERIFY", "DISB_EOD")
        .doesNotContain("DISB_APPROVE");
    assertThat(authorities("disbappr2")).contains("DISB_APPROVE", "DISB_FUNDING_APPROVE");
    assertThat(authorities("mktao")).contains("PRQ_CREATE").doesNotContain("PRQ_APPROVE");
    assertThat(authorities("mktrev")).contains("PRQ_REVIEW", "PRQ_ASSIGN");
    assertThat(authorities("mktappr")).contains("PRQ_APPROVE");
    assertThat(authorities("hrappr")).contains("PRQ_HR_APPROVE");
    assertThat(authorities("acsl")).contains("ACSL_PROCESS", "ACSL_UPLOAD", "CLX_VIEW");
    assertThat(authorities("acsltl")).contains("ACSL_REVIEW", "REMIT_DEDUCTION_CONFIRM");
    assertThat(authorities("acslhead")).contains("ACSL_APPROVE").doesNotContain("ACSL_PROCESS");
    assertThat(authorities("glofficer")).contains("JOURNAL_CREATE", "SERVICE_FEE_MANAGE");
    assertThat(authorities("gltl")).contains("JOURNAL_ASSIGN", "COA_UPLOAD", "GL_CLOSE_SCHEDULE");
    assertThat(authorities("glhead")).contains("REVALUATION_RATE_MAINTAIN", "MASTER_AUTHORIZE");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "CLX_ESCALATION",
        "DISB_VOUCHER",
        "DISB_STATUS_EDIT",
        "DISB_FUNDING",
        "DISB_PAYEE",
        "PRQ_REFUND",
        "PRQ_CASH_ADVANCE",
        "PRQ_CHECK_CANCEL",
        "ACSL_CASE",
        "ACSL_CORRECTION",
        "REM_DEDUCTION",
        "FRBS_SERVICE_FEE"
      })
  void workflowsHaveOneInitialStageAndKnownPermissions(String workflow) {
    assertThat(definitions.initialStage(workflow)).isNotNull();
    Set<String> known =
        Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
    List<String> permissions =
        jdbc.queryForList(
            "select permission from wf_transition where workflow_code = ?", String.class, workflow);
    assertThat(permissions).isNotEmpty();
    permissions.forEach(p -> assertThat(known).containsAll(Arrays.asList(p.split(","))));
    List<String> owners =
        jdbc.queryForList(
            "select owner_permission from wf_stage where workflow_code = ?"
                + " and owner_permission is not null",
            String.class,
            workflow);
    assertThat(known).containsAll(owners);
  }

  @Test
  void listsAndCollectionsAttributesAreSeeded() {
    for (String type :
        List.of(
            "CLX_PR_DISPOSITION",
            "CLX_UPP_DISPOSITION",
            "CLX_EFFORT_CODE",
            "CLX_BILLING_FREQUENCY",
            "CLX_ESCALATION_REASON",
            "PAYEE_CLASS",
            "DISBURSEMENT_TYPE",
            "DISB_CANCEL_REASON",
            "DISB_RETURN_REASON",
            "REFUND_REASON",
            "RRF_CATEGORY_A",
            "RRF_CATEGORY_B",
            "ACSL_CASE_TYPE",
            "ACSL_CORRECTION_KIND",
            "SERVICE_FEE_SEGMENT")) {
      assertThat(lovs.activeValues(type, TODAY)).as(type).isNotEmpty();
    }
    assertThat(lovs.activeValues("DISBURSEMENT_TYPE", TODAY))
        .extracting(LovValue::getCode)
        .contains("REMITTANCE", "REFUND", "SUPPLIER", "GOVERNMENT", "OTHER_BANK_UNIT");
    assertThat(
            jdbc.queryForObject(
                "select value from clx_lov_attribute where type_code = 'CLX_PR_DISPOSITION'"
                    + " and code = 'PR2307_FOR_REVERSAL' and attribute = 'ops_action'",
                String.class))
        .isEqualTo("CWT2307_REVERSAL");
    assertThat(
            jdbc.queryForObject(
                "select value from clx_lov_attribute where type_code = 'CLX_UPP_DISPOSITION'"
                    + " and code = 'FOR_APPLICATION_TO_INVOICE' and attribute = 'requires_invoice'",
                String.class))
        .isEqualTo("true");
  }

  @Test
  void everyNewEventTypeHasASeedRuleOnExistingAccounts() {
    List<String> events =
        List.of(
            "DISB_VOUCHER",
            "DISB_CHECK_NEGOTIATED",
            "DISB_CHECK_STALE",
            "DISB_FUND_TRANSFER",
            "TAX_CWT_CERT_RECEIVED",
            "OPS_REMIT_CPC2",
            "OPS_REMIT_DEDUCTION",
            "FRBS_SERVICE_FEE_ACCRUE",
            "PRQ_CA_LIQUIDATION");
    for (String event : events) {
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from acc_rule r join acc_rule_line l on l.rule_id = r.id"
                      + " where r.event_type = ?",
                  Integer.class,
                  event))
          .as(event)
          .isPositive();
    }
    assertThat(
            jdbc.queryForList(
                "select distinct l.account_code from acc_rule r"
                    + " join acc_rule_line l on l.rule_id = r.id"
                    + " where r.event_type in ("
                    + String.join(",", events.stream().map(e -> "'" + e + "'").toList())
                    + ") and l.account_code not like '@%'"
                    + " and not exists (select 1 from coa_account a where a.code = l.account_code"
                    + " and a.company_id = r.company_id)",
                String.class))
        .isEmpty();
    assertThat(
            jdbc.queryForObject(
                "select a.name from coa_account a join org_company c on c.id = a.company_id"
                    + " where c.code = 'FVI' and a.code = '2210'",
                String.class))
        .isEqualTo("Payable to Insurance Companies");
  }

  @Test
  void parametersAlertsFeedsAndCronsAreConfigured() {
    assertThat(parameters.intValue(SystemParameterService.LOGIN_MAX_FAILED_ATTEMPTS, 0))
        .isEqualTo(3);
    assertThat(parameters.intValue("CLX_EDIT_LOCK_MINUTES", 0)).isEqualTo(15);
    assertThat(parameters.intValue("DISB_STALE_DAYS", 0)).isEqualTo(180);
    assertThat(parameters.items("DISB_AUTO_APPROVER_ROUTING")).contains("REFUND", "REMITTANCE");
    assertThat(parameters.text("CLX_INVOICE_NO_PATTERN", "")).isEqualTo("^(I\\d{8}|BI-.+)$");
    assertThat("BI-HO-2026-000001").matches(parameters.text("CLX_INVOICE_NO_PATTERN", ""));
    assertThat("I12345678").matches(parameters.text("CLX_INVOICE_NO_PATTERN", ""));
    assertThat(
            jdbc.queryForList(
                "select code from alt_exception_code where code in ('CLX_REFRESH_FAILED',"
                    + " 'CLX_OUTBOX_STALE', 'DISB_UNREGULARIZED', 'ACSL_GLSL_DIFFERENCE')",
                String.class))
        .hasSize(4);
    assertThat(
            jdbc.queryForList(
                "select code from msg_notification_event where module = 'COLLECTIONS'",
                String.class))
        .contains("CLX_ESCALATED", "CLX_FILE_READY");
    assertThat(environment.getProperty("brokerverse.jobs.clx-daily-refresh-cron"))
        .isEqualTo("0 15 14 * * *");
    assertThat(environment.getProperty("brokerverse.jobs.clx-weekly-files-cron"))
        .isEqualTo("0 30 14 * * FRI");
    assertThat(environment.getProperty("brokerverse.jobs.disb-check-stale-cron"))
        .isEqualTo("0 20 16 * * *");
    assertThat(environment.getProperty("brokerverse.jobs.disb-eod-reports-cron")).isEqualTo("-");
    assertThat(environment.getProperty("brokerverse.jobs.broking-books-close-cron"))
        .isEqualTo("0 0 15 L * *");
  }
}
