package com.iortatechnxt.brokerverse.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.util.BusinessText;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The events of the notification settings screen and the minimal balance rules of Cashiering Setup
 * are named and described in business terms only: no requirement references such as "(MKT 1.20.0,
 * 2.26.0)" or "(Cashiering summary 5.f)" and none of the references of {@link
 * BusinessText#FORBIDDEN}.
 */
@IntegrationTest
class NotificationEventWordingIT {

  /** A section reference in brackets: "(MKT 1.20.0", "(DIS 2.7", "(Cashiering summary 5.f)". */
  private static final Pattern SECTION_REFERENCE =
      Pattern.compile("\\([A-Z]{2,5} \\d+\\.\\d|\\bsummary \\d+\\.[a-z]\\b");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void notificationEventsCarryNoRequirementReferences() {
    assertBusinessWording(
        "select code || ': ' || name || ' - ' || coalesce(description, '')"
            + " from msg_notification_event");
  }

  @Test
  void minimalBalanceRulesCarryNoRequirementReferences() {
    assertBusinessWording(
        "select kind || ': ' || coalesce(description, '') from csh_minimal_balance_rule");
  }

  private void assertBusinessWording(String query) {
    List<String> texts = jdbc.queryForList(query, String.class);
    List<String> findings =
        texts.stream()
            .filter(
                t -> SECTION_REFERENCE.matcher(t).find() || BusinessText.FORBIDDEN.matcher(t).find())
            .toList();
    assertThat(texts).isNotEmpty();
    assertThat(findings).isEmpty();
  }
}
