package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.ActorContext;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.system.service.ProductModules;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads users and maps their effective permissions to Spring Security authorities. The user is read
 * on every request (status and roles take effect at once); the permissions of each role come from
 * the role cache ({@link RolePermissionLookup}). A permission of switched-off product modules
 * grants nothing ({@link ProductModules#isPermissionActive}). The role names of the user are
 * remembered on the request for the audit entries of the action ({@link ActorContext}).
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository users;
  private final RolePermissionLookup rolePermissions;
  private final ProductModules modules;

  /**
   * Creates the service.
   *
   * @param users user repository
   * @param rolePermissions cached role permissions
   * @param modules product module switches
   */
  public AppUserDetailsService(
      AppUserRepository users, RolePermissionLookup rolePermissions, ProductModules modules) {
    this.users = users;
    this.rolePermissions = rolePermissions;
    this.modules = modules;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
    AppUser user =
        users
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    List<SimpleGrantedAuthority> authorities =
        user.getRoles().stream()
            .map(Role::getCode)
            .flatMap(code -> rolePermissions.permissionsOf(code).permissions().stream())
            .distinct()
            .filter(modules::isPermissionActive)
            .map(SimpleGrantedAuthority::new)
            .toList();
    ActorContext.rememberRoles(AuditActorRoles.names(user));
    return User.withUsername(user.getUsername())
        .password(user.getPasswordHash())
        .authorities(authorities)
        .disabled(!user.isEnabled())
        .accountLocked(user.isLocked())
        .build();
  }
}
