package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.security.api.dto.DataScopeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.DataScopeResponse;
import com.iortatechnxt.brokerverse.security.service.DataScopeService;
import com.iortatechnxt.brokerverse.security.service.OrganizationUnits;
import com.iortatechnxt.brokerverse.security.service.OrganizationUnits.CompanyUnit;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data access of users (docs/architecture/DATA_SCOPE_DESIGN.md): the companies and branches each
 * user may act for. Readable by the user maintenance and user access request holders; a direct
 * change needs the user maintenance permission (the emergency direct edit of the Users screen), the
 * regular path is an access request.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class DataScopeController {

  private static final String READ =
      "hasAnyAuthority('USER_MANAGE','UAM_VIEW','UAM_ENROLL','UAM_MODIFY','ACCESS_REQUEST',"
          + "'ACCESS_APPROVE')";

  private final DataScopeService service;
  private final OrganizationUnits units;

  /**
   * Creates the controller.
   *
   * @param service data scope administration
   * @param units companies and branches
   */
  public DataScopeController(DataScopeService service, OrganizationUnits units) {
    this.service = service;
    this.units = units;
  }

  /**
   * The companies and branches the current user may grant (those of his own data access).
   *
   * @return companies with their branches
   */
  @GetMapping("/data-scope/units")
  @PreAuthorize(READ)
  public List<CompanyUnit> grantableUnits() {
    return service.grantableUnits();
  }

  /**
   * The data access of a user.
   *
   * @param id user id
   * @return scope with names
   */
  @GetMapping("/users/{id}/data-scope")
  @PreAuthorize(READ)
  public DataScopeResponse scope(@PathVariable Long id) {
    return response(service.scopeOf(id));
  }

  /**
   * Replaces the data access of a user (direct change, audited).
   *
   * @param id user id
   * @param request new scope
   * @return scope in force
   */
  @PutMapping("/users/{id}/data-scope")
  @PreAuthorize("hasAuthority('USER_MANAGE')")
  public DataScopeResponse replace(
      @PathVariable Long id, @Valid @RequestBody DataScopeRequest request) {
    return response(service.replace(id, request.toScope()));
  }

  private DataScopeResponse response(UserDataScope scope) {
    return DataScopeResponse.of(scope, units.companies(), service.describe(scope));
  }
}
