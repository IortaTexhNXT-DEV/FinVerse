package com.iortatechnxt.brokerverse;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The reference data the screens show (lists of values, business parameters, notification events,
 * exception codes, accounting events, feeds, file layouts, rules and schedules), as the migrations
 * and the seed data leave it, carries no internal project references ({@link
 * UserTextReferencesTest#INTERNAL_REFERENCE}).
 */
@IntegrationTest
class UserTextReferencesIT {

  private static final List<String> TABLES =
      List.of(
          "lov_type",
          "lov_value",
          "sys_parameter",
          "msg_notification_event",
          "alt_exception_code",
          "acc_event_type",
          "ops_flow_in_feed",
          "csh_payment_file_layout",
          "csh_minimal_balance_rule",
          "nba_retention_rule",
          "cmr_incentive_scheme",
          "frbs_service_fee_rule",
          "fin_schedule_def",
          "bkg_incentive_rule",
          "rem_incentive_rule",
          "cat_incentive_criteria",
          "wf_stage");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void referenceTextsCarryNoInternalReferences() {
    List<String> hits = new ArrayList<>();
    for (Map<String, Object> column :
        jdbc.queryForList(
            "select table_name, column_name from information_schema.columns"
                + " where table_schema = current_schema() and table_name = any(?::text[])"
                + " and data_type in ('text', 'character varying')"
                + " order by table_name, column_name",
            "{" + String.join(",", TABLES) + "}")) {
      String table = (String) column.get("table_name");
      String name = (String) column.get("column_name");
      for (String value :
          jdbc.queryForList(
              "select distinct \""
                  + name
                  + "\" from "
                  + table
                  + " where \""
                  + name
                  + "\" is not null",
              String.class)) {
        if (UserTextReferencesTest.INTERNAL_REFERENCE.matcher(value).find()) {
          hits.add(table + "." + name + ": " + value);
        }
      }
    }
    assertThat(hits).isEmpty();
  }
}
