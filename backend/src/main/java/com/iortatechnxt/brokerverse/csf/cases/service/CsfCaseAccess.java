package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may read a case and which actions the signed-in user may take on it in its status (FRS BRD-9
 * section 5.3; FR-CSF-053, 056, 060): the CSF roles see every case, a member of a fulfilment unit
 * the cases of his units only; the unit resolves, returns or asks for CSR action on a case it
 * holds, the agent re-endorses or closes his returned cases, the supervisor reassigns, escalates,
 * recategorises and closes any open case.
 */
@Component
@Transactional(readOnly = true)
public class CsfCaseAccess {

  /** Resolve and close (fulfilment unit). */
  public static final String RESOLVE = "RESOLVE";

  /** Return to the agent. */
  public static final String RETURN = "RETURN";

  /** Request CSR action. */
  public static final String CSR_ACTION = "CSR_ACTION";

  /** Re-endorse to the fulfilment unit. */
  public static final String REENDORSE = "REENDORSE";

  /** Close as Closed by CSR. */
  public static final String CLOSE = "CLOSE";

  /** Reassign. */
  public static final String REASSIGN = "REASSIGN";

  /** Escalate. */
  public static final String ESCALATE = "ESCALATE";

  /** Change category. */
  public static final String CHANGE_CATEGORY = "CHANGE_CATEGORY";

  /** Add a note. */
  public static final String NOTE = "NOTE";

  /** Upload a document. */
  public static final String UPLOAD = "UPLOAD";

  /** Create a follow-up case. */
  public static final String FOLLOW_UP = "FOLLOW_UP";

  private static final String DENIED = "You are not permitted to perform this action";

  private final CsfCaseStore cases;
  private final CsfCaseSetupStore setup;
  private final CurrentUser currentUser;

  /**
   * Creates the access rules.
   *
   * @param cases cases
   * @param setup fulfilment units of the user
   * @param currentUser signed-in user
   */
  public CsfCaseAccess(CsfCaseStore cases, CsfCaseSetupStore setup, CurrentUser currentUser) {
    this.cases = cases;
    this.setup = setup;
    this.currentUser = currentUser;
  }

  /**
   * The case, when the signed-in user may read it; otherwise not found.
   *
   * @param id case
   * @return case
   */
  public CsfCase require(Long id) {
    CsfCase c = cases.find(id).orElseThrow(() -> new ResourceNotFoundException("Case", id));
    if (!mayRead(c)) {
      throw new ResourceNotFoundException("Case", id);
    }
    return c;
  }

  /**
   * An open case the signed-in user may read.
   *
   * @param id case
   * @return case
   */
  public CsfCase requireOpen(Long id) {
    CsfCase c = require(id);
    if (c.status().closed()) {
      throw notAllowed(c);
    }
    return c;
  }

  /**
   * A case with the fulfilment unit of the signed-in user, waiting for the unit.
   *
   * @param id case
   * @return case
   */
  public CsfCase requireFulfilment(Long id) {
    requirePermission(Permission.CSF_CASE_FULFIL);
    CsfCase c = require(id);
    if (!isUnitMember(c)) {
      throw new AccessDeniedException(DENIED);
    }
    if (c.status() != CaseStatus.WITH_FULFILMENT) {
      throw notAllowed(c);
    }
    return c;
  }

  /**
   * Whether the signed-in user may read a case.
   *
   * @param c case
   * @return true when allowed
   */
  public boolean mayRead(CsfCase c) {
    return has(Permission.CSF_VIEW) || isUnitMember(c);
  }

  /**
   * Whether the signed-in user is a member of the fulfilment unit of a case.
   *
   * @param c case
   * @return true for a member
   */
  public boolean isUnitMember(CsfCase c) {
    return has(Permission.CSF_CASE_FULFIL)
        && setup.unitsOf(currentUser.username()).contains(c.category().fulfilmentUnit());
  }

  /**
   * The actions the signed-in user may take on a case now.
   *
   * @param c case
   * @return action codes
   */
  public List<String> actionsOn(CsfCase c) {
    if (c.status().closed()) {
      return has(Permission.CSF_CASE_CREATE) ? List.of(FOLLOW_UP) : List.of();
    }
    return openActions(c);
  }

  private List<String> openActions(CsfCase c) {
    List<String> actions = new ArrayList<>();
    if (isUnitMember(c) && c.status() == CaseStatus.WITH_FULFILMENT) {
      actions.addAll(List.of(RESOLVE, RETURN, CSR_ACTION));
    }
    if (mayHandleReturned(c)) {
      actions.addAll(List.of(REENDORSE, CLOSE));
    }
    boolean supervisor = has(Permission.CSF_CASE_SUPERVISE);
    if (supervisor) {
      actions.addAll(supervisorActions(c, actions.contains(CLOSE)));
    }
    if (supervisor || has(Permission.CSF_CASE_CREATE) || isUnitMember(c)) {
      actions.addAll(List.of(NOTE, UPLOAD));
    }
    return actions;
  }

  /**
   * Checks that the signed-in user may add notes and documents to a case.
   *
   * @param c case
   */
  public void requireMayWork(CsfCase c) {
    if (!actionsOn(c).contains(NOTE)) {
      throw new AccessDeniedException(DENIED);
    }
  }

  /**
   * Checks that the agent may act on a returned case: its owner, or a supervisor.
   *
   * @param c case
   */
  public void requireOwnerOrSupervisor(CsfCase c) {
    if (!isOwner(c) && !has(Permission.CSF_CASE_SUPERVISE)) {
      throw new AccessDeniedException(DENIED);
    }
  }

  /**
   * Checks a permission of the signed-in user.
   *
   * @param p permission
   */
  public void requirePermission(Permission p) {
    if (!has(p)) {
      throw new AccessDeniedException(DENIED);
    }
  }

  /**
   * Whether the signed-in user holds a permission.
   *
   * @param p permission
   * @return true when held
   */
  public boolean has(Permission p) {
    return currentUser.hasAuthority(p.name());
  }

  /**
   * The refusal of an action the status does not allow.
   *
   * @param c case
   * @return exception
   */
  public static BusinessRuleException notAllowed(CsfCase c) {
    return new BusinessRuleException(
        "CSF_CASE_ACTION_NOT_ALLOWED",
        "The case is " + c.status().label() + ". This action is not allowed");
  }

  private boolean mayHandleReturned(CsfCase c) {
    return has(Permission.CSF_CASE_CREATE)
        && CaseStatus.withAgent().contains(c.status())
        && (isOwner(c) || has(Permission.CSF_CASE_SUPERVISE));
  }

  private static List<String> supervisorActions(CsfCase c, boolean closeOffered) {
    List<String> actions = new ArrayList<>();
    if (!closeOffered) {
      actions.add(CLOSE);
    }
    actions.addAll(List.of(REASSIGN, CHANGE_CATEGORY));
    if (!c.escalation().escalated()) {
      actions.add(ESCALATE);
    }
    return actions;
  }

  private boolean isOwner(CsfCase c) {
    return CurrentUser.sameUser(c.owner(), currentUser.username());
  }
}
