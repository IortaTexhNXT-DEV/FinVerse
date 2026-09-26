package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSla.SlaState;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteria of the case list and tiles (SNSRP-402, 403, 405; FR-SS-041, 042, 045): the user's scope,
 * the tab, the search text and the filters. Dates are Philippine calendar days.
 */
final class CaseSpecs {

  /** Philippine time, the business calendar of the case list. */
  static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private static final String STAGE = "stage";
  private static final String STATUS = "status";
  private static final String ASSIGNEE = "assignee";
  private static final String DUE_AT = "dueAt";
  private static final String BREACHED = "breached";
  private static final String CREATED_AT = "createdAt";

  private CaseSpecs() {}

  /**
   * Cases of a company.
   *
   * @param companyId company
   * @return criteria
   */
  static Specification<ScreeningCase> company(Long companyId) {
    return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
  }

  /**
   * The team scope of an investigator (FR-SS-041 R1): assigned to the user, investigated by the
   * user, in the user's sales team, or waiting in a queue of a stage the user works.
   *
   * @param user the user
   * @param team the user's sales team, may be null
   * @param ownStages the stages whose owner permission the user holds
   * @return criteria
   */
  static Specification<ScreeningCase> teamScope(
      String user, String team, Collection<CaseStage> ownStages) {
    String me = user.toLowerCase(Locale.ROOT);
    return (root, query, cb) -> {
      List<Predicate> any = new ArrayList<>();
      any.add(cb.equal(cb.lower(root.get(ASSIGNEE)), me));
      any.add(cb.equal(cb.lower(root.get("investigator")), me));
      if (team != null) {
        any.add(cb.equal(root.get("teamCode"), team));
      }
      if (!ownStages.isEmpty()) {
        any.add(cb.and(cb.isNull(root.get(ASSIGNEE)), root.get(STAGE).in(ownStages)));
      }
      return cb.or(any.toArray(Predicate[]::new));
    };
  }

  /**
   * A status tab.
   *
   * @param tab the tab
   * @param user the user (MY)
   * @return criteria, null for ALL
   */
  static Specification<ScreeningCase> tab(CaseSearch.Tab tab, String user) {
    return switch (tab) {
      case MY ->
          (root, query, cb) ->
              cb.and(
                  cb.equal(cb.lower(root.get(ASSIGNEE)), user.toLowerCase(Locale.ROOT)),
                  cb.equal(root.get(STATUS), CaseStatus.OPEN));
      case TEAM -> (root, query, cb) -> cb.equal(root.get(STATUS), CaseStatus.OPEN);
      case APPROVAL -> stages(List.of(CaseStage.UNIT_HEAD_APPROVAL, CaseStage.COMPLIANCE_REVIEW));
      case COMMITTEE -> stages(List.of(CaseStage.AML_COMMITTEE));
      case STR -> stages(List.of(CaseStage.STR_PREPARATION, CaseStage.STR_EXTRACTION));
      case CLOSED -> (root, query, cb) -> cb.equal(root.get(STATUS), CaseStatus.CLOSED);
      case ALL -> null;
    };
  }

  private static Specification<ScreeningCase> stages(List<CaseStage> stages) {
    return (root, query, cb) -> root.get(STAGE).in(stages);
  }

  /**
   * The search text on the case number, client name and client code (case-insensitive).
   *
   * @param q the text, at least 3 characters
   * @return criteria
   */
  static Specification<ScreeningCase> text(String q) {
    String like = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("caseNo")), like),
            cb.like(cb.lower(root.get("clientName")), like),
            cb.like(cb.lower(root.get("clientCode")), like));
  }

  /**
   * An exact attribute filter.
   *
   * @param attribute the attribute
   * @param value the value
   * @return criteria
   */
  static Specification<ScreeningCase> equal(String attribute, Object value) {
    return (root, query, cb) -> cb.equal(root.get(attribute), value);
  }

  /**
   * The assignee filter (case-insensitive).
   *
   * @param user the assignee
   * @return criteria
   */
  static Specification<ScreeningCase> assignee(String user) {
    return (root, query, cb) ->
        cb.equal(cb.lower(root.get(ASSIGNEE)), user.toLowerCase(Locale.ROOT));
  }

  /**
   * Created on or after a day.
   *
   * @param day the day
   * @return criteria
   */
  static Specification<ScreeningCase> createdFrom(LocalDate day) {
    Instant start = day.atStartOfDay(MANILA).toInstant();
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(CREATED_AT), start);
  }

  /**
   * Created on or before a day.
   *
   * @param day the day
   * @return criteria
   */
  static Specification<ScreeningCase> createdTo(LocalDate day) {
    Instant end = day.plusDays(1).atStartOfDay(MANILA).toInstant();
    return (root, query, cb) -> cb.lessThan(root.get(CREATED_AT), end);
  }

  /**
   * The SLA state of open cases at a time (FR-SS-044 badge).
   *
   * @param state the state
   * @param now the time
   * @return criteria
   */
  static Specification<ScreeningCase> sla(SlaState state, Instant now) {
    return (root, query, cb) -> {
      Predicate open = cb.equal(root.get(STATUS), CaseStatus.OPEN);
      Predicate late =
          cb.or(cb.isTrue(root.get(BREACHED)), cb.lessThanOrEqualTo(root.get(DUE_AT), now));
      return switch (state) {
        case BREACHED -> cb.and(open, late);
        case DUE_SOON ->
            cb.and(open, cb.not(late), cb.lessThanOrEqualTo(root.get("remindAt"), now));
        case ON_TIME -> cb.and(open, cb.not(late), cb.greaterThan(root.get("remindAt"), now));
        case NONE -> cb.or(cb.not(open), cb.isNull(root.get(DUE_AT)));
      };
    };
  }

  /**
   * Open cases due before a time and not breached (the "due today" tile).
   *
   * @param before the end of the day
   * @return criteria
   */
  static Specification<ScreeningCase> dueBefore(Instant before) {
    return (root, query, cb) ->
        cb.and(
            cb.equal(root.get(STATUS), CaseStatus.OPEN),
            cb.isFalse(root.get(BREACHED)),
            cb.lessThan(root.get(DUE_AT), before));
  }
}
