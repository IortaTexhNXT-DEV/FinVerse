package com.iortatechnxt.finverse.journal.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validation report of a journal upload.
 *
 * @param fileName uploaded file
 * @param committed true when valid vouchers were created (IMPORT), false for a dry run (VALIDATE)
 * @param totalRows data rows read
 * @param vouchers per-voucher outcome
 * @param rows per-row outcome
 */
public record UploadResult(
    String fileName,
    boolean committed,
    int totalRows,
    List<VoucherResult> vouchers,
    List<RowResult> rows) {

  /** Canonical constructor copying the lists. */
  public UploadResult {
    vouchers = List.copyOf(vouchers);
    rows = List.copyOf(rows);
  }

  /**
   * Number of vouchers without errors.
   *
   * @return count
   */
  public long validVouchers() {
    return vouchers.stream().filter(v -> v.status() != VoucherStatus.ERROR).count();
  }

  /**
   * Number of vouchers created as drafts.
   *
   * @return count
   */
  public long createdVouchers() {
    return vouchers.stream().filter(v -> v.status() == VoucherStatus.CREATED).count();
  }

  /** Voucher outcome. */
  public enum VoucherStatus {
    /** Passed every check (dry run). */
    VALID,
    /** Created as a draft journal. */
    CREATED,
    /** Rejected; nothing was created for this voucher. */
    ERROR
  }

  /**
   * Outcome of one voucher.
   *
   * @param voucherKey voucher key
   * @param firstRow first file row of the voucher
   * @param lineCount number of lines
   * @param totalDebit total debit in transaction currency
   * @param status outcome
   * @param batchId created journal (IMPORT only)
   * @param batchNo created journal number (IMPORT only)
   * @param messages errors
   */
  public record VoucherResult(
      String voucherKey,
      int firstRow,
      int lineCount,
      BigDecimal totalDebit,
      VoucherStatus status,
      Long batchId,
      String batchNo,
      List<String> messages) {

    /** Canonical constructor copying messages. */
    public VoucherResult {
      messages = List.copyOf(messages);
    }
  }

  /**
   * Outcome of one row.
   *
   * @param rowNumber file row number (header = 1)
   * @param voucherKey voucher key
   * @param valid true when the row passed row-level checks
   * @param messages errors
   */
  public record RowResult(int rowNumber, String voucherKey, boolean valid, List<String> messages) {

    /** Canonical constructor copying messages. */
    public RowResult {
      messages = List.copyOf(messages);
    }
  }
}
