package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first system administrator on an empty installation.
 *
 * <p>The password comes from {@code BROKERVERSE_ADMIN_INITIAL_PASSWORD}; nothing is created when it
 * is absent, so no default credentials ever exist in production.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);

  private final AppUserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;
  private final String username;
  private final String initialPassword;

  /**
   * Creates the bootstrapper.
   *
   * @param users user repository
   * @param roles role repository
   * @param encoder password encoder
   * @param username administrator user name
   * @param initialPassword initial password (blank disables bootstrap)
   */
  public AdminBootstrap(
      AppUserRepository users,
      RoleRepository roles,
      PasswordEncoder encoder,
      @Value("${BROKERVERSE_ADMIN_USERNAME:sysadmin}") String username,
      @Value("${BROKERVERSE_ADMIN_INITIAL_PASSWORD:}") String initialPassword) {
    this.users = users;
    this.roles = roles;
    this.encoder = encoder;
    this.username = username;
    this.initialPassword = initialPassword;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (users.count() > 0) {
      return;
    }
    if (initialPassword == null || initialPassword.isBlank()) {
      LOG.warn(
          "No users exist and BROKERVERSE_ADMIN_INITIAL_PASSWORD is not set; no administrator created");
    } else {
      createAdministrator();
    }
  }

  private void createAdministrator() {
    AppUser admin = new AppUser(username, "System Administrator", encoder.encode(initialPassword));
    admin.replaceRoles(new HashSet<>(roles.findByCodeIn(List.of("SYSADMIN"))));
    users.save(admin);
    LOG.info("Initial administrator '{}' created; change the password after first login", username);
  }
}
