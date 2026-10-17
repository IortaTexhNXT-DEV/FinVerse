package com.iortatechnxt.brokerverse.csf.cases.api;

import com.iortatechnxt.brokerverse.common.security.GlobalRecord;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore.Member;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore.Route;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore.TypeSetting;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseSetupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Case Routing screen (FR-CSF-051, 060, 061; {@code CSF_CASE_ROUTING}): turnaround times of the
 * case types, the routing table and the members of the fulfilment units. The turnaround times are
 * read by every user who logs or supervises cases.
 */
@GlobalRecord("the case routing set-up is programme-wide")
@RestController
@RequestMapping("/api/v1/csf/case-setup")
public class CsfCaseSetupController {

  private static final String MAINTAIN = "hasAuthority('CSF_CASE_ROUTING')";

  private final CsfCaseSetupService setup;

  /**
   * Creates the controller.
   *
   * @param setup configuration
   */
  public CsfCaseSetupController(CsfCaseSetupService setup) {
    this.setup = setup;
  }

  /**
   * The turnaround times of the case types.
   *
   * @return settings
   */
  @GetMapping("/turnaround-times")
  @PreAuthorize("hasAnyAuthority('CSF_CASE_ROUTING', 'CSF_VIEW', 'CSF_CASE_FULFIL')")
  public List<TypeSetting> turnaroundTimes() {
    return setup.typeSettings();
  }

  /**
   * Sets the turnaround time of a case type.
   *
   * @param request setting
   * @return settings
   */
  @PutMapping("/turnaround-times")
  @PreAuthorize(MAINTAIN)
  public List<TypeSetting> saveTurnaroundTime(
      @Valid @RequestBody CaseRequests.TypeSetting request) {
    return setup.saveTypeSetting(
        new TypeSetting(request.caseType(), request.tatDays(), request.autoEscalate()));
  }

  /**
   * The routing table.
   *
   * @return rows
   */
  @GetMapping("/routes")
  @PreAuthorize(MAINTAIN)
  public List<Route> routes() {
    return setup.routes();
  }

  /**
   * Adds a routing row.
   *
   * @param request row
   * @return rows
   */
  @PostMapping("/routes")
  @PreAuthorize(MAINTAIN)
  public List<Route> addRoute(@Valid @RequestBody CaseRequests.Route request) {
    return setup.saveRoute(route(null, request));
  }

  /**
   * Changes a routing row.
   *
   * @param id row
   * @param request row
   * @return rows
   */
  @PutMapping("/routes/{id}")
  @PreAuthorize(MAINTAIN)
  public List<Route> changeRoute(
      @PathVariable Long id, @Valid @RequestBody CaseRequests.Route request) {
    return setup.saveRoute(route(id, request));
  }

  /**
   * The members of the fulfilment units.
   *
   * @return members
   */
  @GetMapping("/members")
  @PreAuthorize(MAINTAIN)
  public List<Member> members() {
    return setup.members();
  }

  /**
   * Adds a member.
   *
   * @param request unit and user
   * @return members
   */
  @PostMapping("/members")
  @PreAuthorize(MAINTAIN)
  public List<Member> addMember(@Valid @RequestBody CaseRequests.Member request) {
    return setup.addMember(request.unitCode(), request.username());
  }

  /**
   * Removes a member.
   *
   * @param id membership
   * @return members
   */
  @DeleteMapping("/members/{id}")
  @PreAuthorize(MAINTAIN)
  public List<Member> removeMember(@PathVariable Long id) {
    return setup.removeMember(id);
  }

  private static Route route(Long id, CaseRequests.Route r) {
    return new Route(
        id,
        r.caseType(),
        r.marketSegment(),
        r.nature(),
        r.fulfilmentUnit(),
        r.escalationGroup(),
        r.active());
  }
}
