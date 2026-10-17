package com.iortatechnxt.brokerverse.collections.disposition.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A collection effort on an item (p.43-46, p.59; BRCLXN.021): code of LOV {@code CLX_EFFORT_CODE},
 * when, channel, contact person and remarks. Efforts are a log: never changed or deleted.
 */
@Entity
@Table(name = "clx_effort")
public class Effort extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "item_id", nullable = false, updatable = false)
  private Long itemId;

  @Column(name = "effort_code", nullable = false, length = 40, updatable = false)
  private String effortCode;

  @Column(name = "effort_at", nullable = false, updatable = false)
  private Instant effortAt;

  @Column(length = 30, updatable = false)
  private String channel;

  @Column(name = "contact_person", length = 120, updatable = false)
  private String contactPerson;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(name = "bulk_ref", length = 40, updatable = false)
  private String bulkRef;

  protected Effort() {}

  /**
   * Logs an effort.
   *
   * @param companyId company
   * @param itemId item
   * @param facts code, time, channel, contact and remarks
   * @param bulkRef bulk reference, null for one item
   */
  public Effort(Long companyId, Long itemId, Facts facts, String bulkRef) {
    this.companyId = companyId;
    this.itemId = itemId;
    this.effortCode = facts.code();
    this.effortAt = facts.at();
    this.channel = facts.channel();
    this.contactPerson = facts.contactPerson();
    this.remarks = facts.remarks();
    this.bulkRef = bulkRef;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getItemId() {
    return itemId;
  }

  public String getEffortCode() {
    return effortCode;
  }

  public Instant getEffortAt() {
    return effortAt;
  }

  public String getChannel() {
    return channel;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getBulkRef() {
    return bulkRef;
  }

  /**
   * What was done.
   *
   * @param code effort code
   * @param at when
   * @param channel channel (phone, e-mail, visit ...)
   * @param contactPerson person contacted
   * @param remarks remarks
   */
  public record Facts(
      String code, Instant at, String channel, String contactPerson, String remarks) {}
}
