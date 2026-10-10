package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PersonaMenus;
import com.iortatechnxt.brokerverse.support.PersonaMenus.Persona;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Persona menus of BRD-10, BRD-11, BRD-7 and BRD-8 (client requirement 14) on the server side of
 * the shared file {@code frontend/src/navigation/personaMenus.json}: each role holds exactly the
 * permissions the web client's menu is computed from (V1050, V1060, V1062, V1063; Claims V1020;
 * Employee Benefits V1030), within its permission scope for the Marketing roles, its SIT/UAT user
 * holds only that role, and every screen the role sees answers its SIT/UAT user (no menu entry
 * opens on a refusal).
 */
@IntegrationTest
class PersonaMenusIT {

  /** The read the screen makes when it opens ({c} = the seed company). */
  private static final Map<String, String> SCREEN_READS =
      Map.ofEntries(
          Map.entry("/screening", "/api/v1/screening/cases/tiles?companyId={c}"),
          Map.entry("/screening/cases", "/api/v1/screening/cases?companyId={c}"),
          Map.entry("/screening/matches", "/api/v1/screening/matches?companyId={c}"),
          Map.entry("/screening/runs", "/api/v1/screening/runs?companyId={c}"),
          Map.entry("/screening/high-risk", "/api/v1/screening/high-risk-clients?companyId={c}"),
          Map.entry("/screening/str", "/api/v1/screening/str?companyId={c}"),
          Map.entry(
              "/screening-setup/config",
              "/api/v1/screening/config/versions?companyId={c}&type=MATCH_CRITERIA"),
          Map.entry(
              "/screening-setup/templates",
              "/api/v1/screening/config/versions?companyId={c}&type=TEMPLATE"),
          Map.entry("/screening-setup/watchlist", "/api/v1/screening/watchlist/entries"),
          Map.entry("/screening-setup/sources", "/api/v1/screening/watchlist/sources"),
          Map.entry("/user-access/requests", "/api/v1/nbadmin/access-requests"),
          Map.entry(
              "/user-access/group-profiles", "/api/v1/nbadmin/access-requests?groupProfiles=true"),
          Map.entry("/user-access/bulk", "/api/v1/nbadmin/access-batches"),
          Map.entry("/user-access/matrix", "/api/v1/nbadmin/access-matrix"),
          Map.entry("/user-access/sod-rules", "/api/v1/nbadmin/sod-rules"),
          Map.entry("/user-access/reports", "/api/v1/reports"),
          Map.entry("/claims-handling", "/api/v1/broker-claims/home?companyId={c}"),
          Map.entry("/claims-handling/worklist", "/api/v1/broker-claims/worklist?companyId={c}"),
          Map.entry("/claims-handling/new", "/api/v1/broker-claims/covers?companyId={c}&q=ARN"),
          Map.entry("/claims-handling/covers", "/api/v1/broker-claims/covers?companyId={c}&q=ARN"),
          Map.entry("/claims-handling/diary", "/api/v1/broker-claims/diary/mine?companyId={c}"),
          Map.entry(
              "/claims-handling/location-refs",
              "/api/v1/broker-claims/location-refs?companyId={c}"),
          Map.entry("/claims-handling/reports", "/api/v1/reports"),
          Map.entry("/claims-handling/setup", "/api/v1/broker-claims/setup/matrix"),
          Map.entry("/eb", "/api/v1/eb/home?companyId={c}"),
          Map.entry("/eb/programmes", "/api/v1/eb/programmes?companyId={c}"),
          Map.entry("/eb/programmes/new", "/api/v1/eb/account-officers"),
          Map.entry("/eb/member-changes", "/api/v1/eb/member-changes?companyId={c}"),
          Map.entry("/eb/pending-items", "/api/v1/eb/pending-items?companyId={c}"),
          Map.entry("/eb/soa", "/api/v1/eb/soa?companyId={c}"),
          Map.entry("/eb/setup", "/api/v1/eb/setup/threshold-rules?companyId={c}"),
          Map.entry("/migration", "/api/v1/migration/home?companyId={c}"),
          Map.entry("/migration/objects", "/api/v1/migration/objects"),
          Map.entry("/migration/maps", "/api/v1/migration/maps"),
          Map.entry("/migration/layouts", "/api/v1/migration/layouts"),
          Map.entry("/migration/extracts", "/api/v1/migration/extracts?companyId={c}"),
          Map.entry("/migration/batches", "/api/v1/migration/batches?companyId={c}"),
          Map.entry("/migration/matching", "/api/v1/migration/matches"),
          Map.entry("/migration/reconciliation", "/api/v1/migration/batches?companyId={c}"),
          Map.entry("/migration/trueups", "/api/v1/migration/trueups?companyId={c}"),
          Map.entry("/migration/cutover", "/api/v1/migration/cutover/plans?companyId={c}"),
          Map.entry("/migration/runoff", "/api/v1/migration/decommission?companyId={c}"),
          Map.entry("/migration/signoff", "/api/v1/migration/signoffs/matrix?companyId={c}"),
          Map.entry("/legacy-inquiry", "/api/v1/legacy-inquiry/settings"),
          Map.entry(
              "/legacy-inquiry/access-log", "/api/v1/legacy-inquiry/access-log?companyId={c}"),
          Map.entry("/renewal", "/api/v1/renewal/home?companyId={c}"),
          Map.entry("/renewal/dashboard", "/api/v1/renewal/dashboard?companyId={c}"),
          Map.entry(
              "/renewal/accounts", "/api/v1/renewal/candidates?companyId={c}&tab=BUCKET_CLEAN"),
          Map.entry(
              "/renewal/processing-dashboard",
              "/api/v1/renewal/processing-dashboard?companyId={c}"),
          Map.entry("/renewal/budget", "/api/v1/renewal/budgets?companyId={c}&fiscalYear=2027"),
          Map.entry("/renewal/audit-logs", "/api/v1/renewal/audit-logs?companyId={c}"),
          Map.entry("/renewal/expiry", "/api/v1/renewal/candidates?companyId={c}&tab=EXTRACTED"),
          Map.entry("/renewal/mine", "/api/v1/renewal/candidates?companyId={c}&mine=true"),
          Map.entry("/renewal/review", "/api/v1/renewal/candidates?companyId={c}&tab=REVIEW"),
          Map.entry("/renewal/transfers", "/api/v1/renewal/transfers/incoming?companyId={c}"),
          Map.entry("/renewal/referrals", "/api/v1/renewal/referrals?companyId={c}"),
          Map.entry("/renewal/rmu-officers", "/api/v1/renewal/rmu-officers?companyId={c}"),
          Map.entry("/renewal/channels", "/api/v1/renewal/channels?companyId={c}"),
          Map.entry(
              "/renewal/placement",
              "/api/v1/renewal/candidates?companyId={c}&stage=FOR_PLACEMENT_BOOKING"),
          Map.entry("/renewal/epolicies", "/api/v1/renewal/epolicy/receipts?companyId={c}"),
          Map.entry(
              "/renewal/approvals", "/api/v1/renewal/candidates?companyId={c}&stage=ACCEPTED"),
          Map.entry("/renewal/billing", "/api/v1/renewal/billing-files?companyId={c}"),
          Map.entry(
              "/renewal/tsu-requests",
              "/api/v1/renewal/tsu-requests?companyId={c}&status=PENDING_TL_APPROVAL"),
          Map.entry("/renewal/kyc", "/api/v1/renewal/kyc/dashboard?companyId={c}"),
          Map.entry(
              "/renewal/risk-codes", "/api/v1/renewal/setup/risk-code-maintenance?companyId={c}"),
          Map.entry(
              "/renewal/processing", "/api/v1/renewal/candidates?companyId={c}&tab=FOR_PROCESSING"),
          Map.entry("/renewal/insurer", "/api/v1/renewal/insurer-batches?companyId={c}"),
          Map.entry("/renewal/letters", "/api/v1/renewal/candidates?companyId={c}&tab=RA_READY"),
          Map.entry("/renewal/followups", "/api/v1/renewal/candidates?companyId={c}&tab=RA_SENT"),
          Map.entry("/renewal/lamd", "/api/v1/renewal/lamd-reports?companyId={c}"),
          Map.entry("/renewal/setup", "/api/v1/renewal/setup/package-map?companyId={c}"),
          Map.entry("/submitted", "/api/v1/submitted/home?companyId={c}"),
          Map.entry("/submitted/masterlist", "/api/v1/submitted/policies?companyId={c}"),
          Map.entry("/submitted/intake", "/api/v1/submitted/intake-runs?companyId={c}"),
          Map.entry("/submitted/extractions", "/api/v1/submitted/extractions?companyId={c}"),
          Map.entry("/submitted/runs", "/api/v1/submitted/runs?companyId={c}"),
          Map.entry(
              "/submitted/reviews", "/api/v1/submitted/iaaf?companyId={c}&status=FOR_APPROVAL"),
          Map.entry("/submitted/tors", "/api/v1/submitted/tors?companyId={c}&status=FOR_APPROVAL"),
          Map.entry("/submitted/renewals", "/api/v1/submitted/renewals?companyId={c}"),
          Map.entry("/submitted/proposals", "/api/v1/submitted/proposals/batches?companyId={c}"),
          Map.entry("/submitted/letters", "/api/v1/submitted/letters?companyId={c}&status=FAILED"),
          Map.entry(
              "/submitted/fees", "/api/v1/submitted/handling-fees?companyId={c}&status=BILLED"),
          Map.entry("/submitted/no-touch", "/api/v1/submitted/no-touch?companyId={c}"),
          Map.entry("/submitted/setup", "/api/v1/submitted/setup/rule-sets?companyId={c}"),
          Map.entry("/csf", "/api/v1/csf/search?companyId={c}&keyType=NAME&q=Santos"),
          Map.entry("/csf/changes", "/api/v1/csf/contact-changes?companyId={c}"),
          Map.entry("/csf/reports", "/api/v1/reports"));

  @Autowired private Api api;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TestData data;

  private Set<String> granted(String role) {
    return new TreeSet<>(
        jdbc.queryForList(
            "select p.permission from sec_role_permission p join sec_role r on r.id = p.role_id"
                + " where r.code = ? and r.active",
            String.class,
            role));
  }

  @Test
  void everyRoleHoldsExactlyThePermissionsTheMenuIsBuiltFrom() throws Exception {
    Map<String, Persona> personas = PersonaMenus.load();
    assertThat(personas).hasSize(48);
    for (Persona p : personas.values()) {
      assertThat(p.inScope(granted(p.role()))).as(p.role()).isEqualTo(p.permissions());
      JsonNode me = api.read(api.doGet(p.seedUser(), "/api/v1/auth/me"));
      assertThat(me.get("roles").size()).as(p.seedUser()).isEqualTo(1);
      assertThat(me.get("roles").get(0).asText()).isEqualTo(p.role());
      Set<String> permissions = new TreeSet<>();
      me.get("permissions").forEach(n -> permissions.add(n.asText()));
      assertThat(p.inScope(permissions)).as(p.seedUser()).isEqualTo(p.permissions());
    }
  }

  @Test
  void everyScreenOfAPersonaOpensForItsSeedUser() throws Exception {
    String company = data.company().getId().toString();
    for (Persona p : PersonaMenus.load().values()) {
      for (String screen : p.screens()) {
        assertThat(SCREEN_READS).as(screen).containsKey(screen);
        api.doGet(p.seedUser(), SCREEN_READS.get(screen).replace("{c}", company))
            .andExpect(status().isOk());
      }
    }
  }

  @Test
  void marketingReachesTheClaimsReportsOnlyAndTheInsurerClaimsAreGone() throws Exception {
    String company = data.company().getId().toString();
    for (String user : new String[] {"ao", "mkttl"}) {
      Map<String, Boolean> claimsReports = new TreeMap<>();
      api.read(api.doGet(user, "/api/v1/reports"))
          .forEach(
              e -> {
                if ("CLAIMS_HANDLING".equals(e.get("category").asText())) {
                  claimsReports.put(e.get("code").asText(), e.get("exportable").asBoolean());
                }
              });
      assertThat(claimsReports)
          .as(user)
          .containsKeys("BCL-OUTSTANDING", "BCL-LOSS-EXPERIENCE", "BCL-LOSS-RATIO")
          .doesNotContainKey("BCL-DATA-EXTRACT");
      assertThat(claimsReports.get("BCL-LOSS-EXPERIENCE")).as(user).isEqualTo("mkttl".equals(user));
      api.doGet(user, "/api/v1/broker-claims/worklist?companyId=" + company)
          .andExpect(status().isForbidden());
    }
    for (String user : new String[] {"clmofficer", "clmth", "clmuh", "clmrisk"}) {
      api.doGet(user, "/api/v1/claims?companyId=" + company).andExpect(status().isNotFound());
    }
  }

  @Test
  void theUserAccessMatrixIsNotOfferedToRolesItRefuses() throws Exception {
    // The matrix used to be in the menu of every UAM_VIEW holder, but its endpoint refuses the
    // Requestor and the Second Approver: the entry is now shown to the endpoint's permissions only.
    for (String user : new String[] {"requestor", "secapprover"}) {
      api.doGet(user, "/api/v1/nbadmin/access-matrix").andExpect(status().isForbidden());
    }
    api.doGet("uamapprover", "/api/v1/nbadmin/access-matrix").andExpect(status().isOk());
  }
}
