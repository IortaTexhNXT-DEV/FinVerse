package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An item of the Terms of Reference: benefit line, plan, description and requirement. */
@Entity
@Table(name = "eb_tor_item")
public class EbTorItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tor_id", nullable = false, updatable = false)
  private EbTor tor;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "benefit_line", nullable = false, length = 30)
  private String benefitLine;

  @Column(name = "plan_code", length = 30)
  private String planCode;

  @Column(nullable = false, length = 300)
  private String description;

  @Column(nullable = false, length = 1000)
  private String requirement;

  protected EbTorItem() {}

  EbTorItem(EbTor tor, int sortOrder, Data data) {
    this.tor = tor;
    this.sortOrder = sortOrder;
    this.benefitLine = data.benefitLine();
    this.planCode = data.planCode();
    this.description = data.description();
    this.requirement = data.requirement();
  }

  public EbTor getTor() {
    return tor;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getBenefitLine() {
    return benefitLine;
  }

  public String getPlanCode() {
    return planCode;
  }

  public String getDescription() {
    return description;
  }

  public String getRequirement() {
    return requirement;
  }

  /**
   * Data of an item.
   *
   * @param benefitLine benefit line
   * @param planCode plan, may be null (every plan)
   * @param description item, e.g. "Annual benefit limit"
   * @param requirement what the client requires
   */
  public record Data(String benefitLine, String planCode, String description, String requirement) {}
}
