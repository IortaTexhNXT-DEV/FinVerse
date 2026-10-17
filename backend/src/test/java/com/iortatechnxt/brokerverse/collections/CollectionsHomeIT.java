package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource.WorkCount;
import com.iortatechnxt.brokerverse.collections.home.service.CollectionsHomeService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Follow-up tiles of the Collections home for handlers and Team Leads (FR-CL-085). */
@IntegrationTest
class CollectionsHomeIT {

  private static final List<String> FOLLOW_UP =
      List.of("brokenPromises", "promisesDue", "overdueInstallments", "openEscalations");

  @Autowired private CollectionsHomeService home;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private List<String> keys(String user) {
    return as.run(user, () -> home.home(data.company().getId())).tiles().stream()
        .map(WorkCount::key)
        .toList();
  }

  @Test
  void handlersAndTeamLeadsSeeTheirFollowUps() {
    assertThat(keys("mktcoll")).containsAll(FOLLOW_UP);
    assertThat(keys("mkttl")).containsAll(FOLLOW_UP);
    assertThat(as.run("mkttl", () -> home.home(data.company().getId())).tiles())
        .filteredOn(t -> FOLLOW_UP.contains(t.key()))
        .allMatch(t -> t.value() >= 0 && t.link().startsWith("/collections/"));
  }
}
