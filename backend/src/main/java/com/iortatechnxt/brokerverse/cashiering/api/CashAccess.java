package com.iortatechnxt.brokerverse.cashiering.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Security expressions and helpers of the cashiering endpoints (OPERATIONS_DESIGN 6.1). */
final class CashAccess {

  /** Read access to the cashiering records. */
  static final String VIEW =
      "hasAnyAuthority('CASH_RECEIPT', 'CASH_APPROVE', 'CASH_APPLY', 'CASH_DISPOSITION', 'CASH_PRINT',"
          + " 'CASH_UPLOAD', 'CWT_PROCESS')";

  /** Receipts and over-the-counter payments. */
  static final String RECEIPT = "hasAuthority('CASH_RECEIPT')";

  /** Cancellation requests. */
  static final String CANCEL = "hasAuthority('CASH_CANCEL')";

  /** Reinstatement requests. */
  static final String REINSTATE = "hasAuthority('CASH_REINSTATE')";

  /** Approval of cancellations and reinstatements. */
  static final String APPROVE = "hasAuthority('CASH_APPROVE')";

  /** Applications, re-matching and the pre-booked queue. */
  static final String APPLY = "hasAuthority('CASH_APPLY')";

  /** Uploads and the PDC warehouse. */
  static final String UPLOAD = "hasAuthority('CASH_UPLOAD')";

  /** Unapplied workbench. */
  static final String DISPOSITION = "hasAuthority('CASH_DISPOSITION')";

  /** Approval of dispositions and reversals. */
  static final String DISPOSITION_APPROVE = "hasAuthority('CASH_DISPOSITION_APPROVE')";

  /** Receipt series master. */
  static final String SERIES = "hasAuthority('CASH_SERIES_MANAGE')";

  /** Authorization of a series (checker). */
  static final String SERIES_AUTHORIZE =
      "hasAnyAuthority('CASH_SERIES_MANAGE', 'MASTER_AUTHORIZE')";

  /** Batch printing. */
  static final String PRINT = "hasAuthority('CASH_PRINT')";

  /** Marketing 2307 tagging. */
  static final String CWT_TAG = "hasAuthority('CWT_TAG')";

  /** Cashiering 2307 validation and report. */
  static final String CWT_PROCESS = "hasAuthority('CWT_PROCESS')";

  /** Read access to the 2307 tags. */
  static final String CWT_VIEW = "hasAnyAuthority('CWT_TAG', 'CWT_PROCESS', 'DISB_PROCESS')";

  /** Disbursement release of 2307 certificates. */
  static final String DISBURSEMENT = "hasAuthority('DISB_PROCESS')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private CashAccess() {}

  /**
   * A bounded page request.
   *
   * @param page page
   * @param size size
   * @return pageable
   */
  static Pageable page(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE));
  }

  /**
   * A bounded page request, newest first.
   *
   * @param page page
   * @param size size
   * @return pageable sorted by id descending
   */
  static Pageable newest(int page, int size) {
    Pageable p = page(page, size);
    return PageRequest.of(p.getPageNumber(), p.getPageSize(), Sort.by("id").descending());
  }

  /**
   * A PDF download.
   *
   * @param fileName file name
   * @param content PDF
   * @return response
   */
  static ResponseEntity<byte[]> pdf(String fileName, byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName).build().toString())
        .body(content);
  }
}
