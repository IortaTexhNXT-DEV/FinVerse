package com.iortatechnxt.brokerverse.collections.worklist.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.AssignmentKind;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One assignment of a collection item to a handler (BRCLXN.052): by rule, permanent, temporary with
 * an end date, or the automatic return at the end of a temporary assignment. The history of an
 * item's assignments stays visible on the account page.
 */
@Entity
@Table(name = "clx_assignment")
public class Assignment extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "item_id", nullable = false, updatable = false)
  private Long itemId;

  @Column(name = "handler_username", nullable = false, length = 50, updatable = false)
  private String handlerUsername;

  @Column(name = "previous_handler", length = 50, updatable = false)
  private String previousHandler;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12, updatable = false)
  private AssignmentKind kind;

  @Column(name = "valid_from", nullable = false, updatable = false)
  private LocalDate validFrom;

  @Column(name = "valid_to", updatable = false)
  private LocalDate validTo;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;

  @Column(name = "assigned_by", nullable = false, length = 50, updatable = false)
  private String assignedBy;

  @Column(name = "reverted_at")
  private Instant revertedAt;

  @Column(name = "bulk_ref", length = 40, updatable = false)
  private String bulkRef;

  protected Assignment() {}

  /**
   * Records an assignment.
   *
   * @param companyId company
   * @param itemId item
   * @param handlers new and previous handler
   * @param terms kind, period and reason
   * @param assignedBy user (SYSTEM for rules and reverts)
   * @param bulkRef bulk reference, null for one item
   */
  public Assignment(
      Long companyId,
      Long itemId,
      Handlers handlers,
      Terms terms,
      String assignedBy,
      String bulkRef) {
    this.companyId = companyId;
    this.itemId = itemId;
    this.handlerUsername = handlers.handler();
    this.previousHandler = handlers.previous();
    this.kind = terms.kind();
    this.validFrom = terms.validFrom();
    this.validTo = terms.validTo();
    this.reason = terms.reason();
    this.assignedBy = assignedBy;
    this.bulkRef = bulkRef;
  }

  /**
   * Marks a temporary assignment as ended (returned to the previous handler or replaced).
   *
   * @param at time
   */
  public void ended(Instant at) {
    if (revertedAt == null) {
      revertedAt = at;
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getItemId() {
    return itemId;
  }

  public String getHandlerUsername() {
    return handlerUsername;
  }

  public String getPreviousHandler() {
    return previousHandler;
  }

  public AssignmentKind getKind() {
    return kind;
  }

  public LocalDate getValidFrom() {
    return validFrom;
  }

  public LocalDate getValidTo() {
    return validTo;
  }

  public String getReason() {
    return reason;
  }

  public String getAssignedBy() {
    return assignedBy;
  }

  public Instant getRevertedAt() {
    return revertedAt;
  }

  public String getBulkRef() {
    return bulkRef;
  }

  /**
   * New and previous handler.
   *
   * @param handler new handler
   * @param previous previous handler, null when unassigned
   */
  public record Handlers(String handler, String previous) {}

  /**
   * Kind, period and reason of an assignment.
   *
   * @param kind kind
   * @param validFrom first day
   * @param validTo last day of a temporary assignment
   * @param reason reason
   */
  public record Terms(AssignmentKind kind, LocalDate validFrom, LocalDate validTo, String reason) {}
}
