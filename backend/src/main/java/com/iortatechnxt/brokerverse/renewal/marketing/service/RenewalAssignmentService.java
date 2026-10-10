package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignmentRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assignment and re-assignment of renewals to Marketing AOs (FR-RN-030): the Team Leader tags the
 * accounts of the Unassigned Disposition tab to an officer of the unit and pushes them to the AO's
 * My Dispositions, or re-assigns them until the Renewal Advice locks them. Loan-driven
 * straight-through accounts are never assigned.
 */
@Service
public class RenewalAssignmentService {

  private final RenewalRecords records;
  private final RenewalAssignmentRepository assignments;
  private final RenewalFlow flow;
  private final RenewalScope scope;
  private final RenewalParameters parameters;
  private final RenewalNotices notices;
  private final RenewalBatch batch;
  private final SalesOrganisationService sales;
  private final AppUserRepository users;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param assignments assignment history
   * @param flow workflow
   * @param scope data scope
   * @param parameters parameters
   * @param notices notifications
   * @param batch batch runner
   * @param sales sales organisation
   * @param users users
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RenewalAssignmentService(
      RenewalRecords records,
      RenewalAssignmentRepository assignments,
      RenewalFlow flow,
      RenewalScope scope,
      RenewalParameters parameters,
      RenewalNotices notices,
      RenewalBatch batch,
      SalesOrganisationService sales,
      AppUserRepository users,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.assignments = assignments;
    this.flow = flow;
    this.scope = scope;
    this.parameters = parameters;
    this.notices = notices;
    this.batch = batch;
    this.sales = sales;
    this.users = users;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The officers the current user may assign to: the AOs of the user's units, or of every unit for
   * the Renewal processing team.
   *
   * @param companyId company
   * @return officers with their unit
   */
  @Transactional(readOnly = true)
  public List<Officer> officers(Long companyId) {
    RenewalScope.Scope userScope = scope.current(companyId);
    Set<String> units =
        userScope.kind() == RenewalScope.Kind.ALL ? null : new HashSet<>(userScope.units());
    Set<String> aos = new HashSet<>(users.findUsernamesWithPermission(Permission.RNW_DISPOSE));
    return sales.officers(companyId).stream()
        .filter(o -> aos.contains(o.getUsername()))
        .filter(o -> units == null || units.contains(o.getTeamCode()))
        .map(this::officer)
        .toList();
  }

  /**
   * Assigns or re-assigns renewals to an AO and pushes them to the AO's list.
   *
   * @param companyId company
   * @param refs renewals
   * @param ao account officer
   * @param reasonCode reason of a re-assignment (list RNW_TRANSFER_REASON), may be null
   * @return assigned and refused renewals
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public BatchOutcome assign(Long companyId, List<String> refs, String ao, String reasonCode) {
    AppUser officer = requireOfficer(ao);
    if (reasonCode != null && !reasonCode.isBlank()) {
      lovs.requireValid(RenewalCodes.LOV_TRANSFER_REASON, reasonCode, BusinessClock.today(clock));
    }
    String reason = reasonCode == null || reasonCode.isBlank() ? null : reasonCode;
    return batch.run(refs, ref -> assignOne(records.get(companyId, ref), officer, reason));
  }

  /** The display name of a user (the sign-in ID when the user is unknown). */
  private String nameOf(String login) {
    return users.findByUsernameIgnoreCase(login).map(AppUser::getFullName).orElse(login);
  }

  private void assignOne(RenewalCandidate c, AppUser officer, String reason) {
    requireAssignable(c, officer);
    String ao = officer.getUsername();
    String previous = c.getAssignedAo();
    if (ao.equals(previous)) {
      throw new BusinessRuleException(
          "RNW_ASSIGN_SAME", "Renewal " + c.getRenewalRef() + " is already assigned to " + ao);
    }
    c.assignAo(ao);
    assignments.save(
        new RenewalAssignment(c.getId(), RenewalAssignment.Role.AO, ao, previous, reason));
    if (c.getStage() == RenewalStage.UNASSIGNED) {
      flow.act(c, "assign", TransitionNote.comment("Assigned to " + officer.getFullName()));
    }
    flow.assign(c, ao);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        (previous == null ? "Assigned to " : "Re-assigned from " + nameOf(previous) + " to ")
            + officer.getFullName());
    notices.users(
        List.of(ao),
        RenewalCodes.EVENT_ASSIGNED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + " assigned to you",
            "Give the disposition of the renewal of "
                + c.getSnapshot().clientName()
                + ", expiring "
                + DisplayFormat.date(c.getExpiryDate())));
  }

  private void requireAssignable(RenewalCandidate c, AppUser officer) {
    RenewalRecords.requireStage(c, RenewalStage.UNASSIGNED, RenewalStage.FOR_DISPOSITION);
    RenewalRecords.requireUnlocked(c);
    var product = c.getSnapshot().product();
    if (product != null && parameters.loanDriven(product.segment(), product.lineCode())) {
      throw new BusinessRuleException(
          "RNW_ASSIGN_STP", "Renewal " + c.getRenewalRef() + " follows the loan-driven path");
    }
    String unit = c.getOwnerUnit();
    if (unit != null && !scope.unitsOf(c.getCompanyId(), officer.getUsername()).contains(unit)) {
      throw new BusinessRuleException(
          "RNW_ASSIGN_UNIT", officer.getFullName() + " is not an officer of unit " + unit);
    }
  }

  private AppUser requireOfficer(String ao) {
    if (ao == null || ao.isBlank()) {
      throw new BusinessRuleException("RNW_ASSIGN_AO", "Select the account officer");
    }
    AppUser user =
        users
            .findByUsernameIgnoreCase(ao.strip())
            .filter(AppUser::isEnabled)
            .orElseThrow(
                () -> new BusinessRuleException("RNW_ASSIGN_AO", ao + " is not an active user"));
    if (!users.findUsernamesWithPermission(Permission.RNW_DISPOSE).contains(user.getUsername())) {
      throw new BusinessRuleException(
          "RNW_ASSIGN_AO", user.getFullName() + " cannot give renewal dispositions");
    }
    return user;
  }

  private Officer officer(SalesOfficer o) {
    String name =
        users
            .findByUsernameIgnoreCase(o.getUsername())
            .map(AppUser::getFullName)
            .orElse(o.getUsername());
    return new Officer(o.getUsername(), name, o.getTeamCode());
  }

  /**
   * An officer to assign to.
   *
   * @param username user name
   * @param fullName name
   * @param unit sales unit
   */
  public record Officer(String username, String fullName, String unit) {}
}
