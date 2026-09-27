package com.iortatechnxt.brokerverse.migration.intake.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A source extract (DATA_MIGRATION_DESIGN section 6; FR-DM-010): one file of one layout, source
 * system and as-of date with its control file, the SHA-256, the declared and parsed counts, the
 * control totals and the result of the intake checks. The files are in the file store (record class
 * MIGRATION_EXTRACT); the counts, hashes and totals survive the purge as reconciliation evidence.
 */
@Entity
@Table(name = "mig_extract")
public class MigExtract extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "extract_no", nullable = false, length = 20, updatable = false)
  private String extractNo;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(name = "layout_code", nullable = false, length = 10, updatable = false)
  private String layoutCode;

  @Column(name = "layout_version", nullable = false, updatable = false)
  private int layoutVersion;

  @Column(name = "source_system", nullable = false, length = 10, updatable = false)
  private String sourceSystem;

  @Column(name = "as_of", nullable = false, updatable = false)
  private LocalDateTime asOf;

  @Column(name = "sequence_no", nullable = false, updatable = false)
  private int sequenceNo;

  @Column(nullable = false, length = 10, updatable = false)
  private String mode;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Column(name = "control_file_name", length = 255, updatable = false)
  private String controlFileName;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  @Column(name = "control_file_id")
  private Long controlFileId;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(name = "extracted_at")
  private LocalDateTime extractedAt;

  @Column(name = "extracted_by", length = 60)
  private String extractedBy;

  @Column(name = "declared_rows")
  private Integer declaredRows;

  @Column(name = "parsed_rows", nullable = false)
  private int parsedRows;

  @Column(name = "staged_rows", nullable = false)
  private int stagedRows;

  @Column(name = "control_totals", columnDefinition = "text")
  private String controlTotals;

  @Column(name = "hash_total", length = 40)
  private String hashTotal;

  @Column(nullable = false)
  private boolean masked;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private ExtractStatus status = ExtractStatus.RECEIVED;

  @Column(name = "reject_code", length = 40)
  private String rejectCode;

  @Column(name = "reject_message", length = 2000)
  private String rejectMessage;

  @Column(name = "received_by", nullable = false, length = 50, updatable = false)
  private String receivedBy;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "checked_at")
  private Instant checkedAt;

  @Column(name = "purged_at")
  private Instant purgedAt;

  protected MigExtract() {}

  /**
   * A received extract.
   *
   * @param companyId company
   * @param extractNo number MGX-yyyy-nnnnnn
   * @param file identity of the file
   * @param receivedBy operator
   * @param receivedAt time
   */
  public MigExtract(
      Long companyId, String extractNo, FileIdentity file, String receivedBy, Instant receivedAt) {
    this.companyId = companyId;
    this.extractNo = extractNo;
    this.objectCode = file.objectCode();
    this.layoutCode = file.layoutCode();
    this.layoutVersion = file.layoutVersion();
    this.sourceSystem = file.sourceSystem();
    this.asOf = file.asOf();
    this.sequenceNo = file.sequenceNo();
    this.mode = file.mode();
    this.fileName = file.fileName();
    this.controlFileName = file.controlFileName();
    this.sha256 = file.sha256();
    this.receivedBy = receivedBy;
    this.receivedAt = receivedAt;
  }

  /**
   * Records the control file facts.
   *
   * @param declared declared row count
   * @param totals control totals (JSON)
   * @param hash declared hash total
   * @param extractedAt extraction time
   * @param extractedBy extracted by
   */
  public void control(
      Integer declared, String totals, String hash, LocalDateTime extractedAt, String extractedBy) {
    this.declaredRows = declared;
    this.controlTotals = totals;
    this.hashTotal = hash;
    this.extractedAt = extractedAt;
    this.extractedBy = extractedBy;
  }

  /**
   * Keeps the stored files.
   *
   * @param dataFile data file
   * @param controlFile control file
   */
  public void files(Long dataFile, Long controlFile) {
    this.storedFileId = dataFile;
    this.controlFileId = controlFile;
  }

  /**
   * The extract failed a check.
   *
   * @param code check code
   * @param message message
   * @param parsed rows parsed
   * @param when time
   */
  public void reject(String code, String message, int parsed, Instant when) {
    this.status = ExtractStatus.REJECTED;
    this.rejectCode = code;
    this.rejectMessage = message;
    this.parsedRows = parsed;
    this.checkedAt = when;
  }

  /**
   * The extract passed the checks and was staged.
   *
   * @param parsed rows parsed
   * @param staged rows staged
   * @param isMasked personal data masked
   * @param when time
   */
  public void staged(int parsed, int staged, boolean isMasked, Instant when) {
    this.status = ExtractStatus.STAGED;
    this.parsedRows = parsed;
    this.stagedRows = staged;
    this.masked = isMasked;
    this.checkedAt = when;
  }

  /**
   * Payloads and files purged.
   *
   * @param when time
   */
  public void purged(Instant when) {
    this.status = ExtractStatus.PURGED;
    this.purgedAt = when;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getExtractNo() {
    return extractNo;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public String getLayoutCode() {
    return layoutCode;
  }

  public int getLayoutVersion() {
    return layoutVersion;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public LocalDateTime getAsOf() {
    return asOf;
  }

  public int getSequenceNo() {
    return sequenceNo;
  }

  public String getMode() {
    return mode;
  }

  public String getFileName() {
    return fileName;
  }

  public String getControlFileName() {
    return controlFileName;
  }

  public Long getStoredFileId() {
    return storedFileId;
  }

  public Long getControlFileId() {
    return controlFileId;
  }

  public String getSha256() {
    return sha256;
  }

  public LocalDateTime getExtractedAt() {
    return extractedAt;
  }

  public String getExtractedBy() {
    return extractedBy;
  }

  public Integer getDeclaredRows() {
    return declaredRows;
  }

  public int getParsedRows() {
    return parsedRows;
  }

  public int getStagedRows() {
    return stagedRows;
  }

  public String getControlTotals() {
    return controlTotals;
  }

  public String getHashTotal() {
    return hashTotal;
  }

  public boolean isMasked() {
    return masked;
  }

  public ExtractStatus getStatus() {
    return status;
  }

  public String getRejectCode() {
    return rejectCode;
  }

  public String getRejectMessage() {
    return rejectMessage;
  }

  public String getReceivedBy() {
    return receivedBy;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getCheckedAt() {
    return checkedAt;
  }

  public Instant getPurgedAt() {
    return purgedAt;
  }

  /**
   * Identity of an extract file.
   *
   * @param objectCode data object
   * @param layoutCode layout
   * @param layoutVersion layout version in force
   * @param sourceSystem source system
   * @param asOf as-of date and time
   * @param sequenceNo sequence of the day
   * @param mode FULL or DELTA
   * @param fileName data file name
   * @param controlFileName control file name
   * @param sha256 SHA-256 of the data file
   */
  public record FileIdentity(
      String objectCode,
      String layoutCode,
      int layoutVersion,
      String sourceSystem,
      LocalDateTime asOf,
      int sequenceNo,
      String mode,
      String fileName,
      String controlFileName,
      String sha256) {}
}
