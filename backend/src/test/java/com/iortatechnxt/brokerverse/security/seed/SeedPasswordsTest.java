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

class SeedPasswordsTest {

  private static final String ENVIRONMENT_PASSWORD = "Env#" + System.nanoTime() + "aZ";

  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

  @Test
  void theUsersOfTheSeedScriptsGetThePasswordOfTheEnvironment() {
    when(jdbc.queryForList(anyString(), eq(String.class), eq(SeedPasswords.SCRIPT_HASH)))
        .thenReturn(List.of("auditor", "badmin"));
    when(jdbc.update(anyString(), any(), any(), any())).thenReturn(2);

    new SeedPasswords(jdbc, encoder, ENVIRONMENT_PASSWORD, true)
        .run(new DefaultApplicationArguments());

    ArgumentCaptor<Object> hash = ArgumentCaptor.forClass(Object.class);
    verify(jdbc).update(anyString(), hash.capture(), eq(true), eq(SeedPasswords.SCRIPT_HASH));
    assertThat(encoder.matches(ENVIRONMENT_PASSWORD, (String) hash.getValue())).isTrue();
  }

  @Test
  void withoutAnEnvironmentPasswordOrSeedUsersNothingChanges() {
    when(jdbc.queryForList(anyString(), eq(String.class), eq(SeedPasswords.SCRIPT_HASH)))
        .thenReturn(List.of("auditor"));
    new SeedPasswords(jdbc, encoder, " ", false).run(new DefaultApplicationArguments());

    when(jdbc.queryForList(anyString(), eq(String.class), eq(SeedPasswords.SCRIPT_HASH)))
        .thenReturn(List.of());
    new SeedPasswords(jdbc, encoder, ENVIRONMENT_PASSWORD, false)
        .run(new DefaultApplicationArguments());

    verify(jdbc, never()).update(anyString(), any(), any(), any());
  }
}
