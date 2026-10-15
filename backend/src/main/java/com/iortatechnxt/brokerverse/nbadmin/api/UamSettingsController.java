package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.system.api.SystemController.RejectRequest;
import com.iortatechnxt.brokerverse.system.api.dto.ParameterResponse;
import com.iortatechnxt.brokerverse.system.api.dto.ParameterUpdateRequest;
import com.iortatechnxt.brokerverse.system.service.SecurityParameterApprovals;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UAM Settings: the user access and security parameters (session timeout and warnings, dormancy,
 * lock-out and password of the local test accounts, reconciliation interval and the group-profile
 * switches), moved from System Parameters. Every change waits until another holder of
 * SECURITY_PARAMETER_APPROVE approves it (maker-checker of {@link SecurityParameterApprovals}).
 */
@RestController
@RequestMapping("/api/v1/nbadmin/uam-settings")
public class UamSettingsController {

  /** The parameters of the screen, in the order shown. */
  public static final List<String> KEYS =
      List.of(
          "SESSION_TIMEOUT_MINUTES",
          "SESSION_IDLE_WARNING_MINUTES",
          "SESSION_EXPIRY_WARNING_MINUTES",
          "SESSION_BDOI_DIALOG",
          "SESSION_TIMEOUT_PAGE",
          "UAM_DORMANT_DAYS",
          "UAM_DORMANT_NOTICE_DAYS",
          "LOGIN_MAX_FAILED_ATTEMPTS",
          "PASSWORD_MAX_AGE_DAYS",
          "PASSWORD_MIN_AGE_DAYS",
          "PASSWORD_HISTORY_COUNT",
          "PASSWORD_EXPIRY_NOTICE_DAYS",
          "PASSWORD_RESET_BDOI_RULES",
          "UAM_SSO_PROVISIONING",
          "UAM_PROVISIONING_ROLES",
          "UAM_RECONCILIATION_INTERVAL_DAYS",
          "UAM_ROLE_APPLY_ON_APPROVAL",
          "UAM_ANY_APPROVER",
          "UAM_WORKING_HOURS",
          "UAM_DIRECT_ROLE_EDIT",
          "USER_ID_PATTERN");

  private static final String READ =
      "hasAnyAuthority('USER_MANAGE', 'SYSTEM_PARAMETER_MANAGE', 'SECURITY_PARAMETER_APPROVE')";
  private static final String MAKER = "hasAnyAuthority('USER_MANAGE', 'SYSTEM_PARAMETER_MANAGE')";

  private final SystemParameterService parameters;
  private final SecurityParameterApprovals approvals;

  /**
   * Creates the controller.
   *
   * @param parameters parameters
   * @param approvals maker-checker of the security parameters
   */
  public UamSettingsController(
      SystemParameterService parameters, SecurityParameterApprovals approvals) {
    this.parameters = parameters;
    this.approvals = approvals;
  }

  /**
   * The UAM settings with any change waiting for approval.
   *
   * @return parameters in the order of the screen (those not installed are left out)
   */
  @GetMapping
  @PreAuthorize(READ)
  public List<ParameterResponse> list() {
    return parameters.list().stream()
        .filter(p -> KEYS.contains(p.getKey()))
        .sorted(java.util.Comparator.comparingInt(p -> KEYS.indexOf(p.getKey())))
        .map(ParameterResponse::from)
        .toList();
  }

  /**
   * Proposes a new value; it applies once another user approves it.
   *
   * @param key parameter
   * @param request value
   * @return the parameter with its pending change
   */
  @PutMapping("/{key}")
  @PreAuthorize(MAKER)
  public ParameterResponse change(
      @PathVariable String key, @Valid @RequestBody ParameterUpdateRequest request) {
    return ParameterResponse.from(approvals.change(uam(key), request.value()));
  }

  /**
   * Approves the pending change (another user than the maker).
   *
   * @param key parameter
   * @return the parameter
   */
  @PostMapping("/{key}/approve")
  @PreAuthorize("hasAuthority('SECURITY_PARAMETER_APPROVE')")
  public ParameterResponse approve(@PathVariable String key) {
    return ParameterResponse.from(approvals.approve(uam(key)));
  }

  /**
   * Rejects the pending change, or withdraws it (the maker).
   *
   * @param key parameter
   * @param request reason
   * @return the parameter
   */
  @PostMapping("/{key}/reject")
  @PreAuthorize(
      "hasAnyAuthority('SECURITY_PARAMETER_APPROVE', 'USER_MANAGE', 'SYSTEM_PARAMETER_MANAGE')")
  public ParameterResponse reject(
      @PathVariable String key, @Valid @RequestBody(required = false) RejectRequest request) {
    return ParameterResponse.from(
        approvals.reject(uam(key), request == null ? null : request.reason()));
  }

  private static String uam(String key) {
    if (!KEYS.contains(key)) {
      throw new BusinessRuleException(
          "UAM_SETTING_UNKNOWN", key + " is not a setting of User Access");
    }
    return key;
  }
}
