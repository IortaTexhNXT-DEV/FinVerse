package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Branch of a panel insurer with its Local Government Tax rate (Appendix A: "LGT value is dependent
 * on the Insurer Branch") and optional placement mailbox.
 */
@Entity
@Table(name = "cat_insurer_branch")
public class InsurerBranch extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "insurer_id", nullable = false, updatable = false)
  private Long insurerId;

  @Column(nullable = false, length = 20, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 80)
  private String city;

  @Column(name = "lgt_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal lgtRate;

  @Column(name = "placement_email", length = 120)
  private String placementEmail;

  protected InsurerBranch() {}

  /**
   * Creates a branch, pending authorization.
   *
   * @param insurerId insurer profile
   * @param code branch code
   * @param details name, city, LGT rate and mailbox
   */
  public InsurerBranch(Long insurerId, String code, BranchDetails details) {
    this.insurerId = insurerId;
    this.code = code;
    apply(details);
  }

  /**
   * Changes the branch; it must be authorized again.
   *
   * @param details new attributes
   */
  public void update(BranchDetails details) {
    apply(details);
    markModified();
  }

  private void apply(BranchDetails d) {
    if (d.lgtRate() == null
        || d.lgtRate().signum() < 0
        || d.lgtRate().compareTo(EffectiveDatedRecord.MAX_PERCENT) > 0) {
      throw new BusinessRuleException("RATE_INVALID", "The LGT rate must be between 0 and 100 %");
    }
    this.name = d.name();
    this.city = d.city();
    this.lgtRate = d.lgtRate();
    this.placementEmail = d.placementEmail();
  }

  @Override
  public String catalogReference() {
    return insurerId + "/" + code;
  }

  @Override
  public String catalogDescription() {
    return name + " (LGT " + lgtRate.stripTrailingZeros().toPlainString() + " %)";
  }

  public Long getInsurerId() {
    return insurerId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }

  public BigDecimal getLgtRate() {
    return lgtRate;
  }

  public String getPlacementEmail() {
    return placementEmail;
  }

  /**
   * Maintainable attributes of an insurer branch.
   *
   * @param name branch name
   * @param city city
   * @param lgtRate Local Government Tax rate in percent
   * @param placementEmail branch placement mailbox, may be null
   */
  public record BranchDetails(
      String name, String city, BigDecimal lgtRate, String placementEmail) {}
}
