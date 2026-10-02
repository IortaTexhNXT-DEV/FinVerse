package com.iortatechnxt.brokerverse.crm.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** A controlled tag on a client (list of values CLIENT_TAG), e.g. VIP or DO_NOT_CALL (BRNB.091). */
@Entity
@Table(name = "crm_client_tag")
public class ClientTag extends BaseEntity {

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "tag_code", nullable = false, length = 40, updatable = false)
  private String tagCode;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "removed_by", length = 50)
  private String removedBy;

  @Column(name = "removed_at")
  private Instant removedAt;

  protected ClientTag() {}

  /**
   * Tags a client.
   *
   * @param clientId client
   * @param tagCode tag code
   */
  public ClientTag(Long clientId, String tagCode) {
    this.clientId = clientId;
    this.tagCode = tagCode;
  }

  /**
   * Removes the tag (kept for history).
   *
   * @param user user
   * @param when time
   */
  public void remove(String user, Instant when) {
    this.active = false;
    this.removedBy = user;
    this.removedAt = when;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getTagCode() {
    return tagCode;
  }

  public boolean isActive() {
    return active;
  }

  public String getRemovedBy() {
    return removedBy;
  }

  public Instant getRemovedAt() {
    return removedAt;
  }
}
