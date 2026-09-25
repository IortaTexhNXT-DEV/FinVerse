package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.collections.demo.CollectionsPlansDemoData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The Collections plans demo storyline (wave C1-B) on a fresh demo database: the three-year demo
 * account has a statement of account for each of its three billing cycles, the broken promise
 * escalated the account to the team lead, the job escalated the overdue installment, and running
 * the runner again changes nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class CollectionsPlansDemoDataIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private CollectionsPlansDemoData runner;

  private long count(String sql, Object... args) {
    Long n = jdbc.queryForObject(sql, Long.class, args);
    return n == null ? 0 : n;
  }

  private List<String> values(String sql) {
    return jdbc.queryForList(sql, String.class);
  }

  @Test
  void theThreeYearAccountHasAStatementPerBillingCycle() {
    Map<String, Object> plan =
        jdbc.queryForMap(
            "select id, arn, installment_count, created_by from clx_installment_plan"
                + " where source = 'POLICY_YEARS'");
    assertThat(plan.get("installment_count")).isEqualTo(3);
    assertThat(plan.get("created_by")).isEqualTo("mktcoll");
    assertThat(
            values(
                "select cast(policy_year as varchar) from clx_installment where plan_id = "
                    + plan.get("id")
                    + " order by seq"))
        .containsExactly("1", "2", "3");
    assertThat(
            count(
                "select count(distinct cycle_seq) from clx_billing_statement where plan_id = ?"
                    + " and status <> 'CANCELLED'",
                plan.get("id")))
        .isEqualTo(3);
    assertThat(values("select status from clx_billing_statement")).contains("SENT", "GENERATED");
    assertThat(count("select count(*) from clx_billing_document")).isGreaterThanOrEqualTo(3);
    assertThat(
            count(
                "select count(*) from clx_installment_plan where source = 'GENERATED'"
                    + " and frequency = 'QUARTERLY' and installment_count = 4"))
        .isEqualTo(1);
  }

  @Test
  void theBrokenPromiseEscalatedTheAccountToTheTeamLead() {
    assertThat(values("select status from clx_promise")).contains("BROKEN", "KEPT", "OPEN");
    Map<String, Object> escalation =
        jdbc.queryForMap(
            "select kind, status, target_username from clx_escalation"
                + " where rule_code = 'CLX-BROKEN-PROMISE'");
    assertThat(escalation.get("kind")).isEqualTo("AUTO");
    assertThat(escalation.get("target_username")).isEqualTo("mkttl");
    assertThat(escalation.get("status")).isEqualTo("IN_ACTION");
    assertThat(values("select rule_code from clx_escalation where kind = 'AUTO'"))
        .contains("CLX-INSTALLMENT-15");
    assertThat(values("select created_by from clx_escalation where kind = 'MANUAL'"))
        .containsOnly("mktcoll");
    assertThat(values("select record_status from clx_escalation_rule"))
        .contains("ACTIVE", "PENDING_AUTHORIZATION");
  }

  @Test
  void theRunnerChangesNothingWhenTheApplicationStartsAgain() {
    long plans = count("select count(*) from clx_installment_plan");
    long escalations = count("select count(*) from clx_escalation");
    runner.run(new DefaultApplicationArguments());
    assertThat(count("select count(*) from clx_installment_plan")).isEqualTo(plans);
    assertThat(count("select count(*) from clx_escalation")).isEqualTo(escalations);
  }
}
