package com.iortatechnxt.brokerverse.brokerclaims.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An attribute of a Claims list value (BRCLM.010/014; CLAIMS_BROKING_DESIGN 5.3): the phase,
 * waiting party, follow-up days and "awaiting premium remittance" flag of a claim status, and the
 * outcome, closure and amount rules of a settlement type. The names are the {@code ATTR_*}
 * constants of {@link ClaimCodes}; values are text ({@code true} / {@code false} for flags).
 *
 * <p>Seeded by V1020. Read by every wave; maintained on Claims Setup (wave CL1-B, which adds the
 * maintenance methods).
 */
@Entity
@Table(name = "bcl_lov_attribute")
public class ClaimLovAttribute extends BaseEntity {

  @Column(name = "type_code", nullable = false, length = 40, updatable = false)
  private String typeCode;

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 40, updatable = false)
  private String attribute;

  @Column(nullable = false, length = 200)
  private String value;

  protected ClaimLovAttribute() {}

  /**
   * Creates an attribute.
   *
   * @param typeCode list ({@code BCL_CLAIM_STATUS} or {@code BCL_SETTLEMENT_TYPE})
   * @param code value code
   * @param attribute attribute name
   * @param value attribute value
   */
  public ClaimLovAttribute(String typeCode, String code, String attribute, String value) {
    this.typeCode = typeCode;
    this.code = code;
    this.attribute = attribute;
    this.value = value;
  }

  /**
   * Changes the value.
   *
   * @param newValue value
   */
  public void changeValue(String newValue) {
    this.value = newValue;
  }

  /**
   * Whether the attribute holds the flag value {@code true}.
   *
   * @return true when the value is "true" (any case)
   */
  public boolean isTrue() {
    return Boolean.parseBoolean(value);
  }

  public String getTypeCode() {
    return typeCode;
  }

  public String getCode() {
    return code;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getValue() {
    return value;
  }
}
