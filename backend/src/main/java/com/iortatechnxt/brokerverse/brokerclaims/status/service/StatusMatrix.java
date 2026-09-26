package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandler;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandlerRepository;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusAccess;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusAccessRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The status access matrix applied to a user (BRCLM.012/013, FR-CL-041): a status may be selected
 * when an active row matches one of the user's roles and the user's claims unit (or any unit). The
 * unit comes from the claims handler register; a user outside the register sets no status. The
 * check runs in the service, so a direct API call outside the matrix is refused as well.
 */
@Service
@Transactional(readOnly = true)
public class StatusMatrix {

  private final StatusAccessRepository access;
  private final ClaimHandlerRepository handlers;
  private final UserDirectory directory;

  /**
   * Creates the matrix.
   *
   * @param access matrix rows
   * @param handlers claims handler register
   * @param directory users and roles
   */
  public StatusMatrix(
      StatusAccessRepository access, ClaimHandlerRepository handlers, UserDirectory directory) {
    this.access = access;
    this.handlers = handlers;
    this.directory = directory;
  }

  /**
   * The claims unit of a user.
   *
   * @param username user
   * @return unit of the active register entry
   */
  public Optional<String> unitOf(String username) {
    return handlers
        .findByUsernameIgnoreCase(username)
        .filter(ClaimHandler::isActive)
        .map(ClaimHandler::getUnitCode);
  }

  /**
   * The statuses a user may select.
   *
   * @param username user
   * @return status codes (empty when none)
   * @throws BusinessRuleException {@code BCL_UNIT_NOT_SET} when the user is not in the register
   */
  public Set<String> allowedStatuses(String username) {
    String unit =
        unitOf(username)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "BCL_UNIT_NOT_SET", "Your claims unit is not set. Contact the Unit Head"));
    Set<String> allowed = new LinkedHashSet<>();
    access.findByRoleCodeIn(directory.roleCodes(username)).stream()
        .filter(row -> row.admits(unit))
        .map(StatusAccess::getStatusCode)
        .forEach(allowed::add);
    return allowed;
  }

  /**
   * Refuses a status outside the matrix.
   *
   * @param username user
   * @param statusCode status
   * @param label status label for the message
   */
  public void requireAllowed(String username, String statusCode, String label) {
    if (!allowedStatuses(username).contains(statusCode)) {
      throw new BusinessRuleException(
          "BCL_STATUS_NOT_ALLOWED", "You are not allowed to set the status " + label);
    }
  }
}
