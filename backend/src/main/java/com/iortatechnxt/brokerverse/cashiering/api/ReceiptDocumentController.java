package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.service.BatchPrintService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The documents kept with a receipt (FRS.CSH.02.04.17): the company's copies printed, listed on the
 * Documents tab of the receipt and opened from there.
 */
@RestController
@RequestMapping("/api/v1/cashiering/receipts/{id}/documents")
public class ReceiptDocumentController {

  private final CashReceiptService receipts;
  private final BatchPrintService printing;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param receipts receipts
   * @param printing documents of a receipt
   * @param downloads file download answers
   */
  public ReceiptDocumentController(
      CashReceiptService receipts, BatchPrintService printing, FileDownloads downloads) {
    this.receipts = receipts;
    this.printing = printing;
    this.downloads = downloads;
  }

  /**
   * The documents of a receipt.
   *
   * @param id receipt
   * @return documents, oldest first
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW)
  public List<DocumentResponse> list(@PathVariable Long id) {
    return printing.documents(receipts.get(id)).stream().map(DocumentResponse::from).toList();
  }

  /**
   * Opens a document of a receipt.
   *
   * @param id receipt
   * @param fileId document
   * @param request HTTP request (client address of the link audit)
   * @return redirect or content
   */
  @GetMapping("/{fileId}")
  @PreAuthorize(CashAccess.VIEW)
  public ResponseEntity<byte[]> open(
      @PathVariable Long id, @PathVariable Long fileId, HttpServletRequest request) {
    Receipt receipt = receipts.get(id);
    boolean owned = printing.documents(receipt).stream().anyMatch(f -> f.getId().equals(fileId));
    if (!owned) {
      throw new ResourceNotFoundException("Receipt document", fileId);
    }
    return downloads.respond(FileDownload.stored(fileId), request);
  }

  /**
   * A document of a receipt.
   *
   * @param id stored file
   * @param fileName file name
   * @param createdAt kept at
   * @param createdBy printed by
   */
  public record DocumentResponse(Long id, String fileName, Instant createdAt, String createdBy) {

    static DocumentResponse from(StoredFile f) {
      return new DocumentResponse(f.getId(), f.getFileName(), f.getCreatedAt(), f.getCreatedBy());
    }
  }
}
