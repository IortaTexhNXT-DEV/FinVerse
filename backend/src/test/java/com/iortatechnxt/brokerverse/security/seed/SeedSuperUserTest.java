package com.iortatechnxt.brokerverse.security.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SeedSuperUserTest {

  private static final String ENVIRONMENT_PASSWORD = "Super#" + System.nanoTime() + "aZ";

  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

  @Test
  void theSuperUserGetsThePasswordOfTheEnvironmentWithoutAChangeAtFirstSignIn() {
    when(jdbc.queryForList(anyString(), eq(String.class), eq(SeedSuperUser.PROFILE)))
        .thenReturn(List.of());

    new SeedSuperUser(jdbc, encoder, ENVIRONMENT_PASSWORD).run(new DefaultApplicationArguments());

    ArgumentCaptor<Object> hash = ArgumentCaptor.forClass(Object.class);
    verify(jdbc)
        .update(
            anyString(), hash.capture(), eq(SeedSuperUser.USERNAME), eq(SeedPasswords.SCRIPT_HASH));
    assertThat(encoder.matches(ENVIRONMENT_PASSWORD, (String) hash.getValue())).isTrue();
  }

  @Test
  void aPasswordBreakingThePasswordRulesIsNotUsed() {
    when(jdbc.queryForList(anyString(), eq(String.class), eq(SeedSuperUser.PROFILE)))
        .thenReturn(List.of());

    new SeedSuperUser(jdbc, encoder, "Short@1").run(new DefaultApplicationArguments());
    new SeedSuperUser(jdbc, encoder, " ").run(new DefaultApplicationArguments());

    verify(jdbc, never()).update(anyString(), any(), any(), any());
    assertThat(SeedSuperUser.followsPasswordRules("nouppercase#2026")).isFalse();
    assertThat(SeedSuperUser.followsPasswordRules("Long-Enough#2026")).isTrue();
  }

  @Test
  void theProfileIsGivenThePermissionsItLacks() {
    when(jdbc.queryForList(anyString(), eq(String.class), eq(SeedSuperUser.PROFILE)))
        .thenReturn(List.of("USER_MANAGE"));

    new SeedSuperUser(jdbc, encoder, "").run(new DefaultApplicationArguments());

    verify(jdbc).update(anyString(), eq("ROLE_MANAGE"), eq(SeedSuperUser.PROFILE));
    verify(jdbc, never()).update(anyString(), eq("USER_MANAGE"), eq(SeedSuperUser.PROFILE));
  }
}
