package com.iortatechnxt.brokerverse.brokerclaims.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandler;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandlerRepository;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.domain.LovType;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The claims handler register and the Claims lists on Claims Setup (BRCLM.012/017/036, NFR p.37,
 * FR-CM-041): the Unit Head registers each claims user's unit and team (the unit drives the status
 * access matrix) and maintains the values of the lists whose owner permission is BCL_SETUP
 * (statuses, settlement types, adjusters, catastrophe codes, units and the others) through the
 * list-of-values API. Register changes are audited.
 */
@Service
@Transactional
public class HandlerSetupService {

  private static final String ENTITY = "BrokerClaimHandler";

  private final ClaimHandlerRepository handlers;
  private final LovService lovs;
  private final UserDirectory directory;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param handlers claims handler register
   * @param lovs lists of values
   * @param directory users
   * @param audit audit trail
   * @param clock clock
   */
  public HandlerSetupService(
      ClaimHandlerRepository handlers,
      LovService lovs,
      UserDirectory directory,
      AuditTrailService audit,
      Clock clock) {
    this.handlers = handlers;
    this.lovs = lovs;
    this.directory = directory;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The register.
   *
   * @return handlers in user order
   */
  @Transactional(readOnly = true)
  public List<ClaimHandler> handlers() {
    return handlers.findAllByOrderByUsernameAsc();
  }

  /**
   * The claims users who may be registered or assigned (holders of BCL_VIEW).
   *
   * @return usernames
   */
  @Transactional(readOnly = true)
  public List<String> claimsUsers() {
    return directory.usersWithPermission(Permission.BCL_VIEW.name()).stream().sorted().toList();
  }

  /**
   * Registers a handler or changes its unit, team and active flag.
   *
   * @param username user
   * @param unitCode unit ({@code BCL_UNIT})
   * @param team team, may be blank
   * @param active whether the handler is active
   * @return the handler
   */
  public ClaimHandler save(String username, String unitCode, String team, boolean active) {
    requireFilled(username, "BCL_HANDLER_REQUIRED", "Select the claims user");
    requireFilled(unitCode, "BCL_UNIT_REQUIRED", "Select the claims unit");
    lovs.requireValid(ClaimCodes.LOV_UNIT, unitCode, ClaimAgeing.today(clock));
    String user = username.strip();
    if (claimsUsers().stream().noneMatch(u -> CurrentUser.sameUser(u, user))) {
      throw new BusinessRuleException("BCL_HANDLER_INVALID", user + " is not a claims user");
    }
    String teamName = team == null || team.isBlank() ? null : team.strip();
    ClaimHandler handler =
        handlers
            .findByUsernameIgnoreCase(user)
            .orElseGet(() -> handlers.save(new ClaimHandler(user, unitCode, teamName)));
    handler.update(unitCode, teamName, active);
    audit.record(
        ENTITY,
        user,
        AuditAction.UPDATE,
        "Unit " + unitCode + ", team " + teamName + (active ? ", active" : ", inactive"));
    return handler;
  }

  private static void requireFilled(String value, String code, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException(code, message);
    }
  }

  /**
   * The lists maintained by the Claims Unit Head (owner permission BCL_SETUP).
   *
   * @return list types
   */
  @Transactional(readOnly = true)
  public List<LovType> claimsLists() {
    return lovs.types().stream()
        .filter(t -> Permission.BCL_SETUP.name().equals(t.getOwnerPermission()))
        .toList();
  }
}
