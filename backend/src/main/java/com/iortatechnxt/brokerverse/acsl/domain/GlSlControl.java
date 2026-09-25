package com.iortatechnxt.brokerverse.acsl.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * Which sub-ledger backs a control account in the GL-SL reconciliation (ACSL 2.13.2). Configuration
 * by account code, because the real chart comes from BDOI (AQ01, OQ07): the Operations invoice
 * ledger components for premium receivable, due to insurers or commission receivable, the open
 * items of some document types, or the party postings of the account (the default).
 */
@Entity
@Table(name = "acsl_glsl_control")
public class GlSlControl extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "account_code", nullable = false, length = 30, updatable = false)
  private String accountCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SlSource source;

  @Column(length = 200)
  private String components;

  @Column(name = "document_types", length = 200)
  private String documentTypes;

  @Column(length = 3)
  private String currency;

  @Column(nullable = false)
  private boolean active = true;

  protected GlSlControl() {}

  /**
   * Configures a control account.
   *
   * @param companyId company
   * @param accountCode control account
   * @param setting source, components, document types and currency
   */
  public GlSlControl(Long companyId, String accountCode, Setting setting) {
    this.companyId = companyId;
    this.accountCode = accountCode;
    this.source = setting.source();
    this.components = setting.components();
    this.documentTypes = setting.documentTypes();
    this.currency = setting.currency();
    this.active = setting.active();
  }

  /**
   * Changes the configuration.
   *
   * @param setting source, components, document types, currency and active flag
   */
  public void change(Setting setting) {
    this.source = setting.source();
    this.components = setting.components();
    this.documentTypes = setting.documentTypes();
    this.currency = setting.currency();
    this.active = setting.active();
  }

  /**
   * The configured Operations components.
   *
   * @return components
   */
  public List<String> componentList() {
    return split(components);
  }

  /**
   * The configured open-item document types.
   *
   * @return document types
   */
  public List<String> documentTypeList() {
    return split(documentTypes);
  }

  private static List<String> split(String csv) {
    return csv == null || csv.isBlank()
        ? List.of()
        : Arrays.stream(csv.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public SlSource getSource() {
    return source;
  }

  public String getComponents() {
    return components;
  }

  public String getDocumentTypes() {
    return documentTypes;
  }

  public String getCurrency() {
    return currency;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * A configuration.
   *
   * @param source sub-ledger
   * @param components Operations components (OPS_LEDGER), comma separated
   * @param documentTypes open-item document types (OPEN_ITEMS), comma separated
   * @param currency currency of the Operations invoices (OPS_LEDGER), may be null
   * @param active whether it is used
   */
  public record Setting(
      SlSource source, String components, String documentTypes, String currency, boolean active) {}
}
