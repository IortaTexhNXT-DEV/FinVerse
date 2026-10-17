package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore.Member;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore.Route;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore.TypeSetting;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Case Routing screen (FR-CSF-051, 060, 061): the turnaround time of each case type, the
 * routing table that proposes the fulfilment unit of a case and the members of the fulfilment
 * units. Every change is in the audit trail with the values before and after.
 */
@Service
@Transactional
public class CsfCaseSetupService {

  private static final String ENTITY_ROUTING = "CsfCaseRouting";
  private static final int MAX_TAT = 365;
  private static final int TYPE_WEIGHT = 4;
  private static final int SEGMENT_WEIGHT = 2;

  private final CsfCaseSetupStore store;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param store configuration
   * @param lovs case lists
   * @param audit audit trail
   * @param currentUser signed-in user
   * @param clock clock
   */
  public CsfCaseSetupService(
      CsfCaseSetupStore store,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.store = store;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The turnaround times.
   *
   * @return settings
   */
  @Transactional(readOnly = true)
  public List<TypeSetting> typeSettings() {
    return store.typeSettings();
  }

  /**
   * Sets the turnaround time of a case type.
   *
   * @param s setting
   * @return settings
   */
  public List<TypeSetting> saveTypeSetting(TypeSetting s) {
    requireValue(CsfCaseCodes.LOV_TYPE, s.caseType(), "case type");
    if (s.tatDays() < 0 || s.tatDays() > MAX_TAT) {
      throw new BusinessRuleException(
          "CSF_CASE_TAT_INVALID", "Enter the turnaround time in whole working days");
    }
    Optional<TypeSetting> before = store.typeSetting(s.caseType());
    store.saveTypeSetting(s, currentUser.username());
    audit.recordChange(
        ENTITY_ROUTING,
        s.caseType(),
        AuditAction.UPDATE,
        "Turnaround time of case type " + lovs.label(CsfCaseCodes.LOV_TYPE, s.caseType()),
        before.map(CsfCaseSetupService::text).orElse(null),
        text(s));
    return store.typeSettings();
  }

  /**
   * The routing table.
   *
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<Route> routes() {
    return store.routes();
  }

  /**
   * Adds or changes a routing row.
   *
   * @param r row (id null for a new row); empty keys match any value
   * @return rows
   */
  public List<Route> saveRoute(Route r) {
    String type = blank(r.caseType());
    String segment = blank(r.marketSegment());
    String nature = blank(r.nature());
    optionalValue(CsfCaseCodes.LOV_TYPE, type, "case type");
    optionalValue(CsfCaseCodes.LOV_SEGMENT, segment, "market segment");
    optionalValue(CsfCaseCodes.LOV_NATURE, nature, "nature of transaction");
    requireValue(CsfCaseCodes.LOV_UNIT, r.fulfilmentUnit(), "fulfilment unit");
    String group =
        r.escalationGroup() == null || r.escalationGroup().isBlank() ? null : r.escalationGroup();
    if (group != null) {
      requireValue(CsfCaseCodes.LOV_UNIT, group, "escalation group");
    }
    if (store.routeExists(type, segment, nature, r.id())) {
      throw new BusinessRuleException(
          "CSF_CASE_ROUTE_DUPLICATE",
          "A routing row already exists for this case type, market segment and nature of"
              + " transaction");
    }
    Route clean = new Route(r.id(), type, segment, nature, r.fulfilmentUnit(), group, r.active());
    String before =
        r.id() == null
            ? null
            : store.routes().stream()
                .filter(x -> x.id().equals(r.id()))
                .findFirst()
                .map(CsfCaseSetupService::text)
                .orElse(null);
    store.saveRoute(clean, currentUser.username());
    audit.recordChange(
        ENTITY_ROUTING,
        String.join("/", type, segment, nature),
        r.id() == null ? AuditAction.CREATE : AuditAction.UPDATE,
        "Case routing row",
        before,
        text(clean));
    return store.routes();
  }

  /**
   * The fulfilment unit proposed for a case: the most specific active routing row matching the case
   * type, the market segment and the nature of transaction (a blank key matches any value).
   *
   * @param caseType case type
   * @param segment market segment
   * @param nature nature of transaction
   * @return fulfilment unit
   */
  @Transactional(readOnly = true)
  public Optional<String> propose(String caseType, String segment, String nature) {
    return store.routes().stream()
        .filter(Route::active)
        .filter(r -> fits(r.caseType(), caseType))
        .filter(r -> fits(r.marketSegment(), segment))
        .filter(r -> fits(r.nature(), nature))
        .max(Comparator.comparingInt(CsfCaseSetupService::specificity))
        .map(Route::fulfilmentUnit);
  }

  /**
   * The members of the fulfilment units.
   *
   * @return members
   */
  @Transactional(readOnly = true)
  public List<Member> members() {
    return store.members();
  }

  /**
   * Adds a member to a unit.
   *
   * @param unitCode unit
   * @param username user ID
   * @return members
   */
  public List<Member> addMember(String unitCode, String username) {
    requireValue(CsfCaseCodes.LOV_UNIT, unitCode, "fulfilment unit");
    if (username == null || username.isBlank()) {
      throw new BusinessRuleException("CSF_CASE_MEMBER_REQUIRED", "Enter the user ID");
    }
    store.addMember(unitCode, username.strip(), currentUser.username());
    audit.recordChange(
        ENTITY_ROUTING,
        unitCode,
        AuditAction.UPDATE,
        "Fulfilment unit member added",
        null,
        username.strip());
    return store.members();
  }

  /**
   * Removes a member.
   *
   * @param id membership
   * @return members
   */
  public List<Member> removeMember(Long id) {
    Optional<Member> m = store.members().stream().filter(x -> x.id().equals(id)).findFirst();
    store.removeMember(id);
    m.ifPresent(
        x ->
            audit.recordChange(
                ENTITY_ROUTING,
                x.unitCode(),
                AuditAction.UPDATE,
                "Fulfilment unit member removed",
                x.username(),
                null));
    return store.members();
  }

  private static int specificity(Route r) {
    return (r.caseType().isEmpty() ? 0 : TYPE_WEIGHT)
        + (r.marketSegment().isEmpty() ? 0 : SEGMENT_WEIGHT)
        + (r.nature().isEmpty() ? 0 : 1);
  }

  private static boolean fits(String key, String value) {
    return key.isEmpty() || key.equals(value);
  }

  private void requireValue(String lov, String code, String name) {
    if (code == null || code.isBlank()) {
      throw new BusinessRuleException("CSF_CASE_FIELDS_REQUIRED", "Select the " + name);
    }
    lovs.requireValid(lov, code, BusinessClock.today(clock));
  }

  private void optionalValue(String lov, String code, String name) {
    if (!code.isEmpty()) {
      requireValue(lov, code, name);
    }
  }

  private static String blank(String s) {
    return s == null ? "" : s.strip();
  }

  private static String text(TypeSetting s) {
    return s.tatDays() + " working days" + (s.autoEscalate() ? ", escalated on breach" : "");
  }

  private static String text(Route r) {
    return r.fulfilmentUnit()
        + (r.escalationGroup() == null ? "" : ", escalation " + r.escalationGroup())
        + (r.active() ? "" : ", inactive");
  }
}
