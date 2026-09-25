package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessDecisionResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.DecisionRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.UserAccessResponse;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestReturnService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.security.api.dto.RoleResponse;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * User access requests (BRNB.085): the Business Administrator submits, the Approver approves or
 * rejects; users and roles are listed for the request form.
 */
@RestController
@RequestMapping("/api/v1/nbadmin")
public class AccessRequestController {

  private static final String VIEW = "hasAnyAuthority('ACCESS_REQUEST', 'ACCESS_APPROVE')";
  private static final String APPROVE = "hasAuthority('ACCESS_APPROVE')";
  private static final int PAGE_SIZE = 25;

  private final AccessRequestService requests;
  private final UserAdminService userAdmin;
  private final AccessRequestReturnService returns;

  /**
   * Creates the controller.
   *
   * @param requests access requests
   * @param userAdmin users and roles
   * @param returns return and resubmission of requests
   */
  public AccessRequestController(
      AccessRequestService requests,
      UserAdminService userAdmin,
      AccessRequestReturnService returns) {
    this.requests = requests;
    this.userAdmin = userAdmin;
    this.returns = returns;
  }

  /**
   * Requests matching the filter, newest first.
   *
   * @param status status
   * @param type request type
   * @param text user name or request number
   * @param page page
   * @return requests
   */
  @GetMapping("/access-requests")
  @PreAuthorize(VIEW)
  public PageResponse<AccessRequestResponse> search(
      @RequestParam(required = false) AccessRequestStatus status,
      @RequestParam(required = false) AccessRequestType type,
      @RequestParam(required = false) String text,
      @RequestParam(defaultValue = "0") int page) {
    return PageResponse.of(
        requests.search(
            status,
            type,
            text,
            PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"))),
        AccessRequestResponse::from);
  }

  /**
   * One request.
   *
   * @param id id
   * @return request
   */
  @GetMapping("/access-requests/{id}")
  @PreAuthorize(VIEW)
  public AccessRequestResponse get(@PathVariable Long id) {
    return AccessRequestResponse.from(requests.get(id));
  }

  /**
   * Submits a request for approval.
   *
   * @param request request
   * @return pending request
   */
  @PostMapping("/access-requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ACCESS_REQUEST')")
  public AccessRequestResponse submit(@Valid @RequestBody AccessRequestRequest request) {
    return AccessRequestResponse.from(requests.submit(request.content()));
  }

  /**
   * Approves a request and applies it.
   *
   * @param id request
   * @param body optional comment
   * @return decision with the temporary password of a created user
   */
  @PostMapping("/access-requests/{id}/approve")
  @PreAuthorize(APPROVE)
  public AccessDecisionResponse approve(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessDecisionResponse.from(requests.approve(id, body.comment()));
  }

  /**
   * Rejects a request.
   *
   * @param id request
   * @param body reason
   * @return decision
   */
  @PostMapping("/access-requests/{id}/reject")
  @PreAuthorize(APPROVE)
  public AccessDecisionResponse reject(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessDecisionResponse.from(requests.reject(id, body.comment()));
  }

  /**
   * Returns a request to its requester with remarks (BASAU 2.4.1, 2.6.x).
   *
   * @param id request
   * @param body remarks
   * @return the returned request
   */
  @PostMapping("/access-requests/{id}/return")
  @PreAuthorize(APPROVE)
  public AccessRequestResponse returnRequest(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessRequestResponse.from(returns.returnRequest(id, body.comment()));
  }

  /**
   * Resubmits a returned request with a corrected justification (requester).
   *
   * @param id request
   * @param body new justification
   * @return the pending request
   */
  @PostMapping("/access-requests/{id}/resubmit")
  @PreAuthorize("hasAuthority('ACCESS_REQUEST')")
  public AccessRequestResponse resubmit(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessRequestResponse.from(returns.resubmit(id, body.comment()));
  }

  /**
   * Users with their roles (to prepare a modification request).
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
