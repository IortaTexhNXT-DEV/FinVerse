package com.iortatechnxt.brokerverse.security.service.directory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class LocalPasswordAuthenticatorTest {

  @Test
  void anAcceptedAndARefusedPasswordAreAnsweredAndTheCheckIsReleased() {
    LocalPasswordAuthenticator authenticator =
        new LocalPasswordAuthenticator(
            auth -> {
              if ("right".equals(auth.getCredentials())) {
                return UsernamePasswordAuthenticationToken.authenticated(
                    auth.getPrincipal(), null, java.util.List.of());
              }
              throw new BadCredentialsException("no");
            });
    // More checks than processors one after the other: each releases its turn.
    for (int i = 0; i < Runtime.getRuntime().availableProcessors() + 2; i++) {
      assertThat(authenticator.authenticate("u", "right".toCharArray()).succeeded()).isTrue();
      DirectoryResult wrong = authenticator.authenticate("u", "wrong".toCharArray());
      assertThat(wrong.succeeded()).isFalse();
      assertThat(wrong.message()).isEqualTo(LocalPasswordAuthenticator.INVALID);
    }
  }
}
