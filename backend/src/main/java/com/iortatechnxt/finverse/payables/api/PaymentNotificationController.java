package com.iortatechnxt.finverse.payables.api;

import com.iortatechnxt.finverse.payables.domain.NotificationFormat;
import com.iortatechnxt.finverse.payables.service.NotificationRecord;
import com.iortatechnxt.finverse.payables.service.PaymentNotificationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Payment notification file to the bank (FIN-BRS-PAYNOTIFY). */
@RestController
@RequestMapping("/api/v1/payables/payment-notifications")
public class PaymentNotificationController {

  private final PaymentNotificationService service;

  /**
   * Creates the controller.
   *
   * @param service notification service
   */
  public PaymentNotificationController(PaymentNotificationService service) {
    this.service = service;
  }

  /**
   * Previews the payments that the file would contain.
   *
   * @param bankAccountId bank account
   * @param from from date
   * @param to to date
   * @param includeCheques include cheques and PDCs
   * @return records
   */
  @GetMapping
  @PreAuthorize(PayablesAccess.MAINTAIN_OR_AUTHORIZE)
  public List<NotificationRecord> preview(
      @RequestParam Long bankAccountId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "false") boolean includeCheques) {
    return service.records(bankAccountId, from, to, includeCheques);
  }

  /**
   * Generates and downloads the file (a POST: every generation is audited).
   *
   * @param bankAccountId bank account
   * @param from from date
   * @param to to date (file date)
   * @param includeCheques include cheques and PDCs
   * @param format layout override (default: the bank account's layout)
   * @return file
   */
  @PostMapping("/file")
  @PreAuthorize(PayablesAccess.MAINTAIN_OR_AUTHORIZE)
  public ResponseEntity<byte[]> file(
      @RequestParam Long bankAccountId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "false") boolean includeCheques,
      @RequestParam(required = false) NotificationFormat format) {
    var file = service.generate(bankAccountId, from, to, includeCheques, format);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.fileName()).build().toString())
        .contentType(MediaType.parseMediaType(file.contentType()))
        .body(file.content());
  }
}
