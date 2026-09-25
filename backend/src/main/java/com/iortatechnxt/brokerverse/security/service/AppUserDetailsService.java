package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
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
 * the role cache ({@link RolePermissionLookup}).
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository users;
  private final RolePermissionLookup rolePermissions;

  /**
   * Creates the service.
   *
   * @param users user repository
   * @param rolePermissions cached role permissions
   */
  public AppUserDetailsService(AppUserRepository users, RolePermissionLookup rolePermissions) {
    this.users = users;
    this.rolePermissions = rolePermissions;
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
            .map(SimpleGrantedAuthority::new)
            .toList();
    return User.withUsername(user.getUsername())
        .password(user.getPasswordHash())
        .authorities(authorities)
        .disabled(!user.isEnabled())
        .accountLocked(user.isLocked())
        .build();
  }
}
