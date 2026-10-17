package com.iortatechnxt.brokerverse.collections.common.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An attribute of a Collections LOV value (table {@code clx_lov_attribute}, V1000; BRCLXN.016, 037,
 * 047/048): {@code category}, {@code tagging_owner}, {@code ops_action}, {@code allowed_roles} of
 * the PR collector dispositions and {@code requires_invoice}, {@code cashiering_action} of the
 * unapplied-payment dispositions. {@code lov_value} has no attribute columns, so Collections keeps
 * them by type, code and attribute.
 */
@Entity
@Table(name = "clx_lov_attribute")
public class LovAttribute extends BaseEntity {

  @Column(name = "type_code", nullable = false, length = 40, updatable = false)
  private String typeCode;

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 40, updatable = false)
  private String attribute;

  @Column(nullable = false, length = 200)
  private String value;

  protected LovAttribute() {}

  /**
   * Creates an attribute.
   *
   * @param typeCode LOV type
   * @param code LOV value code
   * @param attribute attribute name
   * @param value value
   */
  public LovAttribute(String typeCode, String code, String attribute, String value) {
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
  public void change(String newValue) {
    this.value = newValue;
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
