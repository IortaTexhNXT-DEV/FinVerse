package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.service.ActorRoles;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The group profiles a user holds, for the audit entries written outside a signed-in request (BDOI
 * FRS FRUM.008.01: the role of the user at the time of the action), and the Windows ID of the users
 * on the Audit Trail (FRUM.008.02).
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

  @Override
  @Transactional(readOnly = true)
  public Map<String, String> windowsIds(Collection<String> usernames) {
    Map<String, String> ids = new HashMap<>();
    if (usernames.isEmpty()) {
      return ids;
    }
    List<String> lower =
        usernames.stream().map(u -> u.toLowerCase(Locale.ROOT)).distinct().toList();
    for (Object[] row : users.windowsIdsOf(lower)) {
      ids.put(((String) row[0]).toLowerCase(Locale.ROOT), (String) row[1]);
    }
    return ids;
  }
}
