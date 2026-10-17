package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFileRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Payment;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentRepository;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The report of a payment file run (FRS.CSH.05.01.12): the count and amount per category (Applied
 * Payment, Excess Payment, Matched on Pre-booked, Unbooked/Unmatched, and the duplicates and failed
 * rows) and the list of the payment records with the transaction date, BP filename, transaction
 * number, amount, reference number, account number and record status, on the screen and in Excel.
 */
@Service
@Transactional(readOnly = true)
public class ChannelRunReport {

  private static final Map<String, String> LABELS =
      Map.ofEntries(
          Map.entry("APPLIED", "Applied Payment"),
          Map.entry("EXCESS", "Unapplied - Excess Payment"),
          Map.entry("PREBOOKED", "Unapplied - Pre-booked Payment"),
          Map.entry("MATCHED_ON_PREBOOKED", "Unapplied - Pre-booked Payment"),
          Map.entry("UNAPPLIED_NO_MATCH", "Unapplied - Unbooked/Unmatched"),
          Map.entry("UNBOOKED_UNMATCHED", "Unapplied - Unbooked/Unmatched"),
          Map.entry("CANCELLED_REFERENCE", "Unapplied - Unbooked/Unmatched"),
          Map.entry("DUPLICATE", "Duplicate"),
          Map.entry("INVALID_BP_FILENAME", "Invalid BP filename"),
          Map.entry("INVALID_TRANSACTION_NUMBER", "Invalid Transaction Number"),
          Map.entry("COMMITTED", "Processed"),
          Map.entry("FAILED", "Failed"));
  private static final List<String> AMOUNT_FIELDS =
      List.of("Amount", "Credit", "Paid amount", "Basic commission");

  private final ChannelFileRepository files;
  private final BulkService bulk;
  private final PaymentRepository payments;
  private final DocumentComposer composer;

  /**
   * Creates the report.
   *
   * @param files files received
   * @param bulk rows of the run
   * @param payments payment records of the run
   * @param composer Excel
   */
  public ChannelRunReport(
      ChannelFileRepository files,
      BulkService bulk,
      PaymentRepository payments,
      DocumentComposer composer) {
    this.files = files;
    this.bulk = bulk;
    this.payments = payments;
    this.composer = composer;
  }

  /**
   * The report of a file.
   *
   * @param fileId file received
   * @return summary and lines
   */
  public Report report(Long fileId) {
    ChannelFile file =
        files
            .findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment file", fileId));
    List<Line> lines = new ArrayList<>();
    if (file.getBulkJobId() != null) {
      for (BulkRowRecord row :
          bulk.rows(file.getBulkJobId(), null, Pageable.unpaged()).getContent()) {
        lines.add(line(file, row));
      }
    }
    Map<String, Summary> summary = new LinkedHashMap<>();
    for (Line l : lines) {
      summary.merge(
          l.status(),
          new Summary(l.status(), 1, l.amount()),
          (a, b) -> new Summary(a.category(), a.count() + 1, a.amount().add(b.amount())));
    }
    return new Report(
        file.getUploadRef(),
        file.getFileName(),
        file.getStatus(),
        file.getMessage(),
        List.copyOf(summary.values()),
        lines);
  }

  private Line line(ChannelFile file, BulkRowRecord row) {
    Map<String, String> values = bulk.values(row);
    BigDecimal amount =
        AMOUNT_FIELDS.stream()
            .map(values::get)
            .filter(v -> v != null && !v.isBlank())
            .findFirst()
            .map(ChannelFileReader::number)
            .orElse(BigDecimal.ZERO);
    Optional<Payment> payment =
        row.getResultRef() == null
            ? Optional.empty()
            : payments.findByPaymentNo(row.getResultRef());
    String status = status(row);
    return new Line(
        row.getRowNo(),
        payment.map(Payment::getValueDate).orElse(null),
        values.getOrDefault("BP filename", file.getFileName()),
        payment
            .map(p -> p.getSourceKey().substring(p.getSourceKey().lastIndexOf(':') + 1))
            .orElse(String.valueOf(row.getRowNo())),
        amount,
        payment.map(Payment::getReference).orElse(firstReference(values)),
        payment.map(Payment::getMatchedRef).orElse(null),
        LABELS.getOrDefault(status, status),
        row.getMessages());
  }

  private static String status(BulkRowRecord row) {
    if (row.getStatus() == BulkRowStatus.INVALID || row.getStatus() == BulkRowStatus.FAILED) {
      return "FAILED";
    }
    return row.getOutcome() == null ? row.getStatus().name() : row.getOutcome();
  }

  private static String firstReference(Map<String, String> values) {
    for (String key :
        List.of(
            "Invoice #",
            "Invoice",
            "Reference",
            "Account ref no",
            "Invoice no",
            "Transaction description")) {
      String v = values.get(key);
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }

  /**
   * The report as an Excel workbook: the summary and the list.
   *
   * @param fileId file received
   * @return xlsx bytes
   */
  public byte[] excel(Long fileId) {
    Report r = report(fileId);
    List<List<Object>> summary = new ArrayList<>();
    r.summary().forEach(s -> summary.add(List.of(s.category(), s.count(), s.amount())));
    List<List<Object>> lines = new ArrayList<>();
    for (Line l : r.lines()) {
      lines.add(
          List.of(
              l.rowNo(),
              l.transactionDate() == null ? "" : l.transactionDate().toString(),
              nz(l.fileName()),
              nz(l.transactionNo()),
              l.amount(),
              nz(l.reference()),
              nz(l.accountNo()),
              l.status(),
              nz(l.messages())));
    }
    return composer.xlsx(
        List.of(
            new SheetSpec("Summary", List.of("Category", "Count", "Amount"), summary),
            new SheetSpec(
                "Payment records",
                List.of(
                    "Row",
                    "Transaction Date",
                    "BP Filename",
                    "Transaction Number",
                    "Amount",
                    "Reference Number",
                    "Account Number",
                    "Record Status",
                    "Reason for Failure"),
                lines)));
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }

  /**
   * The report of a run.
   *
   * @param uploadRef upload reference
   * @param fileName file name
   * @param fileStatus status of the file
   * @param message reason of a refusal
   * @param summary count and amount per category
   * @param lines payment records
   */
  public record Report(
      String uploadRef,
      String fileName,
      String fileStatus,
      String message,
      List<Summary> summary,
      List<Line> lines) {

    /** Defensive copies. */
    public Report {
      summary = List.copyOf(summary);
      lines = List.copyOf(lines);
    }
  }

  /**
   * A category of the summary.
   *
   * @param category category
   * @param count records
   * @param amount amount
   */
  public record Summary(String category, long count, BigDecimal amount) {}

  /**
   * A payment record of the list.
   *
   * @param rowNo row of the file
   * @param transactionDate transaction (value) date
   * @param fileName BP filename
   * @param transactionNo transaction number
   * @param amount amount
   * @param reference reference number
   * @param accountNo account number matched
   * @param status record status
   * @param messages reason of a failed row
   */
  public record Line(
      int rowNo,
      LocalDate transactionDate,
      String fileName,
      String transactionNo,
      BigDecimal amount,
      String reference,
      String accountNo,
      String status,
      String messages) {}
}
