package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/** The user directory gives every signed-in user the display names shown instead of login ids. */
@IntegrationTest
class UserDirectoryIT {

  @Autowired private MockMvc mvc;
  @Autowired private UserDirectory users;

  @Test
  void generatedDocumentsNameUsersByTheirDisplayName() {
    assertThat(users.displayName("ao")).isNotBlank().isNotEqualTo("ao");
    assertThat(users.displayName("AO")).isEqualTo(users.displayName("ao"));
    assertThat(users.displayName("nobody-known")).isEqualTo("nobody-known");
    assertThat(users.displayName(null)).isNull();
  }

  @Test
  @WithUserDetails("accountant")
  void listsDisplayNamesForAnySignedInUser() throws Exception {
    mvc.perform(get("/api/v1/users/directory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.username == 'accountant')].displayName").isNotEmpty())
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
  }

  @Test
  void refusesAnonymousCallers() throws Exception {
    mvc.perform(get("/api/v1/users/directory")).andExpect(status().isUnauthorized());
  }
}
