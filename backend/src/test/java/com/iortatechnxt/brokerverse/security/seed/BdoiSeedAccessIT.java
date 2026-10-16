package com.iortatechnxt.brokerverse.security.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BDOI target-state group profiles (V2900), the super user (V2901) and the retirement of the
 * SIT/UAT persona users ({@link SeedDemoUserRetirement}) on the seed data. Every test rolls back.
 */
@IntegrationTest
@Transactional
class BdoiSeedAccessIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private Api api;

  private Set<String> permissionsOf(String role) {
    return new TreeSet<>(
        jdbc.queryForList(
            "select p.permission from sec_role_permission p join sec_role r on r.id = p.role_id"
                + " where r.code = ?",
            String.class,
            role));
  }

  @Test
  void everyTargetStateProfileIsActiveAndHoldsKnownPermissions() {
    List<String> profiles =
        jdbc.queryForList(
            "select code from sec_role where code like 'BDOI\\_%' and code <> ? and active",
            String.class, SeedSuperUser.PROFILE);
    assertThat(profiles).hasSize(51).contains("BDOI_MKT_UNIT_HEAD", "BDOI_BUSINESS_SYS_ADMIN");
    Set<String> known =
        Arrays.stream(Permission.values()).map(Enum::name).collect(Collectors.toSet());
    for (String profile : profiles) {
      assertThat(permissionsOf(profile)).as(profile).contains("DASHBOARD_VIEW").isSubsetOf(known);
    }
    assertThat(permissionsOf("BDOI_ADJ_TEAM_HEAD")).contains("ADJ_PROCESS", "ADJ_APPROVE");
    assertThat(permissionsOf("BDOI_ADJ_OFFICER"))
        .contains("ADJ_PROCESS")
        .doesNotContain("ADJ_APPROVE");
    assertThat(permissionsOf("BDOI_MKT_OFFICER"))
        .contains("SCR_INVESTIGATE")
        .doesNotContain("SCR_LIST_APPROVE", "SCR_COMMITTEE");
  }

  @Test
  void theSuperUserHoldsEveryPermissionAndTheAdministrationRoles() throws Exception {
    // Every permission of the target-state profiles and of the system administration; the seed
    // runner SeedSuperUser adds the permissions created after V2901 at the start of a seed stack.
    Set<String> expected =
        new TreeSet<>(
            jdbc.queryForList(
                "select distinct p.permission from sec_role_permission p"
                    + " join sec_role r on r.id = p.role_id"
                    + " where r.code like 'BDOI\\_%' or r.code in ('SYSADMIN', 'UAM_ADMINISTRATOR')",
                String.class));
    assertThat(permissionsOf(SeedSuperUser.PROFILE)).hasSizeGreaterThan(250).containsAll(expected);
    JsonNode me = api.read(api.doGet(SeedSuperUser.USERNAME, "/api/v1/auth/me"));
    Set<String> roles = new TreeSet<>();
    me.get("roles").forEach(n -> roles.add(n.asText()));
    assertThat(roles)
        .contains(SeedSuperUser.PROFILE, "SYSADMIN", "UAM_ADMINISTRATOR", "UAM_APPROVER");
    Set<String> permissions = new TreeSet<>();
    me.get("permissions").forEach(n -> permissions.add(n.asText()));
    assertThat(permissions).contains("USER_MANAGE", "ROLE_MANAGE", "ACCESS_APPROVE");
  }

  @Test
  void retirementDisablesThePersonaUsersOfTheSeedScriptsOnly() {
    jdbc.update(
        "insert into sec_user (username, full_name, password_hash, created_at, created_by)"
            + " values ('retire_it_joiner', 'Joiner', 'x', now(), 'SYSTEM')");
    jdbc.update(
        "insert into sec_access_change_log (occurred_at, subject_type, subject, activity,"
            + " attribute, to_value, done_by) values (now(), 'USER', 'retire_it_joiner',"
            + " 'CREATE_USER', 'enabled', 'true', 'SYSTEM')");

    new SeedDemoUserRetirement(jdbc, false).run(new DefaultApplicationArguments());
    assertThat(enabled("uamadmin")).isTrue();

    new SeedDemoUserRetirement(jdbc, true).run(new DefaultApplicationArguments());
    assertThat(enabled("uamadmin")).isFalse();
    assertThat(enabled("clmofficer")).isFalse();
    assertThat(enabled(SeedSuperUser.USERNAME)).isTrue();
    assertThat(enabled("retire_it_joiner")).isTrue();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sec_access_change_log where subject = 'uamadmin'"
                    + " and activity = 'DISABLE_USER'",
                Integer.class))
        .isEqualTo(1);
  }

  private boolean enabled(String username) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select enabled from sec_user where lower(username) = lower(?)",
            Boolean.class,
            username));
  }
}
