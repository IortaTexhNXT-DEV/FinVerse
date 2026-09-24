package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.common.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Policy facts copied onto the claim at registration: the policy header as it stood at the date of
 * loss, the insured risk and the company's coinsurance share. Keeping them on the claim lets the
 * claim reports filter and group without reading the underwriting tables.
 */
@Embeddable
public class ClaimPolicy {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @Column(name = "policy_id", nullable = false)
  private Long policyId;

  @Column(name = "policy_no", nullable = false, length = 40)
  private String policyNo;

  @Column(name = "product_code", nullable = false, length = 20)
  private String productCode;

  @Column(name = "product_name", nullable = false, length = 120)
  private String productName;

  @Column(name = "business_line", nullable = false, length = 20)
  private String businessLine;

  @Column(name = "customer_code", nullable = false, length = 30)
  private String customerCode;

  @Column(name = "customer_name", nullable = false, length = 200)
  private String customerName;

  @Column(name = "insured_name", nullable = false, length = 200)
  private String insuredName;

  @Column(name = "intermediary_code", length = 30)
  private String intermediaryCode;

  @Column(name = "uw_year", nullable = false)
  private int uwYear;

  @Column(name = "share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @Column(name = "coinsurance_leader", nullable = false)
  private boolean coinsuranceLeader;

  @Column(name = "coinsurer_code", length = 30)
  private String coinsurerCode;

  @Column(name = "risk_id")
  private Long riskId;

  @Column(name = "risk_description", length = 300)
  private String riskDescription;

  @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal sumInsured;

  protected ClaimPolicy() {}

  /**
   * Creates the policy facts.
   *
   * @param values policy values
   */
  public ClaimPolicy(ClaimPolicyValues values) {
    this.policyId = values.policyId();
    this.policyNo = values.policyNo();
    this.productCode = values.productCode();
    this.productName = values.productName();
    this.businessLine = values.businessLine();
    this.customerCode = values.customerCode();
    this.customerName = values.customerName();
    this.insuredName = values.insuredName();
    this.intermediaryCode = values.intermediaryCode();
    this.uwYear = values.uwYear();
    this.sharePct = values.sharePct();
    this.coinsuranceLeader = values.coinsuranceLeader();
    this.coinsurerCode = values.coinsurerCode();
    this.riskId = values.riskId();
    this.riskDescription = values.riskDescription();
    this.sumInsured = Money.nz(values.sumInsured());
  }

  /**
   * Company share of an amount at 100 % (share % of the policy, rounded to cents).
   *
   * @param amount100 amount at 100 %
   * @return company share
   */
  public BigDecimal ourShare(BigDecimal amount100) {
    return amount100.multiply(sharePct).divide(HUNDRED, Money.SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * Coinsurance split of an amount added to a running 100 % total (settled or recovered). The
   * company share is the difference of the rounded cumulative shares, so the shares of all
   * movements always add up to the share of the total.
   *
   * @param before100 running total before the amount, at 100 %
   * @param amount100 amount at 100 %
   * @return split (see {@link ShareSplit})
   */
  public ShareSplit split(BigDecimal before100, BigDecimal amount100) {
    BigDecimal ours = ourShare(before100.add(amount100)).subtract(ourShare(before100));
    if (leadsCoinsurance()) {
      return new ShareSplit(ours, amount100, amount100.subtract(ours));
    }
    return new ShareSplit(ours, ours, Money.zero());
  }

  /**
   * Whether the company leads a coinsurance and therefore pays and collects 100 %.
   *
   * @return true when leading with a coinsurer
   */
  public boolean leadsCoinsurance() {
    return coinsuranceLeader && coinsurerCode != null;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getProductName() {
    return productName;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public String getCustomerCode() {
    return customerCode;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getInsuredName() {
    return insuredName;
  }

  public String getIntermediaryCode() {
    return intermediaryCode;
  }

  public int getUwYear() {
    return uwYear;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public boolean isCoinsuranceLeader() {
    return coinsuranceLeader;
  }

  public String getCoinsurerCode() {
    return coinsurerCode;
  }

  public Long getRiskId() {
    return riskId;
  }

  public String getRiskDescription() {
    return riskDescription;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }
}
