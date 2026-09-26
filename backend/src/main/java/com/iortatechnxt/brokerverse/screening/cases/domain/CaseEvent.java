package com.iortatechnxt.brokerverse.screening.cases.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One entry of the insert-only case timeline (SNSRP-401, 404, 405, 701-704, 902, 903; design 4.4):
 * the event, the round, the stages and values before and after, the reason, the remarks, the actor
 * and the time. A database trigger refuses updates and deletes (FR-SS-091).
 */
@Entity
@Table(name = "scr_case_event")
public class CaseEvent extends BaseEntity {

  private static final int MAX_VALUE = 300;
  private static final int MAX_REMARKS = 4000;

  @Column(name = "case_id", nullable = false, updatable = false)
  private Long caseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event", nullable = false, length = 30, updatable = false)
  private CaseEventType event;

  @Column(name = "round_no", nullable = false, updatable = false)
  private int roundNo;

  @Column(name = "from_stage", length = 30, updatable = false)
  private String fromStage;

  @Column(name = "to_stage", length = 30, updatable = false)
  private String toStage;

  @Column(name = "from_value", length = MAX_VALUE, updatable = false)
  private String fromValue;

  @Column(name = "to_value", length = MAX_VALUE, updatable = false)
  private String toValue;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  @Column(name = "remarks", length = MAX_REMARKS, updatable = false)
  private String remarks;

  @Column(name = "actor", nullable = false, length = 50, updatable = false)
  private String actor;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  /** For JPA. */
  protected CaseEvent() {}

  /**
   * Records an event.
   *
   * @param c the case (its round)
   * @param event the event
   * @param facts stages, values, reason and remarks
   * @param actor the user or SYSTEM
   * @param at when
   */
  public CaseEvent(
      ScreeningCase c, CaseEventType event, EventFacts facts, String actor, Instant at) {
    this.caseId = c.getId();
    this.roundNo = c.getRoundNo();
    this.event = event;
    this.fromStage = facts.fromStage();
    this.toStage = facts.toStage();
    this.fromValue = cap(facts.fromValue(), MAX_VALUE);
    this.toValue = cap(facts.toValue(), MAX_VALUE);
    this.reasonCode = facts.reasonCode();
    this.remarks = cap(facts.remarks(), MAX_REMARKS);
    this.actor = actor;
    this.occurredAt = at;
  }

  private static String cap(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  public Long getCaseId() {
    return caseId;
  }

  public CaseEventType getEvent() {
    return event;
  }

  public int getRoundNo() {
    return roundNo;
  }

  public String getFromStage() {
    return fromStage;
  }

  public String getToStage() {
    return toStage;
  }

  public String getFromValue() {
    return fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getActor() {
    return actor;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  /**
   * What an event records besides its type.
   *
   * @param fromStage stage before, may be null
   * @param toStage stage after, may be null
   * @param fromValue value before (assignee, rating...), may be null
   * @param toValue value after, may be null
   * @param reasonCode reason or disposition code, may be null
   * @param remarks remarks, may be null
   */
  public record EventFacts(
      String fromStage,
      String toStage,
      String fromValue,
      String toValue,
      String reasonCode,
      String remarks) {

    /**
     * Facts with remarks only.
     *
     * @param remarks remarks
     * @return facts
     */
    public static EventFacts remarks(String remarks) {
      return new EventFacts(null, null, null, null, null, remarks);
    }

    /**
     * A stage change.
     *
     * @param from stage before
     * @param to stage after
     * @param reasonCode reason or disposition
     * @param remarks remarks
     * @return facts
     */
    public static EventFacts move(String from, String to, String reasonCode, String remarks) {
      return new EventFacts(from, to, null, null, reasonCode, remarks);
    }

    /**
     * A value change.
     *
     * @param from value before
     * @param to value after
     * @param reasonCode reason
     * @param remarks remarks
     * @return facts
     */
    public static EventFacts change(String from, String to, String reasonCode, String remarks) {
      return new EventFacts(null, null, from, to, reasonCode, remarks);
    }
  }
}
