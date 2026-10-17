package com.iortatechnxt.brokerverse.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V1260 (texts naming the system, not the platform) and the seed script V2090 (seed data in
 * business wording) apply on the migrated and seeded database, without switching any guard off.
 */
@IntegrationTest
class WordingMigrationsIT {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void bothScriptsAreApplied() {
    List<String> applied =
        jdbc.queryForList(
            "select version from flyway_schema_history where success and version in ('1260', '2090')"
                + " order by version",
            String.class);
    assertThat(applied).containsExactly("1260", "2090");
  }

  @Test
  void thePlatformTextsNameNoPlatform() {
    assertThat(
            jdbc.queryForObject(
                "select param_value from sys_parameter where param_key = 'MFA_ISSUER_NAME'",
                String.class))
        .isEmpty();
    // REPORT_FOOTER_TEXT is not checked here: SystemAdminIT changes it in the shared database.
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sys_parameter where description like '%BrokerVerse%'",
                Integer.class))
        .isZero();
  }

  @Test
  void theSeedDataReadsAsBusinessData() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from acc_rule where name ~ '\\((?:[Ss]eed|SEED)[^)]*\\)$'",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pty_party where name ~ '\\((?:[Ss]eed|SEED)[^)]*\\)$'",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from wf_case_history where action = 'seed'", Integer.class))
        .isZero();
  }

  @Test
  void theScreeningTimelineKeepsItsSeededEntries() {
    // Known limitation: the insert-only case timeline is never rewritten (its guard stays on).
    assertThat(
            jdbc.queryForObject(
                "select count(*) from scr_case_event where remarks like 'Opened by % (seed)'",
                Integer.class))
        .isPositive();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pg_trigger where tgname = 'trg_scr_case_event_insert_only'"
                    + " and tgenabled = 'O'",
                Integer.class))
        .isOne();
  }
}
