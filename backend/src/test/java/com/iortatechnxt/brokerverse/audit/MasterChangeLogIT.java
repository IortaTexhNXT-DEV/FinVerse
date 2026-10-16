package com.iortatechnxt.brokerverse.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog.Entry;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService.PartyContact;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class MasterChangeLogIT {

  @Autowired private InsurerService insurers;
  @Autowired private CatalogRecords records;
  @Autowired private MasterChangeLog log;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private MockMvc mvc;

  private static InsurerDetails details(String name, int creditDays) {
    return new InsurerDetails(
        name,
        null,
        "IC-LOG-1",
        LocalDate.of(2027, 12, 31),
        PlacementChannel.EMAIL,
        List.of("nb@changelog.example"),
        creditDays);
  }

  @Test
  void everyFieldChangeIsLoggedWithMakerAndChecker() {
    Long company = data.company().getId();
    String code = "INS-LOG" + Long.toString(System.nanoTime() % 1_000_000L, 36).toUpperCase();
    InsurerProfile insurer =
        as.run(
            "badmin",
            () ->
                insurers.create(
                    company,
                    code,
                    new PartyContact(null, null, null, null),
                    details("Change Log Mutual", 30)));
    as.run("approver", () -> records.authorize(CatalogKind.INSURER, insurer.getId()));
    as.run("badmin", () -> insurers.update(insurer.getId(), details("Change Log Mutual Inc", 45)));

    List<Entry> pending = log.history("InsurerProfile", insurer.getId());
    assertThat(pending)
        .filteredOn(e -> "UPDATE".equals(e.changeKind()))
        .extracting(Entry::field, Entry::oldValue, Entry::newValue, Entry::maker, Entry::checker)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(
                "name", "Change Log Mutual", "Change Log Mutual Inc", "badmin", null),
            org.assertj.core.groups.Tuple.tuple("defaultCreditDays", "30", "45", "badmin", null));
    assertThat(pending)
        .filteredOn(e -> "CREATE".equals(e.changeKind()))
        .singleElement()
        .extracting(Entry::checker)
        .isEqualTo("approver");

    as.run("approver", () -> records.authorize(CatalogKind.INSURER, insurer.getId()));
    assertThat(log.history("InsurerProfile", insurer.getId()))
        .allSatisfy(
            e -> {
              assertThat(e.checker()).isEqualTo("approver");
              assertThat(e.checkedAt()).isNotNull();
              assertThat(e.madeAt()).isNotNull();
            });
  }

  @Test
  @WithMockUser(authorities = "AUDIT_VIEW")
  void auditorsReadTheHistory() throws Exception {
    mvc.perform(get("/api/v1/change-history?entityType=InsurerProfile&entityId=1"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = "ACCOUNT_VIEW")
  void otherUsersDoNot() throws Exception {
    mvc.perform(get("/api/v1/change-history?entityType=InsurerProfile&entityId=1"))
        .andExpect(status().isForbidden());
  }
}
