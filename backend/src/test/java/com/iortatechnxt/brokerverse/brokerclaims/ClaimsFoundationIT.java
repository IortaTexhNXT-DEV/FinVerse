package com.iortatechnxt.brokerverse.brokerclaims;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttribute;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttributeRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPremiumStatus;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimSource;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowDefinitions;
import java.math.BigDecimal;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Foundation of Claims Handling (BRD-7, wave CL0; V1020, V1021, V1920): permissions granted and
 * classified, roles and SIT/UAT users, workflow BCL_CLAIM, lists of values with their attributes,
 * the status access matrix, handler register, parameters, alerts, notification events, template,
 * retention rule, job crons, the report category and the claim mapping.
 */
@IntegrationTest
class ClaimsFoundationIT {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
  private static final String STATUS = ClaimCodes.LOV_STATUS;

  @Autowired private UserDetailsService users;
  @Autowired private WorkflowDefinitions definitions;
  @Autowired private LovService lovs;
  @Autowired private SystemParameterService parameters;
  @Autowired private ClaimLovAttributeRepository attributes;
  @Autowired private BrokerClaimRepository claims;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Environment environment;

  private Set<String> authorities(String user) {
    return users.loadUserByUsername(user).getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  private List<String> query(String sql, Object... args) {
    return jdbc.queryForList(sql, String.class, args);
  }

  @Test
  void everyClaimsPermissionIsGrantedAndClassified() {
    List<String> claimsPermissions =
        Arrays.stream(Permission.values())
            .map(Enum::name)
            .filter(p -> p.startsWith("BCL_"))
            .toList();
    assertThat(claimsPermissions).hasSize(18);
    assertThat(query("select distinct permission from sec_role_permission"))
        .containsAll(claimsPermissions);
    assertThat(query("select distinct permission from sec_permission_action where area = 'CLAIMS'"))
        .containsExactlyInAnyOrderElementsOf(claimsPermissions);
  }

  @Test
  void seedUsersCarryTheDesignedRights() {
    assertThat(authorities("clmofficer"))
        .contains("BCL_VIEW", "BCL_RECORD", "BCL_AUTHORIZE", "BCL_STATUS_UPDATE", "WORK_VIEW")
        .doesNotContain("BCL_CLOSE", "BCL_REOPEN", "BCL_CLAIMANT_OVERRIDE", "BCL_SETUP");
    assertThat(authorities("clmtl"))
        .contains("BCL_CLOSE", "BCL_RESERVE_AMEND", "WORK_ASSIGN")
        .doesNotContain("BCL_REOPEN", "BCL_SETUP");
    assertThat(authorities("clmth")).contains("BCL_CLOSE", "BCL_REOPEN");
    assertThat(authorities("clmuh"))
        .contains("BCL_SETUP", "BCL_REOPEN", "BCL_DATA_EXTRACT")
        .doesNotContain("BCL_RECORD", "BCL_STATUS_UPDATE");
    assertThat(authorities("clmrisk"))
        .contains("BCL_DATA_EXTRACT", "BCL_REPORT_EXPORT")
        .doesNotContain("BCL_RECORD");
    assertThat(authorities("ao")).contains("BCL_REPORT_VIEW").doesNotContain("BCL_VIEW");
    assertThat(authorities("mkttl")).contains("BCL_REPORT_EXPORT").doesNotContain("BCL_VIEW");
    assertThat(authorities("auditor")).contains("BCL_VIEW").doesNotContain("BCL_RECORD");
    assertThat(authorities("claims")).doesNotContain("BCL_VIEW");
    assertThat(query("select username from bcl_handler where unit_code = 'MOTOR_HO'"))
        .contains("clmofficer", "clmtl");
  }

  @Test
  void claimWorkflowMirrorsThePhases() {
    String workflow = ClaimCodes.WORKFLOW;
    assertThat(definitions.initialStage(workflow).getStageCode())
        .isEqualTo(ClaimPhase.NEW.stageCode());
    assertThat(query("select stage_code from wf_stage where workflow_code = ?", workflow))
        .containsExactlyInAnyOrder(
            Arrays.stream(ClaimPhase.values()).map(ClaimPhase::stageCode).toArray(String[]::new));
    assertThat(
            query("select stage_code from wf_stage where workflow_code = ? and terminal", workflow))
        .isEmpty();
    Set<String> known =
        Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
    assertThat(query("select permission from wf_transition where workflow_code = ?", workflow))
        .isSubsetOf(known);
    assertThat(
            query(
                "select action from wf_transition where workflow_code = ? and not generic",
                workflow))
        .containsOnly("progress", "temp_close", "resume", "close", "reopen");
    assertThat(
            query(
                "select action from wf_transition where workflow_code = ?"
                    + " and reason_lov = 'BCL_REOPEN_REASON'",
                workflow))
        .containsExactly("reopen");
  }

  @Test
  void listsAreSeededFromTheBrd() {
    assertThat(lovs.activeValues(STATUS, TODAY)).hasSize(18);
    assertThat(lovs.activeValues(ClaimCodes.LOV_SETTLEMENT_TYPE, TODAY)).hasSize(10);
    assertThat(lovs.activeValues(ClaimCodes.LOV_ADJUSTER, TODAY)).hasSize(25);
    assertThat(lovs.activeValues(ClaimCodes.LOV_CATASTROPHE, TODAY)).hasSize(7);
    assertThat(lovs.activeValues(ClaimCodes.LOV_UNIT, TODAY)).hasSize(7);
    for (String type :
        List.of(
            ClaimCodes.LOV_LOSS_NATURE,
            ClaimCodes.LOV_CLAIM_TYPE,
            ClaimCodes.LOV_UPDATE_SOURCE,
            ClaimCodes.LOV_DIARY_TYPE,
            ClaimCodes.LOV_DOCUMENT_TYPE,
            ClaimCodes.LOV_REOPEN_REASON,
            ClaimCodes.LOV_OVERRIDE_REASON)) {
      assertThat(lovs.activeValues(type, TODAY)).as(type).isNotEmpty();
    }
    assertThat(lovs.label("DOCUMENT_TYPE", "CLAIM_REPORT")).isEqualTo("Claims report");
    assertThat(
            query(
                "select distinct owner_permission from lov_type where code like 'BCL\\_%'"
                    + " escape '\\'"))
        .containsExactly("BCL_SETUP");
  }

  @Test
  void statusAndSettlementAttributesDriveTheLifecycle() {
    List<ClaimLovAttribute> phases =
        attributes.findByTypeCodeOrderByCodeAscAttributeAsc(STATUS).stream()
            .filter(a -> ClaimCodes.ATTR_PHASE.equals(a.getAttribute()))
            .toList();
    assertThat(phases).hasSize(18);
    assertThat(phases).allSatisfy(a -> ClaimPhase.valueOf(a.getValue()));
    assertThat(
            attributes.findByTypeCodeAndAttributeAndValueIgnoreCase(
                STATUS, ClaimCodes.ATTR_PHASE, "temp_closed"))
        .extracting(ClaimLovAttribute::getCode)
        .containsExactlyInAnyOrder("TEMP_CLOSED_NO_DOCS", "TEMP_CLOSED_WITH_OFFER");
    assertThat(
            attributes.findByTypeCodeAndAttributeAndValueIgnoreCase(
                STATUS, ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE, "true"))
        .extracting(ClaimLovAttribute::getCode)
        .containsExactly("BDOI_PREMIUM_REMITTANCE");
    assertThat(
            attributes
                .findByTypeCodeAndCodeAndAttribute(
                    ClaimCodes.LOV_SETTLEMENT_TYPE,
                    "SETTLED_LOA_ISSUED",
                    ClaimCodes.ATTR_CLOSES_CLAIM)
                .map(ClaimLovAttribute::isTrue))
        .contains(true);
    assertThat(
            attributes.findByTypeCodeAndAttributeAndValueIgnoreCase(
                ClaimCodes.LOV_SETTLEMENT_TYPE, ClaimCodes.ATTR_CLOSES_CLAIM, "false"))
        .hasSize(2);
  }

  @Test
  void statusAccessMatrixDefaultsToTheStakeholderTable() {
    String count =
        "select count(*) from bcl_status_access where role_code = ? and record_status = 'ACTIVE'";
    assertThat(jdbc.queryForObject(count, Integer.class, "CLM_OFFICER")).isEqualTo(4);
    assertThat(jdbc.queryForObject(count, Integer.class, "CLM_TL")).isEqualTo(18);
    assertThat(jdbc.queryForObject(count, Integer.class, "CLM_TH")).isEqualTo(18);
    assertThat(query("select status_code from bcl_status_access where role_code = 'CLM_OFFICER'"))
        .containsExactlyInAnyOrder(
            "NEW_COMPLETE_DOCS",
            "NEW_INCOMPLETE_DOCS",
            "TEMP_CLOSED_NO_DOCS",
            "TEMP_CLOSED_WITH_OFFER");
  }

  @Test
  void parametersAlertsEventsTemplateRetentionAndCronsAreConfigured() {
    assertThat(parameters.text(ClaimCodes.PARAM_DEFAULT_CURRENCY, "")).isEqualTo("PHP");
    assertThat(parameters.intValue(ClaimCodes.PARAM_FOLLOW_UP_DAYS, 0)).isEqualTo(7);
    assertThat(parameters.items(ClaimCodes.PARAM_AGEING_BUCKETS))
        .containsExactly("30", "60", "90", "180");
    assertThat(parameters.intValue(ClaimCodes.PARAM_PAST_DUE_DAYS, 0)).isEqualTo(90);
    assertThat(parameters.intValue(ClaimCodes.PARAM_PRONE_MIN_CLAIMS, 0)).isEqualTo(3);
    assertThat(parameters.intValue(ClaimCodes.PARAM_PRONE_YEARS, 0)).isEqualTo(3);
    assertThat(parameters.text(ClaimCodes.PARAM_AUTH_DP_POLICY, "")).isEqualTo("CONFIRM");
    assertThat(query("select code from alt_exception_code where module = ?", ClaimCodes.MODULE))
        .containsExactlyInAnyOrder(
            "BCL_UNPAID_PREMIUM_CLAIM",
            "BCL_FOLLOW_UP_OVERDUE",
            "BCL_CLAIM_PAST_DUE",
            "BCL_INSURER_CLAIM_NO_REUSED");
    assertThat(query("select code from msg_notification_event where module = ?", ClaimCodes.MODULE))
        .containsExactlyInAnyOrder(
            "BCL_FOLLOW_UP_DUE",
            "BCL_CLAIM_ASSIGNED",
            "BCL_STATUS_CHANGED",
            "BCL_PREMIUM_REMITTED",
            "BCL_NEWER_COVER_VERSION");
    assertThat(
            query("select body from doc_template where code = ?", ClaimCodes.LOSS_ADVICE_TEMPLATE))
        .singleElement()
        .asString()
        .contains("{{claimNo}}", "{{policyNo}}", "{{lossDate}}", "{{insurerClaimNos}}");
    assertThat(
            query(
                "select statuses from nba_retention_rule where record_type = ?"
                    + " and years_online = 10",
                ClaimCodes.RETENTION_RECORD_TYPE))
        .containsExactly(ClaimPhase.CLOSED.name());
    assertThat(environment.getProperty("brokerverse.jobs.bcl-premium-recheck-cron"))
        .isEqualTo("0 30 21 * * *");
    assertThat(environment.getProperty("brokerverse.jobs.bcl-follow-up-due-cron"))
        .isEqualTo("0 0 22 * * *");
    assertThat(environment.getProperty("brokerverse.jobs.bcl-ageing-alerts-cron"))
        .isEqualTo("0 0 22 * * *");
    ReportMetadata report = ReportMetadata.claimsHandling("BCL-TEST", "Test", "Test", List.of());
    assertThat(report.category()).isEqualTo(ReportCategory.CLAIMS_HANDLING);
    assertThat(report.category().label()).isEqualTo("Claims Handling");
    assertThat(report.permission()).isEqualTo(Permission.BCL_REPORT_VIEW);
    assertThat(report.exportPermission()).isEqualTo(Permission.BCL_REPORT_EXPORT);
  }

  @Test
  void claimRowsMapToTheEntityAndItsParts() {
    Long companyId =
        jdbc.queryForObject("select id from org_company where code = 'FVI'", Long.class);
    String claimNo = "BCL-T-" + System.nanoTime();
    jdbc.update(
        "insert into bcl_claim (company_id, claim_no, unit_code, handler, source, arn, policy_year,"
            + " policy_no, assured_name, period_from, period_to, sum_insured, currency,"
            + " premium_status, loss_date, reported_date, loss_nature, claim_amount,"
            + " claimant_name, status_code, phase, next_follow_up_date, created_at, created_by)"
            + " values (?, ?, 'MOTOR_HO', 'clmofficer', 'BDOI_NOTICE', 'ARN-T-1', 2026, 'MC-1',"
            + " 'Juan Dela Cruz', date '2026-01-01', date '2026-12-31', 1500000.00, 'PHP',"
            + " 'PAID', date '2026-09-01', date '2026-09-02', 'MOTOR_OWN_DAMAGE', 85000.50,"
            + " 'Juan Dela Cruz', 'NEW_COMPLETE_DOCS', 'NEW', date '2026-09-09', now(), 'TEST')",
        companyId,
        claimNo);

    Long id =
        tx.execute(
            s -> {
              Claim claim = claims.findByCompanyIdAndClaimNo(companyId, claimNo).orElseThrow();
              assertThat(claim.getSource()).isEqualTo(ClaimSource.BDOI_NOTICE);
              assertThat(claim.getCover().getArn()).isEqualTo("ARN-T-1");
              assertThat(claim.getCover().getSumInsured()).isEqualByComparingTo("1500000");
              assertThat(claim.getCover().getPremiumStatus()).isEqualTo(ClaimPremiumStatus.PAID);
              assertThat(claim.getLoss().getLossDate()).isEqualTo(LocalDate.of(2026, 9, 1));
              assertThat(claim.getLoss().getClaimAmount()).isEqualTo(new BigDecimal("85000.50"));
              assertThat(claim.getLoss().isClaimantOverridden()).isFalse();
              assertThat(claim.getProgress().getPhase()).isEqualTo(ClaimPhase.NEW);
              assertThat(claim.getProgress().getStatusCode()).isEqualTo("NEW_COMPLETE_DOCS");
              assertThat(claim.isClosed()).isFalse();
              claim.assignTo("clmofficer2", "NON_MOTOR_HO");
              return claim.getId();
            });

    assertThat(query("select handler || ':' || unit_code from bcl_claim where id = ?", id))
        .containsExactly("clmofficer2:NON_MOTOR_HO");
    assertThat(claims.findByIdAndCompanyId(id, companyId)).isPresent();
    assertThat(claims.findByIdAndCompanyId(id, -1L)).isEmpty();
    assertThat(
            claims.findByCompanyIdAndCoverArnAndCoverPolicyYearOrderByIdDesc(
                companyId, "ARN-T-1", 2026))
        .extracting(Claim::getClaimNo)
        .contains(claimNo);
  }
}
