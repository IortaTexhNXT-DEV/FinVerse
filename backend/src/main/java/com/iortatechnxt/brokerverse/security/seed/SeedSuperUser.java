package com.iortatechnxt.brokerverse.security.seed;

import com.iortatechnxt.brokerverse.security.api.dto.PasswordChangeRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.Arrays;
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
 * Looks after the super user of the DEV, SIT and UAT environments (seed profile only, runs before
 * {@link SeedPasswords}).
 *
 * <p>The seed script V2901 creates the user {@value #USERNAME} with the group profile {@value
 * #PROFILE}. On every start the runner gives that profile the permissions added to the system
 * since, so the super user keeps every permission. When {@code
 * BROKERVERSE_SEED_SUPER_USER_PASSWORD} is set and the user still carries the hash of the seed
 * scripts, the user gets that password, without a change at the first sign-in. A password that does
 * not follow the password rules (length and complexity of {@link PasswordChangeRequest}) is not
 * used and a warning is logged. Without it the user follows {@code BROKERVERSE_SEED_PASSWORD} like
 * the other seed users.
 */
@Component
@Profile("seed")
@Order(0)
public class SeedSuperUser implements ApplicationRunner {

  /** User ID of the super user. */
  public static final String USERNAME = "Brokerverse";

  /** Group profile holding every permission. */
  public static final String PROFILE = "BDOI_SUPER_USER";

  private static final int MIN_LENGTH = 10;

  private static final Logger LOG = LoggerFactory.getLogger(SeedSuperUser.class);

  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final String password;

  /**
   * Creates the runner.
   *
   * @param jdbc JDBC
   * @param encoder password encoder
   * @param password password of the super user in this environment (blank: unchanged)
   */
  public SeedSuperUser(
      JdbcTemplate jdbc,
      PasswordEncoder encoder,
      @Value("${brokerverse.seed.super-user-password:}") String password) {
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    completeProfile();
    if (password == null || password.isBlank()) {
      return;
    }
    if (followsPasswordRules(password)) {
      setPassword();
    } else {
      LOG.warn(
          "BROKERVERSE_SEED_SUPER_USER_PASSWORD does not follow the password rules"
              + " ({}+ characters, upper and lower case letters, a digit and a symbol); not used",
          MIN_LENGTH);
    }
  }

  private void completeProfile() {
    List<String> held =
        jdbc.queryForList(
            "select p.permission from sec_role_permission p join sec_role r on r.id = p.role_id"
                + " where r.code = ?",
            String.class,
            PROFILE);
    List<String> missing =
        Arrays.stream(Permission.values()).map(Enum::name).filter(p -> !held.contains(p)).toList();
    if (held.isEmpty() || missing.isEmpty()) {
      return;
    }
    for (String permission : missing) {
      jdbc.update(
          "insert into sec_role_permission (role_id, permission)"
              + " select id, ? from sec_role where code = ?",
          permission,
          PROFILE);
    }
    LOG.info("Group profile {} given {} new permissions", PROFILE, missing.size());
  }

  private void setPassword() {
    int updated =
        jdbc.update(
            "update sec_user set password_hash = ?, must_change_password = false,"
                + " password_changed_at = now() where lower(username) = lower(?)"
                + " and password_hash = ?",
            encoder.encode(password),
            USERNAME,
            SeedPasswords.SCRIPT_HASH);
    if (updated > 0) {
      LOG.info("Password of this environment given to the super user {}", USERNAME);
    }
  }

  static boolean followsPasswordRules(String candidate) {
    return candidate.length() >= MIN_LENGTH && candidate.matches(PasswordChangeRequest.COMPLEXITY);
  }
}
