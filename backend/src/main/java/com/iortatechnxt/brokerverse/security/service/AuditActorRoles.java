package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.service.ActorRoles;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The group profiles a user holds, for the audit entries written outside a signed-in request (BDOI
 * FRS FRUM.008.01: the role of the user at the time of the action).
 */
@Component
public class AuditActorRoles implements ActorRoles {

  private final AppUserRepository users;

  /**
   * Creates the lookup.
   *
   * @param users users
   */
  public AuditActorRoles(AppUserRepository users) {
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true)
  public String rolesOf(String username) {
    if (username == null) {
      return null;
    }
    return users.findByUsernameIgnoreCase(username).map(AuditActorRoles::names).orElse(null);
  }

  /**
   * The role names of a user, sorted.
   *
   * @param user user
   * @return names separated by commas
   */
  static String names(AppUser user) {
    return user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.joining(", "));
  }
}
