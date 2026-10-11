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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The group profiles a user holds, for the audit entries written outside a signed-in request (BDOI
 * FRS FRUM.008.01: the role of the user at the time of the action), and the Windows ID of the users
 * on the Audit Trail (FRUM.008.02).
 */
@Component
public class AuditActorRoles implements ActorRoles {

  private static final String ROLES_SQL =
      "select coalesce(string_agg(r.name, ', ' order by r.name), '') from sec_user u"
          + " left join sec_user_role ur on ur.user_id = u.id"
          + " left join sec_role r on r.id = ur.role_id"
          + " where lower(u.username) = lower(?) group by u.id";

  private final AppUserRepository users;
  private final JdbcTemplate jdbc;

  /**
   * Creates the lookup.
   *
   * @param users users
   * @param jdbc JDBC template (role names without touching the persistence context)
   */
  public AuditActorRoles(AppUserRepository users, JdbcTemplate jdbc) {
    this.users = users;
    this.jdbc = jdbc;
  }

  /**
   * The role names of a user, read with plain SQL: the lookup runs inside the transaction of the
   * audited action and must not flush or load its entities (a flush there would stamp the
   * authorizer as the maker of a record being authorized).
   */
  @Override
  public String rolesOf(String username) {
    if (username == null) {
      return null;
    }
    List<String> found = jdbc.queryForList(ROLES_SQL, String.class, username);
    return found.isEmpty() ? null : found.get(0);
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
