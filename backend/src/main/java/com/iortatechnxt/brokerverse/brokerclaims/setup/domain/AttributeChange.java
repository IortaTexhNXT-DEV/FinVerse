package com.iortatechnxt.brokerverse.brokerclaims.setup.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A proposed value of an attribute of a status or settlement type (BRCLM.010/014, FR-CM-040/043):
 * it waits for another BCL_SETUP user to authorize it, then applies to {@code bcl_lov_attribute}. A
 * null value removes the attribute (e.g. blank follow-up days = parameter). A newer proposal for
 * the same value supersedes a pending one (deactivated).
 */
@Entity
@Table(name = "bcl_attribute_change")
public class AttributeChange extends AuthorizableEntity {

  @Column(name = "type_code", nullable = false, length = 40, updatable = false)
  private String typeCode;

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 40, updatable = false)
  private String attribute;

  @Column(name = "new_value", length = 200, updatable = false)
  private String newValue;

  protected AttributeChange() {}

  /**
   * Proposes a value.
   *
   * @param typeCode list ({@code BCL_CLAIM_STATUS} or {@code BCL_SETTLEMENT_TYPE})
   * @param code value code
   * @param attribute attribute name
   * @param newValue proposed value, null removes the attribute
   */
  public AttributeChange(String typeCode, String code, String attribute, String newValue) {
    this.typeCode = typeCode;
    this.code = code;
    this.attribute = attribute;
    this.newValue = newValue;
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

  public String getNewValue() {
    return newValue;
  }
}
