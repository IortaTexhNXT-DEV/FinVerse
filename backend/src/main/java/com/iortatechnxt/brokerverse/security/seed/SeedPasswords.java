package com.iortatechnxt.brokerverse.security.seed;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives the SIT/UAT users the password of this environment (seed profile only, runs first).
 *
 * <p>The seed scripts ({@code db/seed}) create every SIT/UAT user with one shared password hash.
 * When {@code BROKERVERSE_SEED_PASSWORD} is set, every user still carrying that hash gets the hash
 * of the environment's own password instead, so the hash of the seed scripts no longer opens any
 * account. With {@code BROKERVERSE_SEED_PASSWORD_MUST_CHANGE=true} the users must change it at
 * their first sign-in. Users who already have another password are left alone, so the runner does
 * nothing on later starts; to issue a new environment password, reset the users on the User Access
 * screens or recreate the seed database. Without {@code BROKERVERSE_SEED_PASSWORD} the users are
 * left unchanged and a warning is logged.
 */
@Component
@Profile("seed")
@Order(1)
public class SeedPasswords implements ApplicationRunner {

  /** The password hash that the seed scripts give every SIT/UAT user. */
  public static final String SCRIPT_HASH =
      "$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2";

  private static final Logger LOG = LoggerFactory.getLogger(SeedPasswords.class);

  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final String password;
  private final boolean mustChange;

  /**
   * Creates the runner.
   *
   * @param jdbc JDBC
   * @param encoder password encoder
   * @param password password of the SIT/UAT users of this environment (blank: unchanged)
   * @param mustChange whether the users must change it at their first sign-in
   */
  public SeedPasswords(
      JdbcTemplate jdbc,
      PasswordEncoder encoder,
      @Value("${brokerverse.seed.password:}") String password,
      @Value("${brokerverse.seed.password-must-change:false}") boolean mustChange) {
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.password = password;
    this.mustChange = mustChange;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<String> users =
        jdbc.queryForList(
            "select username from sec_user where password_hash = ?", String.class, SCRIPT_HASH);
    if (users.isEmpty()) {
      return;
    }
    if (password == null || password.isBlank()) {
      LOG.warn(
          "{} SIT/UAT users keep the password hash of the seed scripts;"
              + " set BROKERVERSE_SEED_PASSWORD to give them the password of this environment",
          users.size());
      return;
    }
    int updated =
        jdbc.update(
            "update sec_user set password_hash = ?,"
                + " must_change_password = must_change_password or ? where password_hash = ?",
            encoder.encode(password),
            mustChange,
            SCRIPT_HASH);
    LOG.info("Password of this environment given to {} SIT/UAT users", updated);
  }
}
