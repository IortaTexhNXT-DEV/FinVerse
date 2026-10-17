package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Users by role and by permission (SNSRP-106 team roles, SNSRP-405 escalation roles, SNSRP-303 /
 * 802 notices to the UCC and investigators): enabled, unlocked users of an active role.
 */
@Component
@Transactional(readOnly = true)
public class RoleMembers {

  private static final String BY_ROLE =
      "select distinct u.username from sec_user u"
          + " join sec_user_role ur on ur.user_id = u.id"
          + " join sec_role r on r.id = ur.role_id"
          + " where r.code = :role and r.active = true and u.enabled = true and u.locked = false"
          + " order by u.username";

  private final NamedParameterJdbcTemplate jdbc;
  private final UserDirectory users;

  /**
   * Creates the lookup.
   *
   * @param jdbc named-parameter JDBC
   * @param users user directory
   */
  public RoleMembers(NamedParameterJdbcTemplate jdbc, UserDirectory users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  /**
   * Enabled users of a role.
   *
   * @param roleCode role code
   * @return user names, sorted
   */
  public List<String> ofRole(String roleCode) {
    return jdbc.queryForList(BY_ROLE, Map.of("role", roleCode), String.class);
  }

  /**
   * Enabled users holding a permission.
   *
   * @param permission permission
   * @return user names, sorted
   */
  public List<String> withPermission(String permission) {
    return users.usersWithPermission(permission);
  }

  /**
   * Whether a user holds a permission.
   *
   * @param username user
   * @param permission permission
   * @return true when held
   */
  public boolean holds(String username, String permission) {
    return username != null
        && withPermission(permission).stream().anyMatch(u -> CurrentUser.sameUser(u, username));
  }
}
