package com.iortatechnxt.brokerverse.migration.mapping.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A code map set (BRID 3.1; DATA_MIGRATION_DESIGN section 7): one per coded domain, for example
 * {@code INSURER}, {@code PRODUCT}, {@code PACKAGE}, {@code LOV:MARKET_SEGMENT}, {@code GL_ACCOUNT}
 * or {@code STATUS:UPP}, with its target domain, the kind of target the approval checks, a default
 * target for DEFAULT entries, and the business owner and data steward.
 */
@Entity
@Table(name = "mig_code_map_set")
public class CodeMapSet extends BaseEntity {

  @Column(nullable = false, length = 60, updatable = false)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "target_domain", nullable = false, length = 120)
  private String targetDomain;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_kind", nullable = false, length = 20)
  private TargetKind targetKind = TargetKind.FREE;

  @Column(name = "target_ref", length = 60)
  private String targetRef;

  @Column(name = "default_target", length = 60)
  private String defaultTarget;

  @Column(name = "business_owner", length = 50)
  private String businessOwner;

  @Column(name = "data_steward", length = 50)
  private String dataSteward;

  @Column(name = "owner_title", length = 200)
  private String ownerTitle;

  @Column(name = "steward_title", length = 200)
  private String stewardTitle;

  @Column(name = "used_by", length = 200)
  private String usedBy;

  protected CodeMapSet() {}

  /**
   * A new set.
   *
   * @param code set code
   * @param name name
   * @param targetDomain target domain
   * @param targetKind kind of target checked at approval
   * @param targetRef LOV type of a LOV set, or other qualifier of the target
   */
  public CodeMapSet(
      String code, String name, String targetDomain, TargetKind targetKind, String targetRef) {
    this.code = code;
    this.name = name;
    this.targetDomain = targetDomain;
    this.targetKind = targetKind;
    this.targetRef = targetRef;
  }

  /**
   * Sets the owners.
   *
   * @param owner business owner user
   * @param steward data steward user
   */
  public void assign(String owner, String steward) {
    this.businessOwner = owner;
    this.dataSteward = steward;
  }

  public void setDefaultTarget(String defaultTarget) {
    this.defaultTarget = defaultTarget;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getTargetDomain() {
    return targetDomain;
  }

  public TargetKind getTargetKind() {
    return targetKind;
  }

  public String getTargetRef() {
    return targetRef;
  }

  public String getDefaultTarget() {
    return defaultTarget;
  }

  public String getBusinessOwner() {
    return businessOwner;
  }

  public String getDataSteward() {
    return dataSteward;
  }

  public String getOwnerTitle() {
    return ownerTitle;
  }

  public String getStewardTitle() {
    return stewardTitle;
  }

  public String getUsedBy() {
    return usedBy;
  }
}
