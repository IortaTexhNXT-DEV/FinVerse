package com.iortatechnxt.brokerverse.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.catalog.service.version.CatalogCaches;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.lov.domain.LovDetails;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovCaches;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.organization.service.OrganizationCaches;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.RolePermissionLookup;
import com.iortatechnxt.brokerverse.security.service.SecurityCaches;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import com.iortatechnxt.brokerverse.system.domain.SystemParameterRepository;
import com.iortatechnxt.brokerverse.system.service.SystemCaches;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The reference-data caches (in-memory store of the test profile): reads are cached and every write
 * path, through the owning service or directly through JPA, clears the cache at once.
 */
@IntegrationTest
class ReferenceDataCacheIT {

  private static final AtomicInteger SEQ = new AtomicInteger();
  private static final String ADMIN = "admin";

  @Autowired private CacheManager caches;
  @Autowired private SystemParameterService parameters;
  @Autowired private SystemParameterRepository parameterRepository;
  @Autowired private LovService lovs;
  @Autowired private UserAdminService userAdmin;
  @Autowired private RolePermissionLookup rolePermissions;
  @Autowired private OrganizationDirectory directory;
  @Autowired private CompanyRepository companies;
  @Autowired private TestCompanies testCompanies;
  @Autowired private ProductVersionQueryService versions;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;
  @Autowired private Api api;

  @Test
  void aParameterChangeShowsAtOnceThroughTheServiceAndThroughJpa() {
    String key = SystemParameterService.JOB_HISTORY_DAYS;
    String before = parameters.text(key, "90");
    try {
      assertThat(cache(SystemCaches.PARAMETERS).get(key)).isNotNull();
      as.run(ADMIN, () -> parameters.update(key, "45"));
      assertThat(cache(SystemCaches.PARAMETERS).get(key)).isNull();
      assertThat(parameters.intValue(key, 0)).isEqualTo(45);

      tx.executeWithoutResult(
          s -> parameterRepository.findByKey(key).orElseThrow().changeValue("46"));
      assertThat(cache(SystemCaches.PARAMETERS).get(key)).isNull();
      assertThat(parameters.intValue(key, 0)).isEqualTo(46);
    } finally {
      as.run(ADMIN, () -> parameters.update(key, before));
    }
    assertThat(parameters.text(key, null)).isEqualTo(before);
  }

  @Test
  void aListValueChangeShowsAtOnce() {
    String code = "CACHE_" + SEQ.incrementAndGet() + "_" + System.nanoTime() % 100_000;
    LocalDate from = LocalDate.of(2020, 1, 1);
    LovValue value =
        as.run(
            ADMIN,
            () -> lovs.create("VOID_REASON", code, new LovDetails("First", 90, null, from, null)));
    assertThat(lovs.label("VOID_REASON", code)).isEqualTo("First");
    assertThat(cache(LovCaches.VALUES).get("VOID_REASON")).isNotNull();

    as.run(ADMIN, () -> lovs.update(value.getId(), new LovDetails("Second", 90, null, from, null)));
    assertThat(cache(LovCaches.VALUES).get("VOID_REASON")).isNull();
    assertThat(lovs.label("VOID_REASON", code)).isEqualTo("Second");
    assertThat(lovs.options("VOID_REASON", LocalDate.of(2026, 1, 1)))
        .noneMatch(o -> o.code().equals(code)); // pending authorization: not usable yet
  }

  @Test
  void aRoleGrantShowsAtOnce() {
    String code = "CACHE_ROLE_" + SEQ.incrementAndGet() + "_" + System.nanoTime() % 100_000;
    Role role =
        as.run(
            ADMIN,
            () ->
                userAdmin.createRole(
                    new RoleRequest(code, "Cache test", Set.of(Permission.REPORT_VIEW))));
    assertThat(rolePermissions.permissionsOf(code).permissions()).containsExactly("REPORT_VIEW");
    assertThat(cache(SecurityCaches.ROLE_PERMISSIONS).get(code)).isNotNull();

    as.run(
        ADMIN,
        () ->
            userAdmin.updateRole(
                role.getId(),
                new RoleRequest(
                    code, "Cache test", Set.of(Permission.REPORT_VIEW, Permission.AUDIT_VIEW))));
    assertThat(rolePermissions.permissionsOf(code).permissions())
        .containsExactly("AUDIT_VIEW", "REPORT_VIEW");
  }

  @Test
  void aCompanyChangeThroughJpaClearsTheOrganisationCache() {
    Company company =
        testCompanies.create("CC" + (SEQ.incrementAndGet() + System.nanoTime() % 1000), "PHP");
    assertThat(directory.company(company.getId()).name()).isEqualTo(company.getName());
    assertThat(directory.companyByCode(company.getCode()).id()).isEqualTo(company.getId());
    assertThat(cache(OrganizationCaches.UNITS).get("company:" + company.getId())).isNotNull();

    tx.executeWithoutResult(
        s -> companies.findById(company.getId()).orElseThrow().setName("Renamed"));
    assertThat(cache(OrganizationCaches.UNITS).get("company:" + company.getId())).isNull();
    assertThat(directory.company(company.getId()).name()).isEqualTo("Renamed");
    assertThat(directory.branchesOf(company.getId())).isNotNull();
  }

  @Test
  void theCatalogCacheIsNotUsedInsideReadWriteTransactions() {
    String key = "version:NO-SUCH-PRODUCT:1";
    cache(CatalogCaches.PRODUCT_VERSIONS).evict(key);
    tx.executeWithoutResult(s -> assertThat(versions.version("NO-SUCH-PRODUCT", 1)).isEmpty());
    assertThat(cache(CatalogCaches.PRODUCT_VERSIONS).get(key)).isNull();
    assertThat(versions.version("NO-SUCH-PRODUCT", 1)).isEmpty();
    assertThat(cache(CatalogCaches.PRODUCT_VERSIONS).get(key)).isNotNull();
  }

  @Test
  void administratorsListAndFlushTheCaches() throws Exception {
    api.doGet(ADMIN, "/api/v1/admin/caches")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(jsonPath("$[?(@.name == 'lov-values')].store").value("IN_MEMORY"));
    parameters.intValue(SystemParameterService.JOB_HISTORY_DAYS, 0);
    api.doPost(ADMIN, "/api/v1/admin/caches/system-parameters/clear", null)
        .andExpect(status().isNoContent());
    assertThat(cache(SystemCaches.PARAMETERS).get(SystemParameterService.JOB_HISTORY_DAYS))
        .isNull();
    api.doPost(ADMIN, "/api/v1/admin/caches/clear", null).andExpect(status().isNoContent());
    api.doPost(ADMIN, "/api/v1/admin/caches/no-such-cache/clear", null)
        .andExpect(status().isNotFound());
    api.doGet("accountant", "/api/v1/admin/caches").andExpect(status().isForbidden());
  }

  private Cache cache(String name) {
    return caches.getCache(name);
  }
}
