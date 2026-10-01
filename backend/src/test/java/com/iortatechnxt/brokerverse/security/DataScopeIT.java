package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.service.DataScopeService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Company and branch data scope end to end (DATA_SCOPE_DESIGN.md): every user starts with all
 * companies; an administrator narrows a user's scope; the query parameter, path variable and
 * request body checks refuse other companies and branches with 403 DATA_SCOPE_DENIED; the company
 * list shows only the allowed companies; every change is in the access change log; an access
 * request carries a scope and applies it on approval.
 */
@IntegrationTest
class DataScopeIT {

  private static final String ADMIN = "admin";
  private static final String DENIED = "DATA_SCOPE_DENIED";
  private static final String STRONG = "Str0ng!Passw0rd";

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AccessRequestService requests;
  @Autowired private UserAdminService users;
  @Autowired private DataScopeService scopes;
  @Autowired private AsUser as;

  private long companyA;
  private long branchA1;
  private long branchA2;
  private long companyB;

  @BeforeEach
  void companies() {
    companyA = company("DA");
    branchA1 = branch(companyA, "A1");
    branchA2 = branch(companyA, "A2");
    companyB = company("DB");
    branch(companyB, "HO");
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
        "Scope company " + code);
    return jdbc.queryForObject("select id from org_company where code = ?", Long.class, code);
  }

  private long branch(long companyId, String code) {
    jdbc.update(
        """
        insert into org_branch (company_id, code, name, opening_date, head_office,
            forex_authorized, record_status, authorized_by, authorized_at, created_at, created_by)
        values (?, ?, ?, date '2010-01-01', false, false, 'ACTIVE', 'SYSTEM', now(), now(),
            'SYSTEM')
        """,
        companyId,
        code,
        "Branch " + code);
    return jdbc.queryForObject(
        "select id from org_branch where company_id = ? and code = ?", Long.class, companyId, code);
  }

  private long newUser(String username) throws Exception {
    JsonNode created =
        api.read(
            api.doPost(
                    ADMIN,
                    "/api/v1/admin/users",
                    Json.of(
                        "user",
                        Json.of(
                            "username",
                            username,
                            "fullName",
                            "Scope " + username,
                            "roleCodes",
                            List.of("ACCOUNTANT"),
                            "enabled",
                            true),
                        "initialPassword",
                        Json.of("newPassword", STRONG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allCompanies").value(true)));
    return created.get("id").asLong();
  }

  private static String username() {
    return "scope" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
  }

  private Map<String, Object> onlyBranchA1() {
    return Json.of(
        "allCompanies",
        false,
        "companies",
        List.of(
            Json.of("companyId", companyA, "allBranches", false, "branchIds", List.of(branchA1))));
  }

  private String holidays(long companyId) {
    return "/api/v1/organization/holidays?year=2026&companyId=" + companyId;
  }

  @Test
  void aNarrowedUserIsRefusedOtherCompaniesAndBranches() throws Exception {
    String user = username();
    long id = newUser(user);
    api.doGet(user, holidays(companyB)).andExpect(status().isOk());

    api.doPut(ADMIN, "/api/v1/admin/users/" + id + "/data-scope", onlyBranchA1())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allCompanies").value(false))
        .andExpect(jsonPath("$.companies[0].companyId").value(companyA))
        .andExpect(jsonPath("$.companies[0].branches[0].code").value("A1"));

    // Query parameter.
    api.doGet(user, holidays(companyA)).andExpect(status().isOk());
    api.doGet(user, holidays(companyB))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(DENIED));
    // Query parameter branch.
    api.doGet(user, "/api/v1/dashboard?companyId=" + companyA + "&branchId=" + branchA2)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(DENIED));
    // Request body.
    api.doPost(
            user,
            "/api/v1/organization/holidays",
            Json.of("companyId", companyB, "holidayDate", "2026-12-24", "description", "Eve"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(DENIED));
    api.doPost(
            user,
            "/api/v1/organization/holidays",
            Json.of(
                "companyId",
                companyA,
                "branchId",
                branchA2,
                "holidayDate",
                "2026-12-24",
                "description",
                "Eve"))
        .andExpect(status().isForbidden());
    // The company list shows the allowed companies only.
    JsonNode companies = api.read(api.doGet(user, "/api/v1/organization/companies"));
    assertThat(companies.findValuesAsText("id")).containsExactly(String.valueOf(companyA));
    JsonNode branches =
        api.read(api.doGet(user, "/api/v1/organization/branches?companyId=" + companyA));
    assertThat(branches.findValuesAsText("code")).containsExactly("A1");
    // Every change is in the access change log, in business words.
    assertThat(
            jdbc.queryForObject(
                "select to_value from sec_access_change_log where lower(subject) = lower(?)"
                    + " and activity = 'DATA_SCOPE_CHANGED'",
                String.class,
                user))
        .endsWith(": A1");

    api.doPut(ADMIN, "/api/v1/admin/users/" + id + "/data-scope", Json.of("allCompanies", true))
        .andExpect(jsonPath("$.description").value("All companies"));
    api.doGet(user, holidays(companyB)).andExpect(status().isOk());
  }

  @Test
  void scopesAreValidatedAndNobodyChangesHisOwn() throws Exception {
    long id = newUser(username());
    String url = "/api/v1/admin/users/" + id + "/data-scope";
    api.doPut(ADMIN, url, Json.of("allCompanies", false, "companies", List.of()))
        .andExpect(jsonPath("$.code").value("DATA_SCOPE_EMPTY"));
    api.doPut(
            ADMIN,
            url,
            Json.of(
                "allCompanies",
                false,
                "companies",
                List.of(Json.of("companyId", -1, "allBranches", true))))
        .andExpect(jsonPath("$.code").value("DATA_SCOPE_UNKNOWN_COMPANY"));
    api.doPut(
            ADMIN,
            url,
            Json.of(
                "allCompanies",
                false,
                "companies",
                List.of(
                    Json.of(
                        "companyId",
                        companyB,
                        "allBranches",
                        false,
                        "branchIds",
                        List.of(branchA1)))))
        .andExpect(jsonPath("$.code").value("DATA_SCOPE_UNKNOWN_BRANCH"));
    api.doPut(
            ADMIN,
            url,
            Json.of(
                "allCompanies",
                false,
                "companies",
                List.of(Json.of("companyId", companyB, "allBranches", false))))
        .andExpect(jsonPath("$.code").value("DATA_SCOPE_NO_BRANCH"));
    long adminId = users.getByUsername(ADMIN).getId();
    api.doPut(ADMIN, "/api/v1/admin/users/" + adminId + "/data-scope", onlyBranchA1())
        .andExpect(jsonPath("$.code").value("DATA_SCOPE_SELF_CHANGE"));

    api.doGet(ADMIN, url).andExpect(jsonPath("$.allCompanies").value(true));
    JsonNode units = api.read(api.doGet(ADMIN, "/api/v1/admin/data-scope/units"));
    assertThat(units.findValuesAsText("id"))
        .contains(String.valueOf(companyA), String.valueOf(companyB));
  }

  @Test
  void aRestrictedAdministratorGrantsOnlyInsideHisScope() throws Exception {
    String narrowAdmin = username();
    long narrowId =
        api.read(
                api.doPost(
                    ADMIN,
                    "/api/v1/admin/users",
                    Json.of(
                        "user",
                        Json.of(
                            "username",
                            narrowAdmin,
                            "fullName",
                            "Scope admin",
                            "roleCodes",
                            List.of("SYSADMIN"),
                            "enabled",
                            true),
                        "initialPassword",
                        Json.of("newPassword", STRONG))))
            .get("id")
            .asLong();
    api.doPut(ADMIN, "/api/v1/admin/users/" + narrowId + "/data-scope", onlyBranchA1())
        .andExpect(status().isOk());
    long target = newUser(username());
    String url = "/api/v1/admin/users/" + target + "/data-scope";
    api.doPut(narrowAdmin, url, Json.of("allCompanies", true))
        .andExpect(jsonPath("$.code").value("DATA_SCOPE_BEYOND_OWN"));
    api.doPut(narrowAdmin, url, onlyBranchA1()).andExpect(status().isOk());
    JsonNode units = api.read(api.doGet(narrowAdmin, "/api/v1/admin/data-scope/units"));
    assertThat(units.findValuesAsText("id"))
        .containsExactly(String.valueOf(companyA), String.valueOf(branchA1));
  }

  @Test
  void anAccessRequestCarriesTheScopeAndAppliesItOnApproval() {
    // A new user ID in the format of the user ID pattern (a letter and nine digits).
    String name = String.format("u%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
    String scope = companyA + ":" + branchA1;
    AccessRequestContent enrol =
        new AccessRequestContent(
                AccessRequestType.CREATE_USER,
                name,
                "Scoped User",
                name + "@example.ph",
                Set.of("MKT_AO"),
                null,
                "Joined the branch")
            .withUserData(new RequestedUserData(null, null, null, null, false, null, scope));
    AccessRequest r =
        as.run("requestor", () -> requests.create(enrol, false, List.of("uamapprover")));
    as.run("uamapprover", () -> requests.approve(r.getId(), null));
    AppUser created = users.getByUsername(name);
    assertThat(scopes.scopeOf(created.getId()).text()).isEqualTo(scope);

    AccessRequestContent widen =
        new AccessRequestContent(
                AccessRequestType.MODIFY_USER, name, null, null, Set.of(), null, "Head office")
            .withUserData(
                new RequestedUserData(null, null, null, null, false, null, UserDataScope.ALL_TEXT));
    AccessRequest change =
        as.run("requestor", () -> requests.create(widen, false, List.of("uamapprover")));
    as.run("uamapprover", () -> requests.approve(change.getId(), null));
    assertThat(scopes.scopeOf(created.getId())).isEqualTo(UserDataScope.ALL);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_access_change_log where lower(subject) = lower(?)"
                    + " and activity = 'DATA_SCOPE_CHANGED' and request_no is not null",
                Integer.class,
                name))
        .isEqualTo(2);
  }
}
