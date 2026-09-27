package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A letter rule (BRIDSP-22; SP SQ09): the letter of a type sent to the records of a segment, bucket
 * and status on the day that is the given number of days from their expiry (negative before), by a
 * channel, from a template. Only letters that are not renewal letters. Maker and checker.
 */
@Entity
@Table(name = "sbm_letter_rule")
public class SbmLetterRule extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "letter_type", nullable = false, length = 30)
  private String letterType;

  @Column(length = 30)
  private String segment;

  @Column(length = 40)
  private String bucket;

  @Column(length = 30)
  private String status;

  @Column(name = "days_from_expiry", nullable = false)
  private int daysFromExpiry;

  @Column(nullable = false, length = 20)
  private String channel;

  @Column(name = "template_code", nullable = false, length = 40)
  private String templateCode;

  @Column(length = 250)
  private String description;

  protected SbmLetterRule() {}

  /**
   * A rule.
   *
   * @param companyId company
   * @param row content
   */
  public SbmLetterRule(Long companyId, Row row) {
    this.companyId = companyId;
    set(row);
  }

  /**
   * Changes the rule (pending approval again).
   *
   * @param row content
   */
  public final void change(Row row) {
    set(row);
    markModified();
  }

  private void set(Row row) {
    this.letterType = row.letterType();
    this.segment = row.segment();
    this.bucket = row.bucket();
    this.status = row.status();
    this.daysFromExpiry = row.daysFromExpiry();
    this.channel = row.channel();
    this.templateCode = row.templateCode();
    this.description = row.description();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getLetterType() {
    return letterType;
  }

  public String getSegment() {
    return segment;
  }

  public String getBucket() {
    return bucket;
  }

  public String getStatus() {
    return status;
  }

  public int getDaysFromExpiry() {
    return daysFromExpiry;
  }

  public String getChannel() {
    return channel;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public String getDescription() {
    return description;
  }

  /**
   * The content of a rule.
   *
   * @param letterType letter type (LOV SBM_LETTER_TYPE)
   * @param segment segment, null for all
   * @param bucket bucket, null for all
   * @param status record status, null for all
   * @param daysFromExpiry days from the expiry (negative before)
   * @param channel EMAIL, PRINT or BANK_COUNTERPART
   * @param templateCode template
   * @param description description
   */
  public record Row(
      String letterType,
      String segment,
      String bucket,
      String status,
      int daysFromExpiry,
      String channel,
      String templateCode,
      String description) {}
}
