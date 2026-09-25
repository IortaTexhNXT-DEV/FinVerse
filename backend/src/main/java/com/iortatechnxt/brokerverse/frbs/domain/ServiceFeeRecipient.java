package com.iortatechnxt.brokerverse.frbs.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Who receives the service fee of a sales unit (FRBS 2.10.0; recipients to confirm, AQ20): the
 * payee sent to Disbursement and the cost centre charged by the accrual (account 5614 requires
 * one). A unit without a recipient is paid under its own code and charged to its own cost centre.
 */
@Entity
@Table(name = "frbs_service_fee_recipient")
public class ServiceFeeRecipient extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "sales_unit", nullable = false, length = 20, updatable = false)
  private String salesUnit;

  @Column(name = "payee_code", nullable = false, length = 30)
  private String payeeCode;

  @Column(name = "payee_name", nullable = false, length = 250)
  private String payeeName;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(nullable = false)
  private boolean active;

  protected ServiceFeeRecipient() {}

  /**
   * A new recipient.
   *
   * @param companyId company
   * @param salesUnit sales unit
   * @param values payee, cost centre and status
   */
  public ServiceFeeRecipient(Long companyId, String salesUnit, RecipientValues values) {
    this.companyId = companyId;
    this.salesUnit = salesUnit;
    apply(values);
  }

  /**
   * Replaces the payee, cost centre and status.
   *
   * @param values content
   */
  public final void apply(RecipientValues values) {
    payeeCode = values.payeeCode();
    payeeName = values.payeeName();
    costCenter = values.costCenter();
    active = values.active();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * The content of a recipient.
   *
   * @param payeeCode payee party code
   * @param payeeName payee name
   * @param costCenter cost centre charged, null for the derivation rules
   * @param active whether used
   */
  public record RecipientValues(
      String payeeCode, String payeeName, String costCenter, boolean active) {}
}
