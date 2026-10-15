package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLog;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLogRepository;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The User detail of the User Access screens: the group profiles a user holds with where each came
 * from and since when (read from the access change log), and the access history of the user (every
 * logged change of the user's attributes, newest first).
 */
@Service
@Transactional(readOnly = true)
public class UserAccessDetail {

  private static final String ROLES = "roles";

  private final AppUserRepository users;
  private final AccessChangeLogRepository changes;

  /**
   * Creates the service.
   *
   * @param users users
   * @param changes access change log
   */
  public UserAccessDetail(AppUserRepository users, AccessChangeLogRepository changes) {
    this.users = users;
    this.changes = changes;
  }

  /**
   * The group profiles of a user, by name.
   *
   * @param id user
   * @return group profiles with their source and the time they were given
   */
  public List<HeldProfile> groupProfiles(Long id) {
    AppUser user = user(id);
    Map<String, AccessChangeLog> given = givenAt(user.getUsername());
    List<HeldProfile> held = new ArrayList<>();
    for (Role r : user.getRoles()) {
      AccessChangeLog row = given.get(r.getCode());
      held.add(
          new HeldProfile(
              r.getCode(),
              r.getName(),
              r.isActive(),
              row == null ? null : row.getDoneBy(),
              row == null ? null : row.getOccurredAt(),
              row == null ? null : row.getRequestNo()));
    }
    held.sort(Comparator.comparing(HeldProfile::name));
    return held;
  }

  /**
   * The access history of a user, newest first.
   *
   * @param id user
   * @return logged changes
   */
  public List<AccessChangeLog> history(Long id) {
    return changes.findBySubjectTypeAndSubjectIgnoreCaseOrderByIdDesc(
        AccessSubjectType.USER, user(id).getUsername());
  }

  /** The change that gave each group profile the user holds since then (oldest first scan). */
  private Map<String, AccessChangeLog> givenAt(String username) {
    List<AccessChangeLog> rows =
        new ArrayList<>(
            changes.findBySubjectTypeAndSubjectIgnoreCaseOrderByIdDesc(
                AccessSubjectType.USER, username));
    rows.sort(Comparator.comparing(AccessChangeLog::getId));
    Map<String, AccessChangeLog> since = new HashMap<>();
    for (AccessChangeLog row : rows) {
      if (!ROLES.equals(row.getAttribute())) {
        continue;
      }
      Set<String> after = codes(row.getToValue());
      since.keySet().retainAll(after);
      after.forEach(code -> since.putIfAbsent(code, row));
    }
    return since;
  }

  private static Set<String> codes(String value) {
    Set<String> codes = new TreeSet<>();
    if (value != null && !value.isBlank()) {
      Arrays.stream(value.split(","))
          .map(String::trim)
          .filter(c -> !c.isEmpty())
          .forEach(codes::add);
    }
    return codes;
  }

  private AppUser user(Long id) {
    return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  /**
   * A group profile held by a user.
   *
   * @param code group profile code
   * @param name group profile name
   * @param active whether the group profile grants its permissions
   * @param source who gave it (UIDM-ISC, or the user of an earlier change); null when not logged
   * @param since when it was given; null when not logged
   * @param requestNo UIDM-ISC request number or access request number
   */
  public record HeldProfile(
      String code, String name, boolean active, String source, Instant since, String requestNo) {}
}
