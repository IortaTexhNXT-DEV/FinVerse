package com.iortatechnxt.brokerverse.opsledger.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;

/**
 * The frozen original values of an open legacy invoice (DATA_MIGRATION_DESIGN 14.1, BRID 10.1):
 * header amounts and, per component, the booked, adjusted, paid, remitted, written-off and open
 * amounts at cut-over, written once by the legacy invoice intake and never updated. It is the
 * "original value" of the legacy changes report and of the legacy block of the invoice 360.
 */
@Entity
@Table(name = "ops_invoice_origin_snapshot")
public class OpsInvoiceOriginSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "invoice_id", nullable = false, updatable = false)
  private Long invoiceId;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "source_system", nullable = false, length = 10, updatable = false)
  private String sourceSystem;

  @Column(name = "legacy_invoice_no", nullable = false, length = 40, updatable = false)
  private String legacyInvoiceNo;

  @Column(name = "legacy_ref", length = 80, updatable = false)
  private String legacyRef;

  @Column(name = "legacy_service_invoice_no", length = 40, updatable = false)
  private String legacyServiceInvoiceNo;

  @Column(name = "invoice_date", nullable = false, updatable = false)
  private LocalDate invoiceDate;

  @Column(name = "due_date", updatable = false)
  private LocalDate dueDate;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal grossPremium;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal commission;

  @Column(name = "vat_on_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal vatOnCommission;

  @Column(name = "commission_realised", nullable = false, precision = 19, scale = 2)
  private BigDecimal commissionRealised;

  @Column(name = "deferred_vat_open", nullable = false, precision = 19, scale = 2)
  private BigDecimal deferredVatOpen;

  @Column(name = "open_balance_mode", nullable = false, updatable = false)
  private boolean openBalanceMode;

  @Column(nullable = false, length = 500, updatable = false)
  private String shares;

  @Column(name = "migration_batch", nullable = false, length = 20, updatable = false)
  private String migrationBatch;

  @Column(name = "taken_at", nullable = false, updatable = false)
  private Instant takenAt;

  @Column(name = "taken_by", nullable = false, length = 50, updatable = false)
  private String takenBy;

  @ElementCollection
  @CollectionTable(
      name = "ops_invoice_origin_line",
      joinColumns = @JoinColumn(name = "snapshot_id"))
  @OrderColumn(name = "line_index")
  private final List<Line> lines = new ArrayList<>();

  protected OpsInvoiceOriginSnapshot() {}

  /**
   * Takes the snapshot of a legacy invoice just created.
   *
   * @param invoice the invoice (saved, origin MIGRATED)
   * @param header legacy header facts
   * @param lines positions per component
   * @param takenBy user
   * @param takenAt time
   */
  public OpsInvoiceOriginSnapshot(
      OpsInvoice invoice, Header header, List<Line> lines, String takenBy, Instant takenAt) {
    this.invoiceId = invoice.getId();
    this.companyId = invoice.getCompanyId();
    this.sourceSystem = invoice.getRecordOrigin().sourceSystem();
    this.legacyInvoiceNo = invoice.getLegacy().legacyInvoiceNo();
    this.legacyRef = invoice.getRecordOrigin().legacyRef();
    this.migrationBatch = invoice.getRecordOrigin().migrationBatch();
    this.currency = invoice.getCurrency();
    this.grossPremium = invoice.getGrossPremium();
    this.commission = invoice.getCommission();
    this.vatOnCommission = invoice.getVatOnCommission();
    this.shares =
        invoice.getShares().stream()
            .map(
                s ->
                    s.insurerCode() + " " + s.sharePct().stripTrailingZeros().toPlainString() + "%")
            .collect(Collectors.joining(", "));
    this.legacyServiceInvoiceNo = header.legacyServiceInvoiceNo();
    this.invoiceDate = header.invoiceDate();
    this.dueDate = header.dueDate();
    this.commissionRealised = header.commissionRealised();
    this.deferredVatOpen = header.deferredVatOpen();
    this.openBalanceMode = header.openBalanceMode();
    this.lines.addAll(lines);
    this.takenBy = takenBy;
    this.takenAt = takenAt;
  }

  /** Loads the lines (reads outside the persistence context). */
  public void loadLines() {
    Hibernate.initialize(lines);
  }

  /**
   * Legacy header facts kept on the snapshot.
   *
   * @param legacyServiceInvoiceNo commission service invoice issued in legacy
   * @param invoiceDate legacy invoice date
   * @param dueDate legacy due date
   * @param commissionRealised commission already realised in legacy
   * @param deferredVatOpen deferred output VAT still open
   * @param openBalanceMode legacy gave open balances only (no original amounts)
   */
  public record Header(
      String legacyServiceInvoiceNo,
      LocalDate invoiceDate,
      LocalDate dueDate,
      BigDecimal commissionRealised,
      BigDecimal deferredVatOpen,
      boolean openBalanceMode) {}

  /**
   * Position of one component at cut-over.
   *
   * @param component component
   * @param booked original booked amount
   * @param adjusted net legacy adjustments on the invoice
   * @param paid paid before cut-over
   * @param remitted settled with the insurer before cut-over
   * @param writtenOff written off or DP-reversed before cut-over
   * @param openBalance open balance at cut-over
   */
  @Embeddable
  public record Line(
      @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) LedgerComponent component,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal booked,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal adjusted,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal paid,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal remitted,
      @Column(name = "written_off", nullable = false, precision = 19, scale = 2)
          BigDecimal writtenOff,
      @Column(name = "open_balance", nullable = false, precision = 19, scale = 2)
          BigDecimal openBalance) {

    /**
     * Whether the positions add up: booked + adjusted - paid - remitted - written off = open.
     *
     * @return true when consistent
     */
    public boolean consistent() {
      return booked
              .add(adjusted)
              .subtract(paid)
              .subtract(remitted)
              .subtract(writtenOff)
              .compareTo(openBalance)
          == 0;
    }
  }

  public Long getId() {
    return id;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public String getLegacyInvoiceNo() {
    return legacyInvoiceNo;
  }

  public String getLegacyRef() {
    return legacyRef;
  }

  public String getLegacyServiceInvoiceNo() {
    return legacyServiceInvoiceNo;
  }

  public LocalDate getInvoiceDate() {
    return invoiceDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getVatOnCommission() {
    return vatOnCommission;
  }

  public BigDecimal getCommissionRealised() {
    return commissionRealised;
  }

  public BigDecimal getDeferredVatOpen() {
    return deferredVatOpen;
  }

  public boolean isOpenBalanceMode() {
    return openBalanceMode;
  }

  public String getShares() {
    return shares;
  }

  public String getMigrationBatch() {
    return migrationBatch;
  }

  public Instant getTakenAt() {
    return takenAt;
  }

  public String getTakenBy() {
    return takenBy;
  }

  public List<Line> getLines() {
    return Collections.unmodifiableList(lines);
  }
}
