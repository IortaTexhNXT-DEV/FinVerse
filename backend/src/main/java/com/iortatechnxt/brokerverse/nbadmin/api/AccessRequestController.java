package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestEventResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessSettingsResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessSubmitRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.ApproverResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.UserAccessResponse;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessUserType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessApprovers;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestSearch;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestSearch.Scope;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessSettings;
import com.iortatechnxt.brokerverse.security.api.dto.RoleResponse;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
 * User access requests (BRNB.085; BRD 1.002-1.009, 3.002): work lists, drafts, submission, history,
 * the approver drop-down, and the users and roles of the request form. The decisions are in {@link
 * AccessDecisionController}.
 */
@RestController
@RequestMapping("/api/v1/nbadmin")
public class AccessRequestController {

  /** Anyone taking part in access requests. */
  static final String VIEW =
      "hasAnyAuthority('UAM_VIEW', 'ACCESS_REQUEST', 'ACCESS_APPROVE', 'UAM_SECOND_APPROVE',"
          + " 'ROLE_MANAGE', 'AUDIT_VIEW')";

  /** Any request function (the type-specific permission is checked by the service). */
  static final String REQUEST =
      "hasAnyAuthority('ACCESS_REQUEST', 'UAM_ENROLL', 'UAM_MODIFY', 'UAM_DEACTIVATE',"
          + " 'UAM_REACTIVATE', 'UAM_GROUP_REQUEST', 'UAM_CORRECT', 'PORTAL_USER_REQUEST')";

  private static final int PAGE_SIZE = 25;

  private final AccessRequestService requests;
  private final AccessApprovers approvers;
  private final AccessSettings settings;
  private final UserAdminService userAdmin;

  /**
   * Creates the controller.
   *
   * @param requests access requests
   * @param approvers eligible approvers
   * @param settings parameters
   * @param userAdmin users and roles
   */
  public AccessRequestController(
      AccessRequestService requests,
      AccessApprovers approvers,
      AccessSettings settings,
      UserAdminService userAdmin) {
    this.requests = requests;
    this.approvers = approvers;
    this.settings = settings;
    this.userAdmin = userAdmin;
  }

  /**
   * Work list of the current user, newest first (FR-UA-017, FR-UA-018).
   *
   * @param scope tab (MINE, ASSIGNED, SECOND, IMPLEMENTATION, ALL)
   * @param status status
   * @param type request type
   * @param text user name, role or request number
   * @param requester requester
   * @param approver approver
   * @param from requested on or after
   * @param to requested on or before
   * @param groupProfiles true for group-profile requests only, false for user requests only
   * @param page page
   * @return requests
   */
  @GetMapping("/access-requests")
  @PreAuthorize(VIEW)
  public PageResponse<AccessRequestResponse> search(
      @RequestParam(required = false) Scope scope,
      @RequestParam(required = false) AccessRequestStatus status,
      @RequestParam(required = false) AccessRequestType type,
      @RequestParam(required = false) String text,
      @RequestParam(required = false) String requester,
      @RequestParam(required = false) String approver,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Boolean groupProfiles,
      @RequestParam(defaultValue = "0") int page) {
    return PageResponse.of(
        requests.search(
            new AccessRequestSearch(
                scope, status, type, text, requester, approver, from, to, groupProfiles),
            PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"))),
        AccessRequestResponse::from);
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
    return AccessRequestResponse.from(requests.view(id));
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
   * Creates a request: a draft ({@code draft=true}), or submitted at once (the approvers of the
   * body; without approvers any approver decides, as the one-step submission of PMADD05).
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
    List<String> chosen =
        request.approvers() == null || request.approvers().isEmpty() ? null : request.approvers();
    return AccessRequestResponse.from(requests.create(request.content(), draft, chosen));
  }

  /**
   * Edits a draft or corrects a returned request (creator).
   *
   * @param id request
   * @param request new content
   * @return the request
   */
  @PutMapping("/access-requests/{id}")
  @PreAuthorize(REQUEST)
  public AccessRequestResponse edit(
      @PathVariable Long id, @Valid @RequestBody AccessRequestRequest request) {
    return AccessRequestResponse.from(requests.edit(id, request.content()));
  }

  /**
   * Submits a draft or resubmits a corrected request to the chosen approvers (creator).
   *
   * @param id request
   * @param body approvers and remarks
   * @return the pending request
   */
  @PostMapping("/access-requests/{id}/submit")
  @PreAuthorize(REQUEST)
  public AccessRequestResponse submit(
      @PathVariable Long id, @Valid @RequestBody AccessSubmitRequest body) {
    return AccessRequestResponse.from(requests.submit(id, body.approvers(), body.remarks()));
  }

  /**
   * Eligible approvers of a request by the current user (FR-UA-015).
   *
   * @param userType INTERNAL or EXTERNAL
   * @param subject user the request is about (excluded)
   * @return approvers
   */
  @GetMapping("/approvers")
  @PreAuthorize(REQUEST)
  public List<ApproverResponse> approvers(
      @RequestParam(defaultValue = "INTERNAL") AccessUserType userType,
      @RequestParam(required = false) String subject) {
    return approvers.eligible(userType, subject).stream().map(ApproverResponse::from).toList();
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
    return userAdmin.listRoles().stream().map(RoleResponse::from).toList();
  }
}
