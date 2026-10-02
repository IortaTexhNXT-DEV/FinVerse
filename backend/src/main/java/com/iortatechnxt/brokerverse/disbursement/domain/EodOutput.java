package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.OutputKind;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.Objects;

/**
 * A file produced by an end-of-day run (DIS 2.16.1-2.16.6, 3.28.2): the DCTF, the check print
 * batch, the ATD / MC-DD / credit ticket / TT forms, the day's vouchers and the EOD reports.
 */
@Entity
@Table(name = "dsb_eod_output")
public class EodOutput extends BaseEntity {

  @Column(name = "eod_run_id", nullable = false, updatable = false)
  private Long eodRunId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private OutputKind kind;

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(name = "file_name", nullable = false, length = 200, updatable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100, updatable = false)
  private String contentType;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "content", updatable = false)
  private byte[] content;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  @Column(name = "item_count", nullable = false, updatable = false)
  private int itemCount;

  protected EodOutput() {}

  /**
   * An output file.
   *
   * @param eodRunId run
   * @param kind kind
   * @param code code (report code or output code)
   * @param file the file
   * @param itemCount number of items in it
   */
  public EodOutput(Long eodRunId, OutputKind kind, String code, OutputFile file, int itemCount) {
    this.eodRunId = eodRunId;
    this.kind = kind;
    this.code = code;
    this.fileName = file.fileName();
    this.contentType = file.contentType();
    this.itemCount = itemCount;
  }

  public Long getEodRunId() {
    return eodRunId;
  }

  public OutputKind getKind() {
    return kind;
  }

  public String getCode() {
    return code;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  /**
   * The content of an output produced before ST1 and not yet copied to the file store (null
   * otherwise).
   *
   * @return a copy of the bytes
   */
  public byte[] getContent() {
    return content == null ? null : content.clone();
  }

  /**
   * Records the stored file of the output (file store, build step ST1).
   *
   * @param id stored file id
   */
  public void storedIn(Long id) {
    this.storedFileId = id;
  }

  /**
   * The stored file of the output; null for an output produced before ST1 and not yet copied.
   *
   * @return stored file id
   */
  public Long getStoredFileId() {
    return storedFileId;
  }

  public int getItemCount() {
    return itemCount;
  }

  /**
   * A generated file.
   *
   * @param fileName file name
   * @param contentType media type
   * @param content bytes
   */
  public record OutputFile(String fileName, String contentType, byte[] content) {

    /** Defensive copy. */
    public OutputFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof OutputFile f
          && fileName.equals(f.fileName)
          && contentType.equals(f.contentType)
          && Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fileName, contentType, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return fileName + " (" + content.length + " bytes)";
    }
  }
}
