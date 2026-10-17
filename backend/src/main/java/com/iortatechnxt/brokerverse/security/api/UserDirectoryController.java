package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.service.UserDirectoryService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Display names of the users for the signed-in users: screens and pickers show names, never login
 * ids. The role name of other users (who the administrators are) is given only to the holders of
 * {@value #ROLE_READERS}; every caller sees the role of their own entry.
 */
@RestController
@RequestMapping("/api/v1/users/directory")
public class UserDirectoryController {

  /** Permissions that may read the role names of other users. */
  static final String ROLE_READERS = "USER_MANAGE, UAM_VIEW or AUDIT_VIEW";

  private final UserDirectoryService directory;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param directory user directory
   * @param currentUser the caller and their permissions
   */
  public UserDirectoryController(UserDirectoryService directory, CurrentUser currentUser) {
    this.directory = directory;
    this.currentUser = currentUser;
  }

  /**
   * Lists every user's login id and display name, with the main role name where the caller may read
   * it.
   *
   * @return entries
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<UserDirectoryService.Entry> entries() {
    boolean roles =
        currentUser.hasAuthority("USER_MANAGE")
            || currentUser.hasAuthority("UAM_VIEW")
            || currentUser.hasAuthority("AUDIT_VIEW");
    return directory.entries(roles, currentUser.username());
  }
}
