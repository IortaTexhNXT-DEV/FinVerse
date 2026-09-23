package com.iortatechnxt.finverse.common.domain;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;

/**
 * Master record subject to maker-checker (four eyes) control.
 *
 * <p>Any creation or modification places the record in {@link RecordStatus#PENDING_AUTHORIZATION};
 * a different user must authorize it before it can be used in transactions.
 */
@MappedSuperclass
public abstract class AuthorizableEntity extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "record_status", nullable = false, length = 30)
  private RecordStatus recordStatus = RecordStatus.PENDING_AUTHORIZATION;

  @Column(name = "authorized_by", length = 50)
  private String authorizedBy;

  @Column(name = "authorized_at")
  private Instant authorizedAt;

  /**
   * Authorizes the record (checker action).
   *
   * @param checker user performing the authorization
   * @param when authorization timestamp
   */
  public void authorize(String checker, Instant when) {
    if (recordStatus != RecordStatus.PENDING_AUTHORIZATION) {
      throw new BusinessRuleException("RECORD_NOT_PENDING", "Record is not pending authorization");
    }
    String maker = getUpdatedBy() != null ? getUpdatedBy() : getCreatedBy();
    if (Objects.equals(maker, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    this.recordStatus = RecordStatus.ACTIVE;
    this.authorizedBy = checker;
    this.authorizedAt = when;
  }

  /** Marks the record as modified; it must be re-authorized before use. */
  public void markModified() {
    this.recordStatus = RecordStatus.PENDING_AUTHORIZATION;
    this.authorizedBy = null;
    this.authorizedAt = null;
  }

  /** Deactivates the record (no physical deletion is ever performed). */
  public void deactivate() {
    this.recordStatus = RecordStatus.INACTIVE;
  }

  /**
   * Indicates whether the record may be used in transactions.
   *
   * @return true when the record is authorized and active
   */
  public boolean isActive() {
    return recordStatus == RecordStatus.ACTIVE;
  }

  public RecordStatus getRecordStatus() {
    return recordStatus;
  }

  public String getAuthorizedBy() {
    return authorizedBy;
  }

  public Instant getAuthorizedAt() {
    return authorizedAt;
  }
}
