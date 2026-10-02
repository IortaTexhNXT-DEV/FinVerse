package com.iortatechnxt.brokerverse.collections.disposition.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.DispositionSource;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OpsAction;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.TaggingOwner;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A PR collector disposition of a collection item (BRCLXN.016-023): code of LOV {@code
 * CLX_PR_DISPOSITION} with the category, tagging owner and Operations action of its attributes,
 * remarks and the hand-off details (pick-up date and address, certificate number ...). Append-only:
 * a new disposition supersedes the current one, which stays in the history.
 */
@Entity
@Table(name = "clx_disposition")
public class PrDisposition extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "item_id", nullable = false, updatable = false)
  private Long itemId;

  @Column(name = "disposition_code", nullable = false, length = 40, updatable = false)
  private String dispositionCode;

  @Column(length = 1, updatable = false)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "tagging_owner", length = 12, updatable = false)
  private TaggingOwner taggingOwner;

  @Enumerated(EnumType.STRING)
  @Column(name = "ops_action", nullable = false, length = 20, updatable = false)
  private OpsAction opsAction;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "effective_on", nullable = false, updatable = false)
  private LocalDate effectiveOn;

  @Column(length = 4000, updatable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12, updatable = false)
  private DispositionSource source;

  @Column(name = "outbox_id")
  private Long outboxId;

  @Column(name = "superseded_by")
  private Long supersededBy;

  @Column(name = "bulk_ref", length = 40, updatable = false)
  private String bulkRef;

  protected PrDisposition() {}

  /**
   * Records a disposition.
   *
   * @param companyId company
   * @param itemId item
   * @param tag code, category, owner and Operations action
   * @param details remarks, effective date, payload, source and bulk reference
   */
  public PrDisposition(Long companyId, Long itemId, Tag tag, Details details) {
    this.companyId = companyId;
    this.itemId = itemId;
    this.dispositionCode = tag.code();
    this.category = tag.category();
    this.taggingOwner = tag.owner();
    this.opsAction = tag.opsAction();
    this.remarks = details.remarks();
    this.effectiveOn = details.effectiveOn();
    this.payload = details.payload();
    this.source = details.source();
    this.bulkRef = details.bulkRef();
  }

  /**
   * Links the outbox row created for the hand-off.
   *
   * @param id outbox row
   */
  public void queued(Long id) {
    this.outboxId = id;
  }

  /**
   * Marks the disposition as replaced by a later one.
   *
   * @param dispositionId the later disposition
   */
  public void supersede(Long dispositionId) {
    this.supersededBy = dispositionId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getItemId() {
    return itemId;
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

  public OpsAction getOpsAction() {
    return opsAction;
  }

  public String getRemarks() {
    return remarks;
  }

  public LocalDate getEffectiveOn() {
    return effectiveOn;
  }

  public String getPayload() {
    return payload;
  }

  public DispositionSource getSource() {
    return source;
  }

  public Long getOutboxId() {
    return outboxId;
  }

  public Long getSupersededBy() {
    return supersededBy;
  }

  public String getBulkRef() {
    return bulkRef;
  }

  /**
   * What the disposition says.
   *
   * @param code LOV code
   * @param category tagging category, may be null
   * @param owner tagging owner, may be null
   * @param opsAction Operations hand-off
   */
  public record Tag(String code, String category, TaggingOwner owner, OpsAction opsAction) {}

  /**
   * Remarks and context of a disposition.
   *
   * @param remarks remarks
   * @param effectiveOn date the disposition applies from
   * @param payload hand-off details as JSON, may be null
   * @param source who recorded it
   * @param bulkRef bulk reference, may be null
   */
  public record Details(
      String remarks,
      LocalDate effectiveOn,
      String payload,
      DispositionSource source,
      String bulkRef) {}
}
