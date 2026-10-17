package com.iortatechnxt.brokerverse.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.audit.api.RecordScopeAdvisor;
import com.iortatechnxt.brokerverse.audit.service.RecordScope;
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
import com.iortatechnxt.brokerverse.support.Json;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Records addressed by id follow the data scope of the caller (record scope check): a caller whose
 * scope holds the owning company reads and changes the record, a caller outside it is answered 404
 * as if the record did not exist and nothing is changed, and a caller with all companies reaches
 * every record. Covers the insurer page and the service invoice batches, a write on a record, a
 * child record owned through its parent, and an unknown id.
 */
@IntegrationTest
class RecordScopeIT {

  private static final String ADMIN = "admin";
  private static final String STRONG = "Str0ng!Passw0rd";

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private InsurerService insurers;
  @Autowired private UserAdminService users;
  @Autowired private DataScopeService scopes;
  @Autowired private AsUser as;
  @Autowired private RecordScope recordScope;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mappings;

  private long companyA;
  private long companyB;
  private long insurerA;
  private long insurerB;
  private long batchA;
  private long batchB;

  @BeforeEach
  void records() {
    companyA = company("RA");
    companyB = company("RB");
    insurerA = insurer(companyA);
    insurerB = insurer(companyB);
    batchA = batch(companyA);
    batchB = batch(companyB);
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

  private long insurer(long company) {
    String code = "INS-R" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    InsurerProfile created =
        as.run(
            "badmin",
            () ->
                insurers.create(
                    company,
                    code,
                    new PartyContact(null, null, null, null),
                    new InsurerDetails(
                        "Scope Mutual " + code,
                        null,
                        "IC-R-1",
                        LocalDate.of(2027, 12, 31),
                        PlacementChannel.EMAIL,
                        List.of("nb@scope.example"),
                        30)));
    return created.getId();
  }

  private long batch(long company) {
    String insurer = "INS-B" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    return jdbc.queryForObject(
        """
        insert into bkg_si_batch (company_id, insurer_code, insurer_name, batch_date, item_count,
            commission_total, net_total, delivery_status, created_at, created_by)
        values (?, ?, 'Scope Batch Insurer', current_date, 0, 0, 0, 'GENERATED', now(), 'SYSTEM')
        returning id
        """,
        Long.class,
        company,
        insurer);
  }

  private String caller(UserDataScope scope) {
    String username = "scope" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    AppUser user =
        as.run(
            ADMIN,
            () ->
                users.createUser(
                    new UserRequest(
                        username, "Scope " + username, null, null, null, Set.of(), true),
                    STRONG));
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select ?, id from sec_role where code = ?",
        user.getId(),
        "BUSINESS_ADMIN");
    jdbc.update(
        """
        insert into sec_user_role (user_id, role_id)
        select ?, min(role_id) from sec_role_permission where permission = 'BOOKING_PROCESS'
        on conflict do nothing
        """,
        user.getId());
    if (scope != null) {
      as.run(ADMIN, () -> scopes.replace(user.getId(), scope));
    }
    return username;
  }

  private String callerOfA() {
    return caller(UserDataScope.of(List.of(CompanyScope.allBranchesOf(companyA))));
  }

  private int contactsOf(long insurer) {
    return jdbc.queryForObject(
        "select count(*) from cat_insurer_contact where insurer_id = ?", Integer.class, insurer);
  }

  @Test
  void aCallerInScopeReadsTheRecords() throws Exception {
    String user = callerOfA();
    api.doGet(user, "/api/v1/catalog/insurers/" + insurerA).andExpect(status().isOk());
    api.doGet(user, "/api/v1/catalog/insurers/" + insurerA + "/accounts")
        .andExpect(status().isOk());
    api.doGet(user, "/api/v1/booking/si-batches/" + batchA + "/items").andExpect(status().isOk());
  }

  @Test
  void aCallerOutOfScopeIsAnsweredNotFound() throws Exception {
    String user = callerOfA();
    api.doGet(user, "/api/v1/catalog/insurers/" + insurerB).andExpect(status().isNotFound());
    api.doGet(user, "/api/v1/catalog/insurers/" + insurerB + "/accounts")
        .andExpect(status().isNotFound());
    api.doGet(user, "/api/v1/booking/si-batches/" + batchB + "/items")
        .andExpect(status().isNotFound());
    api.doGet(user, "/api/v1/booking/si-batches/" + batchB + "/manifest")
        .andExpect(status().isNotFound());
    api.doGet(user, "/api/v1/catalog/insurers/" + Long.MAX_VALUE).andExpect(status().isNotFound());
  }

  @Test
  void aCallerOutOfScopeChangesNothing() throws Exception {
    String user = callerOfA();
    api.doPost(
            user,
            "/api/v1/catalog/insurers/" + insurerB + "/contacts",
            Json.of("role", "CLAIMS", "name", "Out Of Scope"))
        .andExpect(status().isNotFound());
    assertThat(contactsOf(insurerB)).isZero();
    api.doPost(
            user,
            "/api/v1/catalog/insurers/" + insurerA + "/contacts",
            Json.of("role", "CLAIMS", "name", "In Scope"))
        .andExpect(status().isCreated());
    assertThat(contactsOf(insurerA)).isEqualTo(1);
  }

  @Test
  void aCatalogRecordOfAnotherCompanyIsNotAuthorizedByKind() throws Exception {
    api.doPost(callerOfA(), "/api/v1/catalog/records/INSURER/" + insurerB + "/authorize", null)
        .andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "select record_status from cat_insurer where id = ?", String.class, insurerB))
        .isEqualTo("PENDING_AUTHORIZATION");
  }

  @Test
  void aChildRecordFollowsItsParent() throws Exception {
    String all = caller(null);
    api.doPost(
            all,
            "/api/v1/catalog/insurers/" + insurerB + "/contacts",
            Json.of("role", "CLAIMS", "name", "Parent B"))
        .andExpect(status().isCreated());
    long contact =
        jdbc.queryForObject(
            "select id from cat_insurer_contact where insurer_id = ?", Long.class, insurerB);
    api.doPut(
            callerOfA(),
            "/api/v1/catalog/insurers/contacts/" + contact,
            Json.of("role", "CLAIMS", "name", "Changed"))
        .andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "select name from cat_insurer_contact where id = ?", String.class, contact))
        .isEqualTo("Parent B");
  }

  @Test
  void aCallerWithAllCompaniesReachesEveryRecord() throws Exception {
    String user = caller(null);
    api.doGet(user, "/api/v1/catalog/insurers/" + insurerA).andExpect(status().isOk());
    api.doGet(user, "/api/v1/catalog/insurers/" + insurerB).andExpect(status().isOk());
    api.doGet(user, "/api/v1/booking/si-batches/" + batchB + "/items").andExpect(status().isOk());
  }

  @Test
  void everyNamedRecordTypeHasItsLookup() {
    List<String> unknown =
        mappings.getHandlerMethods().values().stream()
            .flatMap(
                h ->
                    RecordScopeAdvisor.declarations(h.getBeanType(), h.getMethod())
                        .values()
                        .stream())
            .filter(
                d ->
                    !d.type().isEmpty()
                        && !(d.key().isEmpty()
                            ? recordScope.knowsType(d.type())
                            : recordScope.knowsEntity(d.type(), d.key())))
            .map(d -> d.type())
            .distinct()
            .toList();
    assertThat(unknown).isEmpty();
  }
}
