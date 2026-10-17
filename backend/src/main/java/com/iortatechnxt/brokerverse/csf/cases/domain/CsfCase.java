package com.iortatechnxt.brokerverse.csf.cases.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A case of the contact centre (FR-CSF-050 to 060), as stored in {@code csf_case}.
 *
 * @param id id
 * @param version optimistic lock version
 * @param companyId company
 * @param caseNo Case ID, prefix-yyyy-nnnnnn
 * @param party client or caller
 * @param account account and policy, may be empty
 * @param category channel, type, nature, product, segment, unit and priority
 * @param subject subject
 * @param description description of the request
 * @param status status
 * @param owner user who owns the case (the agent, or the user it was reassigned to)
 * @param agent agent who logged the case
 * @param tatDays turnaround time of the case type in working days
 * @param dueDate due date
 * @param sla SLA status and notices sent
 * @param escalation escalation indicator
 * @param handling return reason, instruction, resolution and closure
 * @param parentCaseId original case of a follow-up case
 * @param followUpReason reason of the follow-up case
 * @param createdAt creation time
 */
public record CsfCase(
    Long id,
    long version,
    Long companyId,
    String caseNo,
    Party party,
    AccountRef account,
    Category category,
    String subject,
    String description,
    CaseStatus status,
    String owner,
    String agent,
    int tatDays,
    LocalDate dueDate,
    Sla sla,
    Escalation escalation,
    Handling handling,
    Long parentCaseId,
    String followUpReason,
    Instant createdAt) {

  /**
   * The client of the case, or the caller who is not a client.
   *
   * @param clientId client, null for a caller
   * @param clientCode client code
   * @param clientName client name
   * @param callerName caller name (no client)
   * @param callerContact caller mobile or e-mail (no client)
   */
  public record Party(
      Long clientId,
      String clientCode,
      String clientName,
      String callerName,
      String callerContact) {}

  /**
   * The account of the case.
   *
   * @param accountId account, may be null
   * @param arn ARN
   * @param policyNo policy number
   */
  public record AccountRef(Long accountId, String arn, String policyNo) {}

  /**
   * The categories of the case (codes of the case lists of values).
   *
   * @param channel channel
   * @param caseType case type
   * @param nature nature of transaction
   * @param productType product type
   * @param marketSegment market segment
   * @param fulfilmentUnit fulfilment unit
   * @param priority priority
   */
  public record Category(
      String channel,
      String caseType,
      String nature,
      String productType,
      String marketSegment,
      String fulfilmentUnit,
      String priority) {}

  /**
   * The SLA status as last stored by the SLA check, and the notices sent.
   *
   * @param status WITHIN_TAT, APPROACHING or BREACHED
   * @param nearNotified approaching breach notice sent
   * @param breachNotified breach notice sent
   */
  public record Sla(String status, boolean nearNotified, boolean breachNotified) {}

  /**
   * The escalation indicator.
   *
   * @param escalated escalated
   * @param reason escalation reason
   * @param at time of the escalation
   */
  public record Escalation(boolean escalated, String reason, Instant at) {}

  /**
   * The handling of the case.
   *
   * @param returnReason reason of the last return to the agent
   * @param instruction instruction of the last CSR action request
   * @param resolution resolution
   * @param closureReason closure reason
   * @param closedAt closure time
   * @param closedBy user who closed the case
   */
  public record Handling(
      String returnReason,
      String instruction,
      String resolution,
      String closureReason,
      Instant closedAt,
      String closedBy) {}

  /**
   * This case with other categories.
   *
   * @param c categories
   * @return case
   */
  public CsfCase withCategory(Category c) {
    return new CsfCase(
        id,
        version,
        companyId,
        caseNo,
        party,
        account,
        c,
        subject,
        description,
        status,
        owner,
        agent,
        tatDays,
        dueDate,
        sla,
        escalation,
        handling,
        parentCaseId,
        followUpReason,
        createdAt);
  }

  /**
   * This case in another status with its handling.
   *
   * @param s status
   * @param h handling
   * @return case
   */
  public CsfCase withStatus(CaseStatus s, Handling h) {
    return new CsfCase(
        id,
        version,
        companyId,
        caseNo,
        party,
        account,
        category,
        subject,
        description,
        s,
        owner,
        agent,
        tatDays,
        dueDate,
        sla,
        escalation,
        h,
        parentCaseId,
        followUpReason,
        createdAt);
  }

  /**
   * This case with another owner.
   *
   * @param o owner
   * @return case
   */
  public CsfCase withOwner(String o) {
    return new CsfCase(
        id,
        version,
        companyId,
        caseNo,
        party,
        account,
        category,
        subject,
        description,
        status,
        o,
        agent,
        tatDays,
        dueDate,
        sla,
        escalation,
        handling,
        parentCaseId,
        followUpReason,
        createdAt);
  }

  /**
   * This case with another turnaround time and due date.
   *
   * @param tat turnaround time
   * @param due due date
   * @return case
   */
  public CsfCase withDue(int tat, LocalDate due) {
    return new CsfCase(
        id,
        version,
        companyId,
        caseNo,
        party,
        account,
        category,
        subject,
        description,
        status,
        owner,
        agent,
        tat,
        due,
        sla,
        escalation,
        handling,
        parentCaseId,
        followUpReason,
        createdAt);
  }

  /**
   * This case with another escalation indicator.
   *
   * @param e escalation
   * @return case
   */
  public CsfCase withEscalation(Escalation e) {
    return new CsfCase(
        id,
        version,
        companyId,
        caseNo,
        party,
        account,
        category,
        subject,
        description,
        status,
        owner,
        agent,
        tatDays,
        dueDate,
        sla,
        e,
        handling,
        parentCaseId,
        followUpReason,
        createdAt);
  }

  /**
   * This case with another SLA status.
   *
   * @param s SLA status
   * @return case
   */
  public CsfCase withSla(Sla s) {
    return new CsfCase(
        id,
        version,
        companyId,
        caseNo,
        party,
        account,
        category,
        subject,
        description,
        status,
        owner,
        agent,
        tatDays,
        dueDate,
        s,
        escalation,
        handling,
        parentCaseId,
        followUpReason,
        createdAt);
  }

  /**
   * The categories with other values; null keeps a value.
   *
   * @param caseType case type
   * @param nature nature
   * @param unit fulfilment unit
   * @param priority priority
   * @return categories
   */
  public Category recategorised(String caseType, String nature, String unit, String priority) {
    Category k = category;
    return new Category(
        k.channel(),
        caseType == null ? k.caseType() : caseType,
        nature == null ? k.nature() : nature,
        k.productType(),
        k.marketSegment(),
        unit == null ? k.fulfilmentUnit() : unit,
        priority == null ? k.priority() : priority);
  }
}
