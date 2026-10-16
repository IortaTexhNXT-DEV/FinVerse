package com.iortatechnxt.brokerverse.security.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retires the SIT/UAT persona users of the seed scripts in an environment that now works with the
 * client's own users (seed profile only, with {@code BROKERVERSE_SEED_RETIRE_SEED_PERSONAS=true}).
 *
 * <p>The users the seed scripts created (created by {@code SYSTEM}, never created through the
 * identity events or the User Access screens, which log a {@code CREATE_USER} change) are disabled
 * on every start, each with a {@code DISABLE_USER} entry in the access change log. The super user
 * {@value SeedSuperUser#USERNAME} stays. The users and their history are kept, so a user can be
 * enabled again on the User Access screens; the seed scripts and the automated tests are not
 * affected (the flag is off by default).
 */
@Component
@Profile("seed")
@Order(2)
public class SeedPersonaRetirement implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(SeedPersonaRetirement.class);

  static final String RETIRE =
      "with retired as ("
          + " update sec_user u set enabled = false, updated_at = now(), updated_by = 'SYSTEM'"
          + " where u.enabled and u.created_by = 'SYSTEM' and lower(u.username) <> lower(?)"
          + " and not exists (select 1 from sec_access_change_log l where l.subject_type = 'USER'"
          + " and lower(l.subject) = lower(u.username) and l.activity = 'CREATE_USER')"
          + " returning u.username)"
          + " insert into sec_access_change_log (occurred_at, subject_type, subject, activity,"
          + " attribute, from_value, to_value, done_by)"
          + " select now(), 'USER', username, 'DISABLE_USER', 'enabled', 'true', 'false', 'SYSTEM'"
          + " from retired";

  private final JdbcTemplate jdbc;
  private final boolean retire;

  /**
   * Creates the runner.
   *
   * @param jdbc JDBC
   * @param retire whether the persona users of the seed scripts are retired
   */
  public SeedPersonaRetirement(
      JdbcTemplate jdbc, @Value("${brokerverse.seed.retire-seed-personas:false}") boolean retire) {
    this.jdbc = jdbc;
    this.retire = retire;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!retire) {
      return;
    }
    int retired = jdbc.update(RETIRE, SeedSuperUser.USERNAME);
    if (retired > 0) {
      LOG.info("{} SIT/UAT persona users of the seed scripts disabled", retired);
    }
  }
}
