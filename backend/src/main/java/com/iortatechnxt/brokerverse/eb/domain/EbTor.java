package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A version of the Terms of Reference of a cycle (BRID-009; FR-EB-035): structured items per
 * benefit line and plan, maintained while DRAFT and frozen when released. A change after release is
 * a new version; the earlier one is superseded. The standard template per benefit line is an open
 * question (EBQ08).
 */
@Entity
@Table(name = "eb_tor")
public class EbTor extends EbCycleRecord {

  /** Status of a TOR version. */
  public enum Status {
    /** Being prepared. */
    DRAFT,
    /** Released to the insurers. */
    RELEASED,
    /** Replaced by a later release. */
    SUPERSEDED
  }

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "released_by", length = 50)
  private String releasedBy;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @OneToMany(mappedBy = "tor", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder")
  private final List<EbTorItem> items = new ArrayList<>();

  protected EbTor() {}

  /**
   * Creates a draft version.
   *
   * @param cycle cycle
   * @param versionNo version, from 1
   */
  public EbTor(EbCycle cycle, int versionNo) {
    super(cycle);
    this.versionNo = versionNo;
  }

  /**
   * Replaces the items of the draft.
   *
   * @param data items in order
   */
  public void replaceItems(List<EbTorItem.Data> data) {
    requireDraft();
    items.clear();
    int order = 1;
    for (EbTorItem.Data d : data) {
      items.add(new EbTorItem(this, order++, d));
    }
  }

  /**
   * Releases the draft.
   *
   * @param at time
   * @param by user
   * @param attachment the stored TOR document
   */
  public void release(Instant at, String by, Long attachment) {
    requireDraft();
    if (items.isEmpty()) {
      throw new BusinessRuleException("EB_TOR_EMPTY", "Add the TOR items before release");
    }
    this.status = Status.RELEASED;
    this.releasedAt = at;
    this.releasedBy = by;
    this.attachmentId = attachment;
  }

  /** A later version was released. */
  public void supersede() {
    if (status == Status.RELEASED) {
      this.status = Status.SUPERSEDED;
    }
  }

  private void requireDraft() {
    if (status != Status.DRAFT) {
      throw new BusinessRuleException(
          "EB_TOR_RELEASED", "TOR version " + versionNo + " is released; start a new version");
    }
  }

  public int getVersionNo() {
    return versionNo;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public String getReleasedBy() {
    return releasedBy;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public List<EbTorItem> getItems() {
    return Collections.unmodifiableList(items);
  }
}
