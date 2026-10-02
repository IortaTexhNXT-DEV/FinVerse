package com.iortatechnxt.brokerverse.adjustment.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One invoice balance written off or credited by the minimal balance file (ADJID.026): at most once
 * per invoice.
 */
@Entity
@Table(name = "adj_min_balance_item")
public class MinBalanceItem extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "file_ref", nullable = false, length = 40, updatable = false)
  private String fileRef;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal balance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private WriteOffAction action;

  @Column(name = "journal_batch_no", length = 40, updatable = false)
  private String journalBatchNo;

  protected MinBalanceItem() {}

  /**
   * A written-off or credited balance.
   *
   * @param companyId company
   * @param fileRef upload (bulk job) or request number
   * @param invoice invoice number, ARN, client and currency
   * @param balance balance cleared (positive debit, negative credit)
   * @param action write-off or credit
   * @param journalBatchNo journal posted
   */
  public MinBalanceItem(
      Long companyId,
      String fileRef,
      InvoiceKeys invoice,
      BigDecimal balance,
      WriteOffAction action,
      String journalBatchNo) {
    this.companyId = companyId;
    this.fileRef = fileRef;
    this.invoiceNo = invoice.invoiceNo();
    this.arn = invoice.arn();
    this.clientCode = invoice.clientCode();
    this.currency = invoice.currency();
    this.balance = balance;
    this.action = action;
    this.journalBatchNo = journalBatchNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getFileRef() {
    return fileRef;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public WriteOffAction getAction() {
    return action;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  /**
   * Keys of the invoice concerned.
   *
   * @param invoiceNo invoice number
   * @param arn ARN
   * @param clientCode client
   * @param currency currency
   */
  public record InvoiceKeys(String invoiceNo, String arn, String clientCode, String currency) {}
}
