package com.iortatechnxt.brokerverse.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.service.DataScopeService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Records addressed by a text business key (invoice number, account reference, batch number) follow
 * the data scope of the caller, like the records addressed by id (DATA_SCOPE_DESIGN.md section
 * 4.1): a caller whose scope holds the owning company reaches the record, a caller outside it is
 * answered 404 and nothing is changed, a caller with all companies reaches every record, and an
 * unknown key is left to the endpoint.
 */
@IntegrationTest
class TextKeyScopeIT {

  private static final String ADMIN = "admin";
  private static final String STRONG = "Str0ng!Passw0rd";
  private static final String ROLE = "TEXT_KEY_SCOPE_TEST";

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserAdminService users;
  @Autowired private DataScopeService scopes;
  @Autowired private AsUser as;

  private long invoiceCompany;
  private String invoiceNo;
  private long accountCompany;
  private String arn;
  private long otherCompany;

  @BeforeEach
  void records() {
    Map<String, Object> invoice =
        jdbc.queryForMap(
            "select company_id, invoice_no from bkg_invoice where invoice_no is not null"
                + " order by id limit 1");
    invoiceCompany = ((Number) invoice.get("company_id")).longValue();
    invoiceNo = (String) invoice.get("invoice_no");
    Map<String, Object> account =
        jdbc.queryForMap(
            "select company_id, arn from acc_account where arn is not null order by id limit 1");
    accountCompany = ((Number) account.get("company_id")).longValue();
    arn = (String) account.get("arn");
    otherCompany = company();
    jdbc.update(
        "insert into sec_role (code, name, created_at, created_by) select ?, 'Text key scope test',"
            + " now(), 'test' where not exists (select 1 from sec_role where code = ?)",
        ROLE,
        ROLE);
    for (String p : List.of("ACCOUNT_VIEW", "BOOKING_PROCESS", "CLX_VIEW", "CLX_WORK")) {
      jdbc.update(
          "insert into sec_role_permission (role_id, permission) select id, ? from sec_role"
              + " where code = ? and not exists (select 1 from sec_role_permission x"
              + " where x.role_id = sec_role.id and x.permission = ?)",
          p,
          ROLE,
          p);
    }
  }

  private long company() {
    String code = "TK" + ThreadLocalRandom.current().nextInt(1000, 9999);
    jdbc.update(
        """
        insert into org_company (code, name, base_currency, fiscal_year_start_month,
            back_value_days, forward_value_days, record_status, authorized_by, authorized_at,
            created_at, created_by)
        values (?, ?, 'PHP', 1, 45, 5, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM')
        """,
        code,
        "Text key company " + code);
    return jdbc.queryForObject("select id from org_company where code = ?", Long.class, code);
  }

  private String caller(Long company) {
    String username = "tkey" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    AppUser user =
        as.run(
            ADMIN,
            () ->
                users.createUser(
                    new UserRequest(username, "Key " + username, null, null, null, Set.of(), true),
                    STRONG));
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select ?, id from sec_role where code = ?",
        user.getId(),
        ROLE);
    if (company != null) {
      UserDataScope scope = UserDataScope.of(List.of(CompanyScope.allBranchesOf(company)));
      as.run(ADMIN, () -> scopes.replace(user.getId(), scope));
    }
    return username;
  }

  @Test
  void aCallerInScopeReachesTheRecordsByTheirKey() throws Exception {
    api.doGet(caller(invoiceCompany), "/api/v1/booking/invoices/by-no/" + invoiceNo)
        .andExpect(status().isOk());
    api.doGet(caller(accountCompany), "/api/v1/accounts/by-arn/" + arn).andExpect(status().isOk());
  }

  @Test
  void aCallerOutOfScopeIsAnsweredNotFound() throws Exception {
    String user = caller(otherCompany);
    api.doGet(user, "/api/v1/booking/invoices/by-no/" + invoiceNo).andExpect(status().isNotFound());
    api.doGet(user, "/api/v1/accounts/by-arn/" + arn).andExpect(status().isNotFound());
    api.doGet(user, "/api/v1/booking/accounts/" + arn + "/invoices")
        .andExpect(status().isNotFound());
    api.doGet(user, "/api/v1/collections/items/" + invoiceNo).andExpect(status().isNotFound());
  }

  @Test
  void aCallerOutOfScopeChangesNothing() throws Exception {
    String user = caller(otherCompany);
    api.doPost(user, "/api/v1/collections/items/" + invoiceNo + "/lock", null)
        .andExpect(status().isNotFound());
    assertThat(lockedBy(user)).isZero();
  }

  @Test
  void aCallerWithAllCompaniesReachesEveryRecord() throws Exception {
    String user = caller(null);
    api.doGet(user, "/api/v1/booking/invoices/by-no/" + invoiceNo).andExpect(status().isOk());
    api.doGet(user, "/api/v1/accounts/by-arn/" + arn).andExpect(status().isOk());
  }

  @Test
  void anUnknownKeyIsLeftToTheEndpoint() throws Exception {
    api.doGet(caller(otherCompany), "/api/v1/accounts/by-arn/NO-SUCH-ARN-0")
        .andExpect(status().isNotFound());
  }

  private long lockedBy(String user) {
    Long n =
        jdbc.queryForObject(
            "select count(*) from clx_item where invoice_no = ? and editing_by = ?",
            Long.class,
            invoiceNo,
            user);
    return n == null ? 0 : n;
  }
}
