package com.iortatechnxt.brokerverse.collections.common.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.TaggingOwner;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Balance;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Classification;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Figures;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Parties;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection item: one ledger invoice that has ever been listed in the worklist
 * (COLLECTIONS_DESIGN 4.1; BRCLXN.001, 022). The ledger facts are refreshed by {@code
 * CLX_DAILY_REFRESH} and the balance listener; the work pointers (handler, current disposition,
 * category, owner, last effort, remarks) change with the collector's work; the flags (promise,
 * escalation, installment) are set by the plans and escalation wave. Items are never deleted.
 */
@Entity
@Table(name = "clx_item")
public class CollectionItem extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Embedded private Parties parties;

  @Embedded private Classification classification;

  @Embedded private Figures figures;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemStatus status;

  @Column(name = "listed_on", nullable = false)
  private LocalDate listedOn;

  @Column(name = "completed_on")
  private LocalDate completedOn;

  @Column(name = "last_refreshed_at", nullable = false)
  private Instant lastRefreshedAt;

  @Column(name = "current_handler", length = 50)
  private String currentHandler;

  @Column(name = "current_disposition_id")
  private Long currentDispositionId;

  @Column(name = "disposition_code", length = 40)
  private String dispositionCode;

  @Column(length = 1)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "tagging_owner", length = 12)
  private TaggingOwner taggingOwner;

  @Column(name = "last_effort_at")
  private Instant lastEffortAt;

  @Column(name = "last_effort_code", length = 40)
  private String lastEffortCode;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "promise_status", length = 20)
  private String promiseStatus;

  @Column(name = "escalation_level", length = 20)
  private String escalationLevel;

  @Column(name = "installment_overdue", nullable = false)
  private boolean installmentOverdue;

  @Column(name = "editing_by", length = 50)
  private String editingBy;

  @Column(name = "editing_since")
  private Instant editingSince;

  @ElementCollection
  @CollectionTable(name = "clx_item_balance", joinColumns = @JoinColumn(name = "item_id"))
  private final List<Balance> balances = new ArrayList<>();

  protected CollectionItem() {}

  /**
   * Lists an invoice (BRCLXN.001, 014).
   *
   * @param companyId company
   * @param invoiceNo invoice number
   * @param snapshot ledger facts
   * @param status initial status
   * @param at listing time
   * @param today business date
   */
  public CollectionItem(
      Long companyId,
      String invoiceNo,
      Snapshot snapshot,
      ItemStatus status,
      Instant at,
      LocalDate today) {
    this.companyId = companyId;
    this.invoiceNo = invoiceNo;
    this.listedOn = today;
    this.status = status;
    apply(snapshot, at);
    if (status == ItemStatus.COMPLETED) {
      completedOn = today;
    }
  }

  /**
   * Refreshes the ledger facts and the status (BRCLXN.008, 015, 022): an item that falls to zero or
   * below the threshold is completed and keeps its history; an item above it again is reopened.
   *
   * @param snapshot ledger facts
   * @param newStatus status from the refresh rules
   * @param at refresh time
   * @param today business date
   * @return the previous status
   */
  public ItemStatus refresh(Snapshot snapshot, ItemStatus newStatus, Instant at, LocalDate today) {
    ItemStatus previous = status;
    apply(snapshot, at);
    if (previous != newStatus) {
      status = newStatus;
      completedOn = newStatus == ItemStatus.COMPLETED ? today : null;
    }
    return previous;
  }

  private void apply(Snapshot s, Instant at) {
    this.parties = s.parties();
    this.classification = s.classification();
    this.figures = s.figures();
    this.balances.clear();
    this.balances.addAll(s.balances());
    this.lastRefreshedAt = at;
  }

  /**
   * Reopens a completed item (e.g. a direct payment account returned by the insurer, CMRID.009).
   *
   * @return true when the status changed
   */
  public boolean reopen() {
    if (status == ItemStatus.OPEN) {
      return false;
    }
    status = ItemStatus.OPEN;
    completedOn = null;
    return true;
  }

  /**
   * Gives the item to a handler (BRCLXN.052).
   *
   * @param handler handler user name, null to unassign
   * @return the previous handler
   */
  public String assignTo(String handler) {
    String previous = currentHandler;
    currentHandler = handler;
    return previous;
  }

  /**
   * Makes a disposition the current one (BRCLXN.016-021).
   *
   * @param dispositionId disposition
   * @param code disposition code
   * @param newCategory tagging category A / B / C, null to keep the current one
   * @param owner tagging owner, null to keep the current one
   */
  public void disposed(Long dispositionId, String code, String newCategory, TaggingOwner owner) {
    this.currentDispositionId = dispositionId;
    this.dispositionCode = code;
    if (newCategory != null) {
      this.category = newCategory;
    }
    if (owner != null) {
      this.taggingOwner = owner;
    }
  }

  /**
   * Records the latest collection effort (report fields "last collection effort", p.59).
   *
   * @param at when the effort was made
   * @param code effort code
   */
  public void effortMade(Instant at, String code) {
    if (lastEffortAt == null || !at.isBefore(lastEffortAt)) {
      this.lastEffortAt = at;
      this.lastEffortCode = code;
    }
  }

  /**
   * Changes the collector's remarks and tagging category (p.41).
   *
   * @param newRemarks remarks, null to clear
   * @param newCategory category A / B / C, null to clear
   */
  public void updateDetails(String newRemarks, String newCategory) {
    this.remarks = newRemarks;
    this.category = newCategory;
  }

  /**
   * Sets the promise flag (plans and escalation wave, BRCLXN.055).
   *
   * @param value promise status, null to clear
   */
  public void flagPromise(String value) {
    this.promiseStatus = value;
  }

  /**
   * Sets the escalation flag (plans and escalation wave, BRCLXN.049/050).
   *
   * @param level escalation level, null when not escalated
   */
  public void flagEscalation(String level) {
    this.escalationLevel = level;
  }

  /**
   * Sets the overdue-installment flag (plans and escalation wave, BRCLXN.053).
   *
   * @param overdue whether an installment is overdue
   */
  public void flagInstallmentOverdue(boolean overdue) {
    this.installmentOverdue = overdue;
  }

  /**
   * Whether another user holds the edit lock that has not expired (NFR record lock).
   *
   * @param username user who wants to edit
   * @param now current time
   * @param lockTime lifetime of a lock
   * @return true when another user is editing
   */
  public boolean lockedByOther(String username, Instant now, Duration lockTime) {
    return editingBy != null
        && !sameUser(editingBy, username)
        && editingSince != null
        && editingSince.plus(lockTime).isAfter(now);
  }

  /**
   * Takes (or refreshes) the edit lock.
   *
   * @param username user
   * @param now time
   */
  public void lock(String username, Instant now) {
    this.editingBy = username;
    this.editingSince = now;
  }

  /**
   * Releases the edit lock of a user.
   *
   * @param username user
   * @return true when the user held it
   */
  public boolean unlock(String username) {
    if (editingBy == null || !sameUser(editingBy, username)) {
      return false;
    }
    editingBy = null;
    editingSince = null;
    return true;
  }

  private static boolean sameUser(String a, String b) {
    return b != null && String.CASE_INSENSITIVE_ORDER.compare(a, b) == 0;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public Parties getParties() {
    return parties;
  }

  public Classification getClassification() {
    return classification;
  }

  public Figures getFigures() {
    return figures;
  }

  public ItemStatus getStatus() {
    return status;
  }

  public LocalDate getListedOn() {
    return listedOn;
  }

  public LocalDate getCompletedOn() {
    return completedOn;
  }

  public Instant getLastRefreshedAt() {
    return lastRefreshedAt;
  }

  public String getCurrentHandler() {
    return currentHandler;
  }

  public Long getCurrentDispositionId() {
    return currentDispositionId;
  }

  public String getDispositionCode() {
    return dispositionCode;
  }

  public String getCategory() {
    return category;
  }

  public TaggingOwner getTaggingOwner() {
    return taggingOwner;
  }

  public Instant getLastEffortAt() {
    return lastEffortAt;
  }

  public String getLastEffortCode() {
    return lastEffortCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getPromiseStatus() {
    return promiseStatus;
  }

  public String getEscalationLevel() {
    return escalationLevel;
  }

  public boolean isInstallmentOverdue() {
    return installmentOverdue;
  }

  public String getEditingBy() {
    return editingBy;
  }

  public Instant getEditingSince() {
    return editingSince;
  }

  public List<Balance> getBalances() {
    return Collections.unmodifiableList(balances);
  }

  /**
   * The ledger facts of one refresh.
   *
   * @param parties keys and parties
   * @param classification classification
   * @param figures figures
   * @param balances balance per component
   */
  public record Snapshot(
      Parties parties, Classification classification, Figures figures, List<Balance> balances) {

    /** Defensive copy. */
    public Snapshot {
      balances = List.copyOf(balances);
    }
  }
}
