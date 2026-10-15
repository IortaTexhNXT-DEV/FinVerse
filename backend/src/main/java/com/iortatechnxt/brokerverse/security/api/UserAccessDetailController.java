package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.api.dto.AccessChangeResponse;
import com.iortatechnxt.brokerverse.security.service.UserAccessDetail;
import com.iortatechnxt.brokerverse.security.service.UserAccessDetail.HeldProfile;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The User detail of User Access: the group profiles of a user with their source (UIDM-ISC) and
 * date, and the access history of the user. Read-only: users and their group profiles are
 * maintained in UIDM-ISC.
 */
@RestController
@RequestMapping("/api/v1/admin/users/{id}")
@PreAuthorize("hasAnyAuthority('USER_MANAGE','USER_VIEW')")
public class UserAccessDetailController {

  private final UserAccessDetail detail;

  /**
   * Creates the controller.
   *
   * @param detail user access detail
   */
  public UserAccessDetailController(UserAccessDetail detail) {
    this.detail = detail;
  }

  /**
   * The group profiles of a user.
   *
   * @param id user
   * @return group profiles by name
   */
  @GetMapping("/group-profiles")
  public List<HeldProfile> groupProfiles(@PathVariable Long id) {
    return detail.groupProfiles(id);
  }

  /**
   * The access history of a user, newest first.
   *
   * @param id user
   * @return logged changes
   */
  @GetMapping("/access-history")
  public List<AccessChangeResponse> history(@PathVariable Long id) {
    return detail.history(id).stream().map(AccessChangeResponse::from).toList();
  }
}
