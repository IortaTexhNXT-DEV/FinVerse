package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Type of cover within a product line (Annex II), e.g. Motor / Comprehensive. */
@Entity
@Table(name = "cat_cover_type")
public class CoverType extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "line_code", nullable = false, length = 30, updatable = false)
  private String lineCode;

  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "parent_code", length = 30, updatable = false)
  private String parentCode;

  protected CoverType() {}

  /**
   * Creates a subtype under a cover type of the same line (PMADD01: Type &gt; Subtype), pending
   * authorization.
   *
   * @param lineCode product line
   * @param code code, unique within the line
   * @param name name
   * @param sortOrder display order
   * @param parent cover type it refines (a top-level type of the same line), null for none
   */
  public CoverType(String lineCode, String code, String name, int sortOrder, CoverType parent) {
    this(lineCode, code, name, sortOrder);
    if (parent != null && parent.getParentCode() != null) {
      throw new BusinessRuleException(
          "COVER_TYPE_DEPTH", "A subtype cannot have subtypes (Type > Subtype only)");
    }
    this.parentCode = parent == null ? null : parent.getCode();
  }

  /**
   * Creates a cover type, pending authorization.
   *
   * @param lineCode product line
   * @param code code, unique within the line
   * @param name name
   * @param sortOrder display order
   */
  public CoverType(String lineCode, String code, String name, int sortOrder) {
    this.lineCode = lineCode;
    this.code = code;
    this.name = name;
    this.sortOrder = sortOrder;
  }

  /**
   * Renames or re-orders the cover type; it must be authorized again.
   *
   * @param newName name
   * @param newSortOrder display order
   */
  public void update(String newName, int newSortOrder) {
    this.name = newName;
    this.sortOrder = newSortOrder;
    markModified();
  }

  @Override
  public String catalogReference() {
    return lineCode + "/" + code;
  }

  @Override
  public String catalogDescription() {
    return name;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getParentCode() {
    return parentCode;
  }
}
