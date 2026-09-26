package com.iortatechnxt.brokerverse.brokerclaims.location.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The reference an insurer uses for an insured location of a cover (BRCLM.042;
 * CLAIMS_BROKING_DESIGN 5.2), valid from a date. A new reference end-dates the open one the day
 * before its own start, so the mapping history stays and is auditable; nothing is deleted
 * (FR-CL-023 R1/R2).
 */
@Entity
@Table(name = "bcl_location_ref")
public class LocationRef extends BaseEntity {

  private static final DateTimeFormatter DAY =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "account_id", updatable = false)
  private Long accountId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "account_item_no", nullable = false, updatable = false)
  private int accountItemNo;

  @Column(name = "location_key", length = 400, updatable = false)
  private String locationKey;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "insurer_location_ref", nullable = false, length = 60, updatable = false)
  private String insurerLocationRef;

  @Column(name = "effective_from", nullable = false, updatable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected LocationRef() {}

  /**
   * A new reference, open-ended.
   *
   * @param companyId company
   * @param location account, item and location key
   * @param insurerCode insurer
   * @param reference insurer's location reference
   * @param effectiveFrom first day of validity
   */
  public LocationRef(
      Long companyId,
      Location location,
      String insurerCode,
      String reference,
      LocalDate effectiveFrom) {
    if (reference == null || reference.isBlank()) {
      throw new BusinessRuleException(
          "BCL_LOCATION_REF_REQUIRED", "Enter the insurer location reference");
    }
    if (effectiveFrom == null) {
      throw new BusinessRuleException("BCL_EFFECTIVE_FROM_REQUIRED", "Enter the effective date");
    }
    this.companyId = companyId;
    this.accountId = location.accountId();
    this.arn = location.arn();
    this.accountItemNo = location.itemNo();
    this.locationKey = location.locationKey();
    this.insurerCode = insurerCode;
    this.insurerLocationRef = reference.strip();
    this.effectiveFrom = effectiveFrom;
  }

  /**
   * Ends the reference the day before a successor starts (R2).
   *
   * @param successorFrom start of the new reference, after this one's start
   */
  public void supersede(LocalDate successorFrom) {
    if (!successorFrom.isAfter(effectiveFrom)) {
      throw new BusinessRuleException(
          "BCL_EFFECTIVE_DATE", "The effective date must be after " + DAY.format(effectiveFrom));
    }
    this.effectiveTo = successorFrom.minusDays(1);
  }

  /**
   * Whether the reference is valid on a date.
   *
   * @param date date
   * @return true inside the effective dates
   */
  public boolean validOn(LocalDate date) {
    return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getArn() {
    return arn;
  }

  public int getAccountItemNo() {
    return accountItemNo;
  }

  public String getLocationKey() {
    return locationKey;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerLocationRef() {
    return insurerLocationRef;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  /**
   * An insured location of a cover.
   *
   * @param accountId account id
   * @param arn account reference number
   * @param itemNo item number
   * @param locationKey normalised location key
   */
  public record Location(Long accountId, String arn, int itemNo, String locationKey) {}
}
