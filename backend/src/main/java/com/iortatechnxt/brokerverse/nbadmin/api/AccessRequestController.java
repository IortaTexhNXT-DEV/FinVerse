package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestEventResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestQuery;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessSettingsResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessSubmitRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.UserAccessResponse;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestDescriber;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessSettings;
import com.iortatechnxt.brokerverse.security.api.dto.RoleResponse;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.system.service.ProductModules;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Group-profile changes (BRD 3.002): the proposals of the maker with drafts, submission, history,
 * and the users and group profiles the User Access screens show. Any checker other than the maker
 * decides a change ({@link AccessDecisionController}). Users and their group profiles come from
 * UIDM-ISC; the requests about users raised before remain readable here.
 */
@RestController
@RequestMapping("/api/v1/nbadmin")
public class AccessRequestController {

  /** Anyone taking part in group-profile changes, and their reviewers. */
  static final String VIEW =
      "hasAnyAuthority('UAM_VIEW', 'ACCESS_REQUEST', 'UAM_GROUP_REQUEST', 'ACCESS_APPROVE',"
          + " 'UAM_SECOND_APPROVE', 'ROLE_MANAGE', 'AUDIT_VIEW')";

  /** The makers of a group-profile change. */
  static final String REQUEST = "hasAnyAuthority('ACCESS_REQUEST', 'UAM_GROUP_REQUEST')";

  private static final int PAGE_SIZE = 25;

  private final AccessRequestService requests;
  private final AccessSettings settings;
  private final UserAdminService userAdmin;
  private final AccessRequestDescriber describer;
  private final ProductModules modules;

  /**
   * Creates the controller.
   *
   * @param requests access requests
   * @param settings parameters
   * @param userAdmin users and roles
   * @param describer descriptions of the requests
   * @param modules product modules (the module of each group profile)
   */
  public AccessRequestController(
      AccessRequestService requests,
      AccessSettings settings,
      UserAdminService userAdmin,
      AccessRequestDescriber describer,
      ProductModules modules) {
    this.describer = describer;
    this.modules = modules;
    this.requests = requests;
    this.settings = settings;
    this.userAdmin = userAdmin;
  }

  /**
   * Work list of the current user, newest first (FR-UA-017, FR-UA-018).
   *
   * @param query tab, filters and page
   * @return requests
   */
  @GetMapping("/access-requests")
  @PreAuthorize(VIEW)
  public PageResponse<AccessRequestResponse> search(@Valid AccessRequestQuery query) {
    return PageResponse.of(
        requests.search(
            query.search(),
            PageRequest.of(query.pageOrFirst(), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"))),
        this::view);
  }

  /**
   * One request the current user may see.
   *
   * @param id id
   * @return request
   */
  @GetMapping("/access-requests/{id}")
  @PreAuthorize(VIEW)
  public AccessRequestResponse get(@PathVariable Long id) {
    return view(requests.view(id));
  }

  /**
   * History of a request (BRD 1.008; History tab).
   *
   * @param id request
   * @return events, oldest first
   */
  @GetMapping("/access-requests/{id}/history")
  @PreAuthorize(VIEW)
  public List<AccessRequestEventResponse> history(@PathVariable Long id) {
    return requests.history(id).stream().map(AccessRequestEventResponse::from).toList();
  }

  /**
   * Creates a group-profile change: a draft ({@code draft=true}), or submitted at once to any
   * checker.
   *
   * @param request request
   * @param draft true to save a draft
   * @return the draft or pending request
   */
  @PostMapping("/access-requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(REQUEST)
  public AccessRequestResponse create(
      @Valid @RequestBody AccessRequestRequest request,
      @RequestParam(defaultValue = "false") boolean draft) {
    return view(requests.create(request.content(), draft));
  }

  /**
   * Edits a draft or corrects a returned change (maker).
   *
   * @param id request
   * @param request new content
   * @return the request
   */
  @PutMapping("/access-requests/{id}")
  @PreAuthorize(REQUEST)
  public AccessRequestResponse edit(
      @PathVariable Long id, @Valid @RequestBody AccessRequestRequest request) {
    return view(requests.edit(id, request.content()));
  }

  /**
   * Submits a draft or resubmits a corrected change to the checkers (maker).
   *
   * @param id request
   * @param body remarks
   * @return the pending request
   */
  @PostMapping("/access-requests/{id}/submit")
  @PreAuthorize(REQUEST)
  public AccessRequestResponse submit(
      @PathVariable Long id, @Valid @RequestBody AccessSubmitRequest body) {
    return view(requests.submit(id, body.remarks()));
  }

  /**
   * The User Access switches the screens follow.
   *
   * @return settings
   */
  @GetMapping("/access-settings")
  @PreAuthorize("isAuthenticated()")
  public AccessSettingsResponse settings() {
    return AccessSettingsResponse.from(settings);
  }

  /**
   * Users with their roles and data (to prepare a request).
   *
   * @return users
   */
  @GetMapping("/users")
  @PreAuthorize(VIEW)
  public List<UserAccessResponse> users() {
    return userAdmin.listUsers().stream().map(UserAccessResponse::from).toList();
  }

  /**
   * Roles (to prepare a request).
   *
   * @return roles
   */
  @GetMapping("/roles")
  @PreAuthorize(VIEW)
  public List<RoleResponse> roles() {
    return userAdmin.listRoles().stream()
        .map(
            r ->
                RoleResponse.from(
                    r,
                    modules.moduleOfProfile(r.getPermissions().stream().map(Enum::name).toList())))
        .toList();
  }

  private AccessRequestResponse view(AccessRequest r) {
    return AccessRequestResponse.from(r, describer.describe(r));
  }
}
