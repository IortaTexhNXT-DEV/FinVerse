package com.iortatechnxt.brokerverse.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.security.service.directory.AuthMode;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryAuthenticator;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryAuthenticators;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryResult;
import com.iortatechnxt.brokerverse.security.service.directory.DirectoryResult.Outcome;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The directory port (FR-UA-003): the adapter of the mode answers; a missing one refuses. */
class DirectoryAuthenticatorsTest {

  /** A stand-in directory that knows one Windows ID and password. */
  private static final DirectoryAuthenticator STUB_DIRECTORY =
      new DirectoryAuthenticator() {
        @Override
        public AuthMode mode() {
          return AuthMode.DIRECTORY;
        }

        @Override
        public DirectoryResult authenticate(String userId, char[] password) {
          if (!"BDO\\a013000101".equals(userId)) {
            return DirectoryResult.locked("Account disabled in EUA");
          }
          return "Network#2026".equals(new String(password))
              ? DirectoryResult.success()
              : DirectoryResult.invalid("EUA: wrong password");
        }
      };

  @Test
  void theModeParameterIsReadLeniently() {
    assertThat(AuthMode.of(" directory ")).isEqualTo(AuthMode.DIRECTORY);
    assertThat(AuthMode.of("LOCAL")).isEqualTo(AuthMode.LOCAL);
    assertThat(AuthMode.of(null)).isEqualTo(AuthMode.LOCAL);
    assertThat(AuthMode.of("LDAP")).isEqualTo(AuthMode.LOCAL);
  }

  @Test
  void theAdapterOfTheModeAnswersWithItsMessage() {
    DirectoryAuthenticators authenticators = new DirectoryAuthenticators(List.of(STUB_DIRECTORY));
    char[] password = "Network#2026".toCharArray();
    assertThat(authenticators.authenticate(AuthMode.DIRECTORY, "BDO\\a013000101", password))
        .extracting(DirectoryResult::succeeded)
        .isEqualTo(true);
    assertThat(password).containsOnly('\0');
    assertThat(
            authenticators.authenticate(
                AuthMode.DIRECTORY, "BDO\\a013000101", "wrong".toCharArray()))
        .isEqualTo(DirectoryResult.invalid("EUA: wrong password"));
    assertThat(authenticators.authenticate(AuthMode.DIRECTORY, "other", new char[0]).outcome())
        .isEqualTo(Outcome.LOCKED);
    assertThat(authenticators.available(AuthMode.LOCAL)).isFalse();
  }

  @Test
  void aModeWithoutAdapterRefusesWithAServiceMessage() {
    DirectoryAuthenticators none = new DirectoryAuthenticators(List.of());
    DirectoryResult result = none.authenticate(AuthMode.DIRECTORY, "BDO\\x", "p".toCharArray());
    assertThat(result.outcome()).isEqualTo(Outcome.ERROR);
    assertThat(result.message()).isEqualTo(DirectoryAuthenticators.NOT_AVAILABLE);
    assertThat(none.available(AuthMode.DIRECTORY)).isFalse();
  }
}
