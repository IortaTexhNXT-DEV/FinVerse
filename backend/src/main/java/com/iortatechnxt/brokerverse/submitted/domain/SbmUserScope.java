package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * The data scope of a user (BRIDSP-28): the segments the user sees and, for account officers, only
 * the records they handle or own.
 */
@Entity
@Table(name = "sbm_user_scope")
public class SbmUserScope extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, updatable = false, length = 50)
  private String username;

  @Column(length = 200)
  private String segments;

  @Column(name = "own_records_only", nullable = false)
  private boolean ownRecordsOnly;

  protected SbmUserScope() {}

  /**
   * A scope.
   *
   * @param companyId company
   * @param username user
   * @param segments segments, empty for all
   * @param ownRecordsOnly own records only
   */
  public SbmUserScope(
      Long companyId, String username, List<String> segments, boolean ownRecordsOnly) {
    this.companyId = companyId;
    this.username = username;
    maintain(segments, ownRecordsOnly);
  }

  /**
   * Changes the scope.
   *
   * @param newSegments segments, empty for all
   * @param own own records only
   */
  public final void maintain(List<String> newSegments, boolean own) {
    this.segments =
        newSegments == null || newSegments.isEmpty() ? null : String.join(",", newSegments);
    this.ownRecordsOnly = own;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getUsername() {
    return username;
  }

  /**
   * The segments of the scope.
   *
   * @return segments, empty for all
   */
  public List<String> segmentList() {
    return segments == null || segments.isBlank()
        ? List.of()
        : Arrays.stream(segments.split(",")).map(String::strip).toList();
  }

  public boolean isOwnRecordsOnly() {
    return ownRecordsOnly;
  }
}
