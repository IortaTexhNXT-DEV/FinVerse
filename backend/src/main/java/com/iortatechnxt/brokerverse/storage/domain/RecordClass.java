package com.iortatechnxt.brokerverse.storage.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.storage.BucketClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * A record class of stored files (DOCUMENT_STORAGE_DECISION, decisions 3 and 5): its bucket, its
 * retention (the retention rules of {@link #getRetentionRecordType()}, else {@link
 * #getRetentionPeriod()}), whether its files are under legal hold from the start and whether its
 * final records are archived to ECM.
 */
@Entity
@Table(name = "sto_record_class")
public class RecordClass extends BaseEntity {

  private static final Pattern PERIOD = Pattern.compile("P[0-9]+[YMD]");

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "bucket_class", nullable = false, length = 20, updatable = false)
  private BucketClass bucketClass;

  @Column(name = "retention_record_type", length = 40)
  private String retentionRecordType;

  @Column(name = "retention_period", nullable = false, length = 10)
  private String retentionPeriod;

  @Column(name = "legal_hold", nullable = false)
  private boolean legalHold;

  @Column(name = "archive_to_ecm", nullable = false)
  private boolean archiveToEcm;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, length = 300)
  private String description;

  protected RecordClass() {}

  /**
   * Changes the maintainable settings.
   *
   * @param settings new settings
   */
  public void change(RecordClassSettings settings) {
    String period = settings.retentionPeriod() == null ? "" : settings.retentionPeriod().strip();
    if (!PERIOD.matcher(period).matches()) {
      throw new BusinessRuleException(
          "RECORD_CLASS_PERIOD", "Enter the retention as years, months or days, e.g. P20Y");
    }
    this.retentionPeriod = period;
    this.retentionRecordType = blankToNull(settings.retentionRecordType());
    this.legalHold = settings.legalHold();
    this.archiveToEcm = settings.archiveToEcm();
    this.active = settings.active();
  }

  /**
   * The fallback retention as a period.
   *
   * @return period
   */
  public Period fallbackRetention() {
    try {
      return Period.parse(retentionPeriod);
    } catch (DateTimeParseException e) {
      throw new IllegalStateException("Invalid retention period of " + code, e);
    }
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public BucketClass getBucketClass() {
    return bucketClass;
  }

  public String getRetentionRecordType() {
    return retentionRecordType;
  }

  public String getRetentionPeriod() {
    return retentionPeriod;
  }

  public boolean isLegalHold() {
    return legalHold;
  }

  public boolean isArchiveToEcm() {
    return archiveToEcm;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }
}
