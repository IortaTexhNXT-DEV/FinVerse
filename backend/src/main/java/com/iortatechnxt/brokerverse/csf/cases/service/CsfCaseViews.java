package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Answers of the case screens: list rows and the case record, with the labels of the case lists,
 * the status group, the age in working days and the SLA status computed for today (FR-CSF-057).
 */
@Component
public class CsfCaseViews {

  private static final int DEFAULT_NEAR_BREACH = 1;

  private final LovService lovs;
  private final CsfCaseCalendar calendar;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the views.
   *
   * @param lovs list labels
   * @param calendar working days
   * @param parameters near-breach threshold
   * @param clock clock
   */
  public CsfCaseViews(
      LovService lovs, CsfCaseCalendar calendar, SystemParameterService parameters, Clock clock) {
    this.lovs = lovs;
    this.calendar = calendar;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * A code with its label.
   *
   * @param code code
   * @param label label
   */
  public record Coded(String code, String label) {}

  /**
   * The age and SLA status of a case.
   *
   * @param ageDays working days from the creation to the closure or today
   * @param status WITHIN_TAT, APPROACHING or BREACHED
   * @param label words of the status
   */
  public record SlaView(int ageDays, String status, String label) {}

  /**
   * One row of a case list.
   *
   * @param id id
   * @param caseNo Case ID
   * @param createdAt creation time
   * @param client client code and name, or the caller
   * @param clientId client, null for a caller
   * @param arn ARN
   * @param policyNo policy number
   * @param caseType case type
   * @param tatDays turnaround time in working days
   * @param nature nature of transaction
   * @param fulfilmentUnit fulfilment unit
   * @param marketSegment market segment
   * @param priority priority
   * @param owner owner
   * @param agent agent who logged the case
   * @param status status code
   * @param statusLabel status label
   * @param statusGroup status group
   * @param dueDate due date
   * @param sla age and SLA status
   * @param escalated escalation reason and date, null when not escalated
   * @param subject subject
   */
  public record CaseRow(
      Long id,
      String caseNo,
      Instant createdAt,
      String client,
      Long clientId,
      String arn,
      String policyNo,
      Coded caseType,
      int tatDays,
      Coded nature,
      Coded fulfilmentUnit,
      Coded marketSegment,
      Coded priority,
      String owner,
      String agent,
      String status,
      String statusLabel,
      String statusGroup,
      LocalDate dueDate,
      SlaView sla,
      Escalated escalated,
      String subject) {}

  /**
   * The escalation indicator.
   *
   * @param reason reason
   * @param at time
   */
  public record Escalated(Coded reason, Instant at) {}

  /**
   * A linked case.
   *
   * @param id id
   * @param caseNo Case ID
   * @param relation ORIGINAL or FOLLOW_UP
   * @param statusLabel status
   * @param reason reason of the follow-up
   */
  public record Linked(
      Long id, String caseNo, String relation, String statusLabel, String reason) {}

  /**
   * The case record.
   *
   * @param row header values
   * @param companyId company
   * @param channel channel
   * @param productType product type
   * @param description description
   * @param callerContact contact of a caller who is not a client
   * @param returnReason reason of the last return
   * @param instruction instruction of the last CSR action request
   * @param resolution resolution
   * @param closureReason closure reason
   * @param closedAt closure time
   * @param closedBy user who closed
   * @param linked original and follow-up cases
   * @param actions actions the user may take now
   */
  public record CaseRecord(
      CaseRow row,
      Long companyId,
      Coded channel,
      Coded productType,
      String description,
      String callerContact,
      Coded returnReason,
      String instruction,
      String resolution,
      Coded closureReason,
      Instant closedAt,
      String closedBy,
      List<Linked> linked,
      List<String> actions) {}

  /**
   * A list row.
   *
   * @param c case
   * @return row
   */
  public CaseRow row(CsfCase c) {
    return row(c, calendar.of(c.companyId()));
  }

  /**
   * List rows of one company.
   *
   * @param companyId company
   * @param cases cases
   * @return rows
   */
  public List<CaseRow> rows(Long companyId, List<CsfCase> cases) {
    Predicate<LocalDate> working = calendar.of(companyId);
    return cases.stream().map(c -> row(c, working)).toList();
  }

  /**
   * The age and SLA status of a case for today.
   *
   * @param c case
   * @param working calendar of the company
   * @return SLA view
   */
  public SlaView sla(CsfCase c, Predicate<LocalDate> working) {
    LocalDate created = BusinessClock.dateOf(c.createdAt());
    LocalDate end =
        c.status().closed() && c.handling().closedAt() != null
            ? BusinessClock.dateOf(c.handling().closedAt())
            : BusinessClock.today(clock);
    int age = CsfCaseCalendar.between(created, end, working);
    String status;
    if (end.isAfter(c.dueDate())) {
      status = CsfCaseCodes.BREACHED;
    } else if (!c.status().closed()
        && CsfCaseCalendar.between(end, c.dueDate(), working) <= nearBreachDays()) {
      status = CsfCaseCodes.APPROACHING;
    } else {
      status = CsfCaseCodes.WITHIN_TAT;
    }
    return new SlaView(age, status, slaLabel(status));
  }

  /**
   * Words of an SLA status.
   *
   * @param status code
   * @return label
   */
  public static String slaLabel(String status) {
    return switch (status) {
      case CsfCaseCodes.BREACHED -> "Breached";
      case CsfCaseCodes.APPROACHING -> "Approaching breach";
      default -> "Within TAT";
    };
  }

  /**
   * The near-breach threshold.
   *
   * @return working days before the due date
   */
  public int nearBreachDays() {
    return parameters.intValue(CsfCaseCodes.PARAM_NEAR_BREACH, DEFAULT_NEAR_BREACH);
  }

  /**
   * A code with the label of its list.
   *
   * @param lov list
   * @param code code
   * @return coded value, null for a null code
   */
  public Coded coded(String lov, String code) {
    return code == null ? null : new Coded(code, lovs.label(lov, code));
  }

  /**
   * The case record.
   *
   * @param c case
   * @param linked original and follow-up cases
   * @param actions allowed actions
   * @return record
   */
  public CaseRecord record(CsfCase c, List<Linked> linked, List<String> actions) {
    return new CaseRecord(
        row(c),
        c.companyId(),
        coded(CsfCaseCodes.LOV_CHANNEL, c.category().channel()),
        coded(CsfCaseCodes.LOV_PRODUCT, c.category().productType()),
        c.description(),
        c.party().callerContact(),
        coded(CsfCaseCodes.LOV_RETURN, c.handling().returnReason()),
        c.handling().instruction(),
        c.handling().resolution(),
        coded(CsfCaseCodes.LOV_CLOSURE, c.handling().closureReason()),
        c.handling().closedAt(),
        c.handling().closedBy(),
        linked,
        actions);
  }

  private CaseRow row(CsfCase c, Predicate<LocalDate> working) {
    CsfCase.Category k = c.category();
    CaseStatus s = c.status();
    return new CaseRow(
        c.id(),
        c.caseNo(),
        c.createdAt(),
        c.party().clientId() == null
            ? c.party().callerName()
            : c.party().clientCode() + " " + c.party().clientName(),
        c.party().clientId(),
        c.account().arn(),
        c.account().policyNo(),
        coded(CsfCaseCodes.LOV_TYPE, k.caseType()),
        c.tatDays(),
        coded(CsfCaseCodes.LOV_NATURE, k.nature()),
        coded(CsfCaseCodes.LOV_UNIT, k.fulfilmentUnit()),
        coded(CsfCaseCodes.LOV_SEGMENT, k.marketSegment()),
        coded(CsfCaseCodes.LOV_PRIORITY, k.priority()),
        c.owner(),
        c.agent(),
        s.name(),
        s.label(),
        s.group(),
        c.dueDate(),
        sla(c, working),
        c.escalation().escalated()
            ? new Escalated(
                coded(CsfCaseCodes.LOV_ESCALATION, c.escalation().reason()), c.escalation().at())
            : null,
        c.subject());
  }
}
