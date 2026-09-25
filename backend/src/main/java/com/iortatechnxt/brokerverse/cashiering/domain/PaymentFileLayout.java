package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * The layout of a payment file handler's TXT files (CSHID.008): auto-detected, delimited with a
 * separator, or fixed width with {@code Header:start:length} fields separated by {@code ;}. The
 * bank layouts are parked (OQ03/OQ04); administrators set them here without a release.
 */
@Entity
@Table(name = "csh_payment_file_layout")
public class PaymentFileLayout {

  private static final Set<String> KINDS = Set.of("AUTO", "DELIMITED", "FIXED_WIDTH");

  @Id
  @Column(name = "handler_code", length = 40)
  private String handlerCode;

  @Column(nullable = false, length = 20)
  private String kind;

  @Column(length = 1)
  private String delimiter;

  @Column(length = 1000)
  private String fields;

  @Column(nullable = false, length = 250)
  private String description;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "updated_by", length = 50)
  private String updatedBy;

  protected PaymentFileLayout() {}

  /**
   * Changes the layout.
   *
   * @param newKind AUTO, DELIMITED or FIXED_WIDTH
   * @param newDelimiter separator of a delimited file
   * @param newFields fixed-width fields
   * @param by user
   * @param at time
   */
  public void change(String newKind, String newDelimiter, String newFields, String by, Instant at) {
    String k = newKind == null ? "" : newKind.strip().toUpperCase(Locale.ROOT);
    if (!KINDS.contains(k)) {
      throw new BusinessRuleException("PAYMENT_LAYOUT_KIND", "Unknown layout kind " + newKind);
    }
    requireShape(k, newDelimiter, newFields);
    this.kind = k;
    this.delimiter = newDelimiter;
    this.fields = newFields;
    this.updatedBy = by;
    this.updatedAt = at;
  }

  private static void requireShape(String k, String newDelimiter, String newFields) {
    boolean oneChar = newDelimiter != null && newDelimiter.length() == 1;
    if ("DELIMITED".equals(k) && !oneChar) {
      throw new BusinessRuleException(
          "PAYMENT_LAYOUT_DELIMITER", "A delimited layout needs a one-character separator");
    }
    if ("FIXED_WIDTH".equals(k) && (newFields == null || newFields.isBlank())) {
      throw new BusinessRuleException(
          "PAYMENT_LAYOUT_FIELDS", "A fixed-width layout needs its fields");
    }
  }

  public String getHandlerCode() {
    return handlerCode;
  }

  public String getKind() {
    return kind;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public String getFields() {
    return fields;
  }

  public String getDescription() {
    return description;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }
}
