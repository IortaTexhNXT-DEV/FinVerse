package com.iortatechnxt.finverse.security.service;

import com.iortatechnxt.finverse.security.domain.AppUser;
import com.iortatechnxt.finverse.security.domain.AppUserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads users and maps their effective permissions to Spring Security authorities. */
@Service
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository users;

  /**
   * Creates the service.
   *
   * @param users user repository
   */
  public AppUserDetailsService(AppUserRepository users) {
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
    AppUser user =
        users
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    List<SimpleGrantedAuthority> authorities =
        user.effectivePermissions().stream()
            .map(p -> new SimpleGrantedAuthority(p.name()))
            .toList();
    return User.withUsername(user.getUsername())
        .password(user.getPasswordHash())
        .authorities(authorities)
        .disabled(!user.isEnabled())
        .accountLocked(user.isLocked())
        .build();
  }
}
