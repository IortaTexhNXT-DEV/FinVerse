package com.iortatechnxt.brokerverse.crm.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * CA / SA information of a client (MKT 2.25.0, Addendum 1): the BDO account a refund is credited to
 * with its payee name, or the payee name of a check. Captured from an approved refund request and
 * never overwritten: a changed account is a new row, an old one is deactivated (MKT 2.25.1).
 */
@Entity
@Table(name = "crm_client_payout_account")
public class ClientPayoutAccount extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private PayoutMode mode;

  @Column(name = "payee_name", nullable = false, length = 250, updatable = false)
  private String payeeName;

  @Column(name = "payee_key", nullable = false, length = 250, updatable = false)
  private String payeeKey;

  @Column(name = "account_no", length = 40, updatable = false)
  private String accountNo;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "deactivated_by", length = 50)
  private String deactivatedBy;

  @Column(name = "deactivated_at")
  private Instant deactivatedAt;

  protected ClientPayoutAccount() {}

  /**
   * Records CA / SA information.
   *
   * @param client client
   * @param details mode, payee name and account number
   * @param sourceModule module that captured it (PAYREQUEST)
   * @param sourceRef its reference (refund request number)
   */
  public ClientPayoutAccount(
      Client client, PayoutDetails details, String sourceModule, String sourceRef) {
    this.companyId = client.getCompanyId();
    this.clientId = client.getId();
    this.clientCode = client.getCode();
    this.mode = details.mode();
    this.payeeName = details.payeeName().strip();
    this.payeeKey = key(details.payeeName());
    this.accountNo = details.mode() == PayoutMode.CTA ? details.accountNo() : null;
    this.sourceModule = sourceModule;
    this.sourceRef = sourceRef;
  }

  /**
   * The duplicate key of a payee name (MKT 2.25.1): upper case, single spaces.
   *
   * @param payeeName payee name
   * @return key
   */
  public static String key(String payeeName) {
    return payeeName.strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
  }

  /**
   * Deactivates the row (a newer account replaces it).
   *
   * @param user user
   * @param when time
   */
  public void deactivate(String user, Instant when) {
    this.active = false;
    this.deactivatedBy = user;
    this.deactivatedAt = when;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public PayoutMode getMode() {
    return mode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public boolean isActive() {
    return active;
  }

  public String getDeactivatedBy() {
    return deactivatedBy;
  }

  public Instant getDeactivatedAt() {
    return deactivatedAt;
  }
}
