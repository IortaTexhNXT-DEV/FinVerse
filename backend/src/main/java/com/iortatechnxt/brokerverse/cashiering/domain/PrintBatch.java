package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * A batch print of receipts (CSHID.019): the receipts printed or failed, and the merged PDF kept as
 * the copy of the batch. Failed receipts can be retried.
 */
@Entity
@Table(name = "csh_print_batch")
public class PrintBatch extends BaseEntity {

  /** Printed status of a line. */
  public static final String PRINTED = "PRINTED";

  /** Failed status of a line. */
  public static final String FAILED = "FAILED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 30, updatable = false)
  private String batchNo;

  @Column(length = 250, updatable = false)
  private String criteria;

  @Column(name = "requested_count", nullable = false)
  private int requestedCount;

  @Column(name = "printed_count", nullable = false)
  private int printedCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "file_name", length = 120)
  private String fileName;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "content")
  private byte[] content;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  @ElementCollection
  @CollectionTable(name = "csh_print_line", joinColumns = @JoinColumn(name = "print_batch_id"))
  @OrderColumn(name = "line_no")
  private final List<Line> lines = new ArrayList<>();

  protected PrintBatch() {}

  /**
   * Creates a batch.
   *
   * @param companyId company
   * @param batchNo PRB- number
   * @param criteria selection or filter used
   */
  public PrintBatch(Long companyId, String batchNo, String criteria) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.criteria = criteria;
    this.status = "COMPLETED";
  }

  /**
   * Adds a result line.
   *
   * @param line line
   */
  public void add(Line line) {
    lines.add(line);
  }

  /**
   * Records the file name and the counts; the merged document goes to the file store ({@link
   * #storedIn(Long)}).
   *
   * @param name file name
   */
  public void finish(String name) {
    this.fileName = name;
    this.requestedCount = lines.size();
    this.printedCount = (int) lines.stream().filter(l -> PRINTED.equals(l.getStatus())).count();
    this.failedCount = requestedCount - printedCount;
    if (failedCount == 0) {
      this.status = "COMPLETED";
    } else {
      this.status = printedCount == 0 ? FAILED : "PARTIAL";
    }
  }

  /**
   * The merged PDF of a batch printed before ST1 and not yet copied to the file store.
   *
   * @return copy of the content, empty when none
   */
  public byte[] document() {
    return content == null ? new byte[0] : content.clone();
  }

  /**
   * Records the stored file of the merged document.
   *
   * @param id stored file id
   */
  public void storedIn(Long id) {
    this.storedFileId = id;
  }

  /**
   * The stored file of the merged document; null when nothing was printed or for a batch printed
   * before ST1 and not yet copied.
   *
   * @return stored file id
   */
  public Long getStoredFileId() {
    return storedFileId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getCriteria() {
    return criteria;
  }

  public int getRequestedCount() {
    return requestedCount;
  }

  public int getPrintedCount() {
    return printedCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public String getStatus() {
    return status;
  }

  public String getFileName() {
    return fileName;
  }

  public List<Line> getLines() {
    return lines;
  }

  /** One receipt of a print batch. */
  @Embeddable
  public static class Line {

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name = "receipt_no", nullable = false, length = 40)
    private String receiptNo;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(length = 250)
    private String message;

    protected Line() {}

    /**
     * Creates a line.
     *
     * @param receiptId receipt
     * @param receiptNo receipt number
     * @param status PRINTED or FAILED
     * @param message failure message, may be null
     */
    public Line(Long receiptId, String receiptNo, String status, String message) {
      this.receiptId = receiptId;
      this.receiptNo = receiptNo;
      this.status = status;
      this.message = message;
    }

    public Long getReceiptId() {
      return receiptId;
    }

    public String getReceiptNo() {
      return receiptNo;
    }

    public String getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }
  }
}
