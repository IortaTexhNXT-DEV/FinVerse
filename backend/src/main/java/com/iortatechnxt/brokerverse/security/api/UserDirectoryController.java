package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.service.UserDirectoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Display names of the users for every signed-in user: screens show names, never login ids (UX
 * readiness, client feedback 26-Sep-2026).
 */
@RestController
@RequestMapping("/api/v1/users/directory")
public class UserDirectoryController {

  private final UserDirectoryService directory;

  /**
   * Creates the controller.
   *
   * @param directory user directory
   */
  public UserDirectoryController(UserDirectoryService directory) {
    this.directory = directory;
  }

  /**
   * Lists every user's login id, display name and main role name.
   *
   * @return entries
   */
  @GetMapping
  public List<UserDirectoryService.Entry> entries() {
    return directory.entries();
  }
}
