package com.iortatechnxt.brokerverse.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The provisioning interface refuses what does not fit the agreed SCIM User before recording
 * anything, answers every error in the SCIM error schema without internals, and answers a repeated
 * event with the earlier result without changing anything twice.
 */
@IntegrationTest
class ScimHardeningIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 10_000_000L);
  private static final String USERS = "/integration/v1/scim/v2/Users";
  private static final String SCIM = "application/scim+json";
  private static final String ERROR = "urn:ietf:params:scim:api:messages:2.0:Error";
  private static final String EXTENSION = "urn:bibs:params:scim:schemas:extension:bdo:2.0:User";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;

  private static String userId() {
    return "a" + String.format("%09d", 800_000_000L + IDS.incrementAndGet() % 99_999_999L);
  }

  private static MockHttpServletRequestBuilder uidm(MockHttpServletRequestBuilder request) {
    return request.with(
        jwt()
            .jwt(j -> j.subject("uidm-isc"))
            .authorities(new SimpleGrantedAuthority("SCOPE_identity.provision")));
  }

  private static Map<String, Object> user(String userId) {
    Map<String, Object> user = new HashMap<>();
    user.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
    user.put("userName", userId);
    user.put("name", Map.of("givenName", "Maria", "familyName", "Santos"));
    user.put("displayName", "Maria Santos");
    user.put("emails", List.of(Map.of("value", userId + "@bdo.test", "primary", true)));
    user.put("active", true);
    user.put(
        EXTENSION,
        Map.of("windowsId", "BDO\\h" + userId, "uidmRequestNo", "UIDM-H-" + userId.substring(4)));
    return user;
  }

  private int events(String userId) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from idn_identity_event where user_id = ?", Integer.class, userId);
    return n == null ? 0 : n;
  }

  @Test
  void anAttributeOutsideTheSchemaOrAValueThatDoesNotFitIsRefusedBeforeAnythingIsRecorded()
      throws Exception {
    String userId = userId();
    Map<String, Object> extra = user(userId);
    extra.put("password", "secret");
    mvc.perform(uidm(post(USERS)).contentType(SCIM).content(json.writeValueAsString(extra)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.schemas[0]").value(ERROR))
        .andExpect(jsonPath("$.scimType").value("invalidValue"))
        .andExpect(jsonPath("$.detail").value("Attribute password is not supported"));

    Map<String, Object> tooLong = user(userId);
    tooLong.put("displayName", "x".repeat(121));
    mvc.perform(uidm(post(USERS)).contentType(SCIM).content(json.writeValueAsString(tooLong)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("displayName is longer than 120 characters"));

    Map<String, Object> badId = user(userId);
    badId.put("userName", "a1 or 1=1");
    mvc.perform(uidm(post(USERS)).contentType(SCIM).content(json.writeValueAsString(badId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.schemas[0]").value(ERROR));

    assertThat(events(userId)).isZero();
  }

  @Test
  void anUnreadableBodyGetsTheScimErrorWithoutInternals() throws Exception {
    String body =
        mvc.perform(uidm(post(USERS)).contentType(SCIM).content("{\"userName\": "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.schemas[0]").value(ERROR))
            .andExpect(jsonPath("$.scimType").value("invalidSyntax"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).doesNotContain("Exception").doesNotContain("at com.");

    mvc.perform(
            uidm(patch(USERS + "/a000000001"))
                .contentType(SCIM)
                .content("{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Operations is not a list of operations"));
  }

  @Test
  void aRepeatedEventGetsTheSameAnswerAndChangesNothingTwice() throws Exception {
    String userId = userId();
    String body = json.writeValueAsString(user(userId));
    String first =
        mvc.perform(uidm(post(USERS)).contentType(SCIM).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int recorded = events(userId);
    String again =
        mvc.perform(uidm(post(USERS)).contentType(SCIM).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readTree(again)).isEqualTo(json.readTree(first));
    assertThat(events(userId)).isEqualTo(recorded);

    Map<String, Object> changed = user(userId);
    changed.put("displayName", "Maria Santos-Reyes");
    mvc.perform(
            uidm(put(USERS + "/" + userId))
                .contentType(SCIM)
                .content(json.writeValueAsString(changed)))
        .andExpect(status().isOk());
    assertThat(events(userId)).isEqualTo(recorded + 1);
  }
}
