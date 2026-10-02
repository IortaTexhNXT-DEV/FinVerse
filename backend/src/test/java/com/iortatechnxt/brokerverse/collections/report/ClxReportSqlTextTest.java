package com.iortatechnxt.brokerverse.collections.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The Collections report files read as the screens do: the handler, the account officer and the
 * unit head by name, the disposition by its label, the tagging owner and the status in words.
 */
class ClxReportSqlTextTest {

  @Test
  void usersAreNamedAndCodesReadInWords() {
    String sql = ClxReportSql.ITEM_SELECT;
    for (String column : new String[] {"current_handler", "ao_username", "unit_head_username"}) {
      assertThat(sql)
          .contains(
              "coalesce((select u.full_name from sec_user u where u.username = i." + column + "),")
          .contains(" i." + column + ") as " + column);
    }
    assertThat(sql)
        .contains("select v.label from lov_value v where v.type_code = 'CLX_PR_DISPOSITION'")
        .contains("as disposition_code")
        .contains("initcap(i.tagging_owner) as tagging_owner")
        .contains("initcap(replace(i.status, '_', ' ')) as status")
        .doesNotContain("i.current_handler, i.payment_status");
  }
}
