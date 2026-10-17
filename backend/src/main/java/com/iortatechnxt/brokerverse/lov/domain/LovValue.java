package com.iortatechnxt.brokerverse.lov.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * One value of a list, valid from {@code effectiveFrom} to {@code effectiveTo} (inclusive, open
 * ended when null). Values are never deleted: they are end-dated or deactivated (BRNB.083).
 */
@Entity
@Table(name = "lov_value")
public class LovValue extends AuthorizableEntity {

  @Column(name = "type_code", nullable = false, length = 40, updatable = false)
  private String typeCode;

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 200)
  private String label;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "parent_code", length = 40)
  private String parentCode;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected LovValue() {}

  /**
   * Creates a value, pending authorization.
   *
   * @param typeCode list
   * @param code code, unique within the list
   * @param details label, order, parent and effectivity
   */
  public LovValue(String typeCode, String code, LovDetails details) {
    this.typeCode = typeCode;
    this.code = code;
    apply(details);
  }

  /**
   * Changes the value; the change must be authorized again.
   *
   * @param details new label, order, parent and effectivity
   */
  public void update(LovDetails details) {
    apply(details);
    markModified();
  }

  private void apply(LovDetails details) {
    if (details.effectiveTo() != null && details.effectiveTo().isBefore(details.effectiveFrom())) {
      throw new BusinessRuleException(
          "LOV_EFFECTIVITY_INVALID", "The effective-to date is before the effective-from date");
    }
    this.label = details.label();
    this.sortOrder = details.sortOrder();
    this.parentCode = details.parentCode();
    this.effectiveFrom = details.effectiveFrom();
    this.effectiveTo = details.effectiveTo();
  }

  /**
   * Whether the value may be used on a date: authorized, active and within its effectivity.
   *
   * @param date business date
   * @return true when usable
   */
  public boolean isUsableOn(LocalDate date) {
    return isActive()
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  public String getTypeCode() {
    return typeCode;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getParentCode() {
    return parentCode;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }
}
