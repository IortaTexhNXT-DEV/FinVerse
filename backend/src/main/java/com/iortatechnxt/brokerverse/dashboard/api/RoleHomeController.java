package com.iortatechnxt.brokerverse.dashboard.api;

import com.iortatechnxt.brokerverse.dashboard.api.dto.HomeWidgetResponse;
import com.iortatechnxt.brokerverse.dashboard.api.dto.RoleHomeDecisionRequest;
import com.iortatechnxt.brokerverse.dashboard.api.dto.RoleHomeRequest;
import com.iortatechnxt.brokerverse.dashboard.api.dto.RoleHomeResponse;
import com.iortatechnxt.brokerverse.dashboard.domain.HomeWidget;
import com.iortatechnxt.brokerverse.dashboard.service.RoleHomeService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role Home Set-up (Setup &amp; Administration): the System Administrator requests the home-page
 * widgets of a role, another user approves them; every signed-in user reads the widgets of their
 * own home page.
 */
@RestController
@RequestMapping("/api/v1/role-homes")
public class RoleHomeController {

  private static final String VIEW = "hasAnyAuthority('ROLE_HOME_MANAGE','ROLE_HOME_APPROVE')";
  private static final String DECIDE = "hasAnyAuthority('ROLE_HOME_APPROVE','ROLE_HOME_MANAGE')";

  private final RoleHomeService homes;

  /**
   * Creates the controller.
   *
   * @param homes role home pages
   */
  public RoleHomeController(RoleHomeService homes) {
    this.homes = homes;
  }

  /**
   * The widgets of the current user's home page.
   *
   * @return widget codes in display order
   */
  @GetMapping("/mine")
  @PreAuthorize("isAuthenticated()")
  public List<String> mine() {
    return homes.mine();
  }

  /**
   * The widget catalogue.
   *
   * @return widgets in delivered order
   */
  @GetMapping("/widgets")
  @PreAuthorize(VIEW)
  public List<HomeWidgetResponse> widgets() {
    return Arrays.stream(HomeWidget.values()).map(HomeWidgetResponse::from).toList();
  }

  /**
   * The home page of every active role.
   *
   * @return roles by name
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<RoleHomeResponse> list() {
    return homes.list().stream().map(RoleHomeResponse::from).toList();
  }

  /**
   * Requests the widgets of a role.
   *
   * @param code role code
   * @param request widgets and reason
   * @return the role's home page
   */
  @PostMapping("/{code}/change")
  @PreAuthorize("hasAuthority('ROLE_HOME_MANAGE')")
  public RoleHomeResponse request(
      @PathVariable String code, @Valid @RequestBody RoleHomeRequest request) {
    return RoleHomeResponse.from(homes.request(code, request.widgets(), request.reason()));
  }

  /**
   * Approves the requested widgets of a role.
   *
   * @param code role code
   * @return the role's home page
   */
  @PostMapping("/{code}/approve")
  @PreAuthorize("hasAuthority('ROLE_HOME_APPROVE')")
  public RoleHomeResponse approve(@PathVariable String code) {
    return RoleHomeResponse.from(homes.approve(code));
  }

  /**
   * Rejects (or, by the requester, withdraws) the requested widgets of a role.
   *
   * @param code role code
   * @param request reason
   * @return the role's home page
   */
  @PostMapping("/{code}/reject")
  @PreAuthorize(DECIDE)
  public RoleHomeResponse reject(
      @PathVariable String code, @Valid @RequestBody RoleHomeDecisionRequest request) {
    return RoleHomeResponse.from(homes.reject(code, request.reason()));
  }
}
