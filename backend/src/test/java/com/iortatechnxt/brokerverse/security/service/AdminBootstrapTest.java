package com.iortatechnxt.brokerverse.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapTest {

  private static final Instant NOW = Instant.parse("2026-09-28T01:00:00Z");

  private final AppUserRepository users = mock(AppUserRepository.class);
  private final RoleRepository roles = mock(RoleRepository.class);
  private final PasswordEncoder encoder = mock(PasswordEncoder.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void theFirstAdministratorMustChangeThePasswordAndItAgesFromNow() {
    when(users.count()).thenReturn(0L);
    when(roles.findByCodeIn(List.of("SYSADMIN"))).thenReturn(List.of());
    when(encoder.encode(any())).thenReturn("hash");

    new AdminBootstrap(users, roles, encoder, "firstadmin", "one-time-value", clock).run(null);

    ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
    verify(users).save(saved.capture());
    assertThat(saved.getValue().isMustChangePassword()).isTrue();
    assertThat(saved.getValue().getPasswordChangedAt()).isEqualTo(NOW);
    assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");
  }

  @Test
  void nothingIsCreatedWithoutAnInitialPasswordOrOnAnInstalledSystem() {
    when(users.count()).thenReturn(0L);
    new AdminBootstrap(users, roles, encoder, "firstadmin", "", clock).run(null);
    when(users.count()).thenReturn(4L);
    new AdminBootstrap(users, roles, encoder, "firstadmin", "one-time-value", clock).run(null);

    verify(users, never()).save(any());
  }
}
