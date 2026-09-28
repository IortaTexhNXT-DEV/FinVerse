package com.iortatechnxt.brokerverse.support;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Passwords for tests that sign in through {@code /api/v1/auth/login} as a SIT/UAT user of the seed
 * data. The SIT/UAT password is never written in the test sources: each test run draws a random
 * password and gives it to the users it signs in as, so the tests work in any build without a
 * secret. The other attributes of the user (lock, must-change, password age) are left as they are.
 */
@Component
public class SignInPasswords {

  /** The password of this test run, shared by every user signed in through this helper. */
  private static final String RUN_PASSWORD = newPassword();

  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private String hash;

  SignInPasswords(JdbcTemplate jdbc, PasswordEncoder encoder) {
    this.jdbc = jdbc;
    this.encoder = encoder;
  }

  /**
   * Gives a user the password of this test run.
   *
   * @param username SIT/UAT user of the seed data
   * @return the password to sign in with
   */
  public synchronized String of(String username) {
    if (hash == null) {
      hash = encoder.encode(RUN_PASSWORD);
    }
    int updated =
        jdbc.update(
            "update sec_user set password_hash = ? where lower(username) = lower(?)",
            hash,
            username);
    if (updated != 1) {
      throw new IllegalStateException("No user " + username + " to sign in as");
    }
    return RUN_PASSWORD;
  }

  /**
   * The JSON body of a sign-in request of a user with the password of this test run.
   *
   * @param username SIT/UAT user of the seed data
   * @return request body
   */
  public String loginBody(String username) {
    return "{\"username\":\"" + username + "\",\"password\":\"" + of(username) + "\"}";
  }

  /**
   * A sign-in request of a user with the password of this test run.
   *
   * @param username SIT/UAT user of the seed data
   * @return request to {@code /api/v1/auth/login}
   */
  public MockHttpServletRequestBuilder login(String username) {
    return MockMvcRequestBuilders.post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginBody(username));
  }

  private static String newPassword() {
    byte[] bytes = new byte[18];
    new SecureRandom().nextBytes(bytes);
    return "Run#" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "9a";
  }
}
