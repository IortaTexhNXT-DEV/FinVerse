package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestNotifier;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestReturnService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * BDOI FRS FRUM.013.01: the notices of access requests are given in the system and, for the users
 * who chose e-mail in their notification preferences, also by e-mail; the setting
 * UAM_REQUEST_NOTICE_EMAIL keeps them in the system only.
 */
@IntegrationTest
class AccessRequestNoticeEmailIT {

  @Autowired private AccessRequestService requests;
  @Autowired private AccessRequestReturnService returns;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private SystemParameterService parameters;

  private AccessRequest submit(String name) {
    return as.run(
        "badmin",
        () ->
            requests.submit(
                new AccessRequestContent(
                    AccessRequestType.CREATE_USER,
                    name,
                    "Notice Copy User",
                    name + "@example.ph",
                    Set.of("MKT_AO"),
                    null,
                    "Joined Marketing")));
  }

  @Test
  void aReturnedRequestIsAlsoEmailedToTheRequesterWithTheRemarks() {
    int before = returnedMails();
    AccessRequest request = submit(AccessRequestIT.username());
    as.run("approver", () -> returns.returnRequest(request.getId(), "Attach the HR memo number"));
    assertThat(returnedMails()).isGreaterThan(before);
    assertThat(
            jdbc.queryForObject(
                "select body from msg_outbound where purpose = 'UAM_REQUEST_RETURNED'"
                    + " order by id desc limit 1",
                String.class))
        .contains("Attach the HR memo number");
  }

  @Test
  void withTheSettingOffTheNoticeStaysInTheSystem() {
    as.run("admin", () -> parameters.update(AccessRequestNotifier.EMAIL_COPY, "false"));
    try {
      int before = returnedMails();
      AccessRequest request = submit(AccessRequestIT.username());
      as.run("approver", () -> returns.returnRequest(request.getId(), "Wrong unit"));
      assertThat(returnedMails()).isEqualTo(before);
    } finally {
      as.run("admin", () -> parameters.update(AccessRequestNotifier.EMAIL_COPY, "true"));
    }
  }

  private int returnedMails() {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from msg_outbound where purpose = 'UAM_REQUEST_RETURNED'",
            Integer.class);
    return n == null ? 0 : n;
  }
}
