package com.iortatechnxt.brokerverse.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService.PartyContact;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.service.DataScopeService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The change history of a record follows the data scope of the reader (G05): a reader whose scope
 * holds the owning company sees it, a reader outside it is answered 404 as if the record did not
 * exist, and a reader with all companies sees every record.
 */
@IntegrationTest
class ChangeHistoryDataScopeIT {

  private static final String ADMIN = "admin";
  private static final String STRONG = "Str0ng!Passw0rd";

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private InsurerService insurers;
  @Autowired private UserAdminService users;
  @Autowired private DataScopeService scopes;
  @Autowired private AsUser as;

  private long companyA;
  private long companyB;
  private long insurerA;
  private long insurerB;

  @BeforeEach
  void records() {
    companyA = company("HA");
    companyB = company("HB");
    insurerA = insurer(companyA);
    insurerB = insurer(companyB);
  }

  private long company(String prefix) {
    String code = prefix + ThreadLocalRandom.current().nextInt(1000, 9999);
    jdbc.update(
        """
        insert into org_company (code, name, base_currency, fiscal_year_start_month,
            back_value_days, forward_value_days, record_status, authorized_by, authorized_at,
            created_at, created_by)
        values (?, ?, 'PHP', 1, 45, 5, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM')
        """,
        code,
        "History company " + code);
    return jdbc.queryForObject("select id from org_company where code = ?", Long.class, code);
  }

  private long insurer(long company) {
    String code = "INS-H" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    InsurerProfile created =
        as.run(
            "badmin",
            () ->
                insurers.create(
                    company,
                    code,
                    new PartyContact(null, null, null, null),
                    new InsurerDetails(
                        "History Mutual " + code,
                        null,
                        "IC-H-1",
                        LocalDate.of(2027, 12, 31),
                        PlacementChannel.EMAIL,
                        List.of("nb@history.example"),
                        30)));
    return created.getId();
  }

  private String reader(UserDataScope scope) {
    String username = "hist" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    AppUser user =
        as.run(
            ADMIN,
            () ->
                users.createUser(
                    new UserRequest(
                        username, "History " + username, null, null, null, Set.of(), true),
                    STRONG));
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select ?, id from sec_role where code = ?",
        user.getId(),
        "BUSINESS_ADMIN");
    if (scope != null) {
      as.run(ADMIN, () -> scopes.replace(user.getId(), scope));
    }
    return username;
  }

  private static String url(long insurer) {
    return "/api/v1/change-history?entityType=InsurerProfile&entityId=" + insurer;
  }

  @Test
  void aReaderInScopeSeesTheHistory() throws Exception {
    String user = reader(UserDataScope.of(List.of(CompanyScope.allBranchesOf(companyA))));
    assertThat(api.read(api.doGet(user, url(insurerA)).andExpect(status().isOk())).size())
        .isPositive();
  }

  @Test
  void aReaderOutOfScopeIsAnsweredNotFound() throws Exception {
    String user = reader(UserDataScope.of(List.of(CompanyScope.allBranchesOf(companyA))));
    api.doGet(user, url(insurerB)).andExpect(status().isNotFound());
    api.doGet(user, url(Long.MAX_VALUE)).andExpect(status().isNotFound());
  }

  @Test
  void aReaderWithAllCompaniesSeesEveryRecord() throws Exception {
    String user = reader(null);
    api.doGet(user, url(insurerA)).andExpect(status().isOk());
    api.doGet(user, url(insurerB)).andExpect(status().isOk());
  }

  @Test
  void theChangeLogKeepsTheRecordsOfTheScopeAndExportsThem() throws Exception {
    String user = reader(UserDataScope.of(List.of(CompanyScope.allBranchesOf(companyA))));
    String url = "/api/v1/change-history/log?entityType=InsurerProfile&maker=badmin";
    JsonNode rows = api.read(api.doGet(user, url).andExpect(status().isOk()));
    assertThat(rows.findValuesAsText("entityId"))
        .contains(String.valueOf(insurerA))
        .doesNotContain(String.valueOf(insurerB));
    JsonNode all = api.read(api.doGet(reader(null), url).andExpect(status().isOk()));
    assertThat(all.findValuesAsText("entityId"))
        .contains(String.valueOf(insurerA), String.valueOf(insurerB));
    byte[] file =
        api.download(user, url.replace("/log?", "/log/export?"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(file).startsWith((byte) 'P', (byte) 'K');
    assertThat(api.read(api.doGet(user, "/api/v1/change-history/log/types")).toString())
        .contains("InsurerProfile");
  }
}
