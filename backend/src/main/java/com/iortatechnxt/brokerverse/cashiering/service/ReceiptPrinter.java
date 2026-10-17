package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptForms.PrintMark;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prints one receipt (FRS.CSH.02.04.06 to 02.04.17): gives it its certificate number at the first
 * print, marks a later print REPRINT, renders the copies asked (Client's Copy, the company's copy
 * or both) in the approved form of the print date, keeps the company's copy as a document of the
 * receipt and counts the print.
 */
@Component
@Transactional
public class ReceiptPrinter {

  /** Both copies. */
  public static final String BOTH = "BOTH";

  private static final String PDF = "application/pdf";

  private final ReceiptForms forms;
  private final CertificateNumbers certificates;
  private final StoredFileService storedFiles;
  private final Clock clock;

  /**
   * Creates the printer.
   *
   * @param forms forms
   * @param certificates certificate numbers
   * @param storedFiles company's copy kept with the receipt
   * @param clock clock
   */
  public ReceiptPrinter(
      ReceiptForms forms,
      CertificateNumbers certificates,
      StoredFileService storedFiles,
      Clock clock) {
    this.forms = forms;
    this.certificates = certificates;
    this.storedFiles = storedFiles;
    this.clock = clock;
  }

  /**
   * Prints a receipt.
   *
   * @param r receipt
   * @param copy CLIENT, COMPANY or BOTH
   * @param printDate print date
   * @return the documents printed
   */
  public Printed print(Receipt r, String copy, LocalDate printDate) {
    boolean reprint = r.getPrintedCount() > 0;
    if (r.getCertificateNo() == null) {
      r.certified(certificates.next(r.getBranchId(), printDate), printDate);
    }
    List<byte[]> pdfs = new ArrayList<>();
    if (!ReceiptForms.COMPANY_COPY.equals(copy)) {
      pdfs.add(
          forms.pdf(
              r,
              new PrintMark(ReceiptForms.CLIENT_COPY, reprint, r.getCertificateNo(), printDate)));
    }
    if (ReceiptForms.COMPANY_COPY.equals(copy) || BOTH.equals(copy)) {
      byte[] kept =
          forms.pdf(
              r,
              new PrintMark(ReceiptForms.COMPANY_COPY, reprint, r.getCertificateNo(), printDate));
      keep(r, kept, reprint);
      pdfs.add(kept);
    }
    r.printed(clock.instant());
    return new Printed(fileName(r), pdfs, reprint, r.getCertificateNo());
  }

  private void keep(Receipt r, byte[] pdf, boolean reprint) {
    storedFiles.storeChecked(
        new StoreRequest(
            new FileOwner(
                r.getCompanyId(), BatchPrintService.RECEIPT_OWNER, String.valueOf(r.getId())),
            "RECEIPT_COPY",
            BatchPrintService.RECORD_CLASS,
            r.getKind().name()
                + "_"
                + r.getReceiptNo()
                + "_Company_Copy"
                + (reprint ? "_Reprint" : "")
                + ".pdf",
            pdf,
            null),
        PDF,
        FileOrigin.GENERATED);
  }

  /**
   * The file name of a receipt: {@code <Receipt Type>_<Receipt Number>.pdf} (FRS.CSH.02.04.09.03).
   *
   * @param r receipt
   * @return file name
   */
  static String fileName(Receipt r) {
    return r.getKind().name() + "_" + r.getReceiptNo().replaceAll("[\\\\/:*?\"<>|]", "-") + ".pdf";
  }

  /**
   * A receipt printed.
   *
   * @param fileName file name in the ZIP
   * @param pdfs documents (one per copy)
   * @param reprint whether it was printed before
   * @param certificateNo certificate number
   */
  public record Printed(String fileName, List<byte[]> pdfs, boolean reprint, String certificateNo) {

    /** Defensive copy. */
    public Printed {
      pdfs = List.copyOf(pdfs);
    }
  }
}
