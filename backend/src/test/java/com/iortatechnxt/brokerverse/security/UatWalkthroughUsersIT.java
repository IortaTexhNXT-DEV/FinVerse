package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.collections.seed.UnappliedSeedData;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.SignInPasswords;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

/**
 * The UAT walkthrough users workbook ({@code docs/deliverables/src/programme/uat/uat_users.yaml}):
 * every persona of every BRD that has a UAT sign-in names SIT/UAT users of the seed scripts that
 * are enabled, not locked and hold the persona's active group profile, and the users added for the
 * personas that had none (seed V2520) sign in with the shared password and receive their profile.
 */
@IntegrationTest
class UatWalkthroughUsersIT {

  /** The workbook's personas, from the backend module directory. */
  static final Path FILE =
      Path.of("..", "docs", "deliverables", "src", "programme", "uat", "uat_users.yaml");

  /** Users added for the personas that had no SIT/UAT user, with their group profile. */
  private static final Map<String, String> ADDED =
      Map.of(
          "appsupport", "APP_SUPPORT",
          "dco", "DCO",
          "sbmrules", "SBM_RULE_ADMIN",
          "rnwmaker", "DATA_STEWARD",
          "rnwchecker", "DATA_OWNER",
          "migpm", "MIGRATION_GONOGO");

  /** Users created at start-up of the seed profile (not by the scripts), checked by their class. */
  private static final Set<String> CREATED_AT_START = Set.of(UnappliedSeedData.UPP_HANDLER);

  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mvc;
  @Autowired private SignInPasswords passwords;

  /** One sign-in of a persona: BRD, persona, sign-in ID and group profile code. */
  private record SignIn(String brd, String persona, String user, String role) {}

  @SuppressWarnings("unchecked")
  private static List<SignIn> signIns() throws IOException {
    Map<String, Object> spec = new Yaml().load(Files.readString(FILE));
    List<SignIn> result = new ArrayList<>();
    for (Map<String, Object> brd : (List<Map<String, Object>>) spec.get("brds")) {
      for (Map<String, Object> p : (List<Map<String, Object>>) brd.get("personas")) {
        if (p.containsKey("no_user")) {
          continue;
        }
        for (Object u : (List<Object>) p.get("users")) {
          String id = u instanceof Map<?, ?> m ? (String) m.get("id") : (String) u;
          result.add(
              new SignIn(
                  (String) brd.get("brd"), (String) p.get("persona"), id, (String) p.get("role")));
        }
      }
    }
    return result;
  }

  @Test
  void everyPersonaOfTheWorkbookHasAnEnabledSeedUserWithItsGroupProfile() throws IOException {
    List<SignIn> signIns = signIns();
    assertThat(signIns).hasSizeGreaterThan(150);
    assertThat(CREATED_AT_START).containsExactly("upphandler");
    for (SignIn s : signIns) {
      String what = s.brd() + " " + s.persona() + " " + s.user();
      assertThat(s.role()).as(what).isNotBlank().doesNotStartWith("SIT_INS_");
      if (CREATED_AT_START.contains(s.user())) {
        continue;
      }
      List<Map<String, Object>> user =
          jdbc.queryForList("select enabled, locked from sec_user where username = ?", s.user());
      assertThat(user).as(what).hasSize(1);
      assertThat(user.get(0).get("enabled")).as(what).isEqualTo(true);
      assertThat(user.get(0).get("locked")).as(what).isEqualTo(false);
      Integer held =
          jdbc.queryForObject(
              "select count(*) from sec_user_role ur join sec_user u on u.id = ur.user_id"
                  + " join sec_role r on r.id = ur.role_id"
                  + " where u.username = ? and r.code = ? and r.active",
              Integer.class,
              s.user(),
              s.role());
      assertThat(held).as(what + " holds " + s.role()).isEqualTo(1);
    }
  }

  @Test
  void theUsersAddedForTheMissingPersonasSignInWithTheirGroupProfile() throws Exception {
    List<SignIn> signIns = signIns();
    for (Map.Entry<String, String> e : ADDED.entrySet()) {
      assertThat(signIns)
          .as(e.getKey())
          .anyMatch(s -> s.user().equals(e.getKey()) && s.role().equals(e.getValue()));
      assertThat(
              jdbc.queryForObject(
                  "select must_change_password from sec_user where username = ?",
                  Boolean.class,
                  e.getKey()))
          .as(e.getKey())
          .isFalse();
      mvc.perform(passwords.login(e.getKey()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").isNotEmpty())
          .andExpect(jsonPath("$.user.roles.length()").value(1))
          .andExpect(jsonPath("$.user.roles[0]").value(e.getValue()));
    }
  }
}
