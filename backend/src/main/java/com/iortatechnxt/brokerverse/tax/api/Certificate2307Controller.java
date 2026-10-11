package com.iortatechnxt.brokerverse.tax.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.CertificateBatchRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.CertificateBatchResponse;
import com.iortatechnxt.brokerverse.tax.api.dto.CertificateResponse;
import com.iortatechnxt.brokerverse.tax.service.Certificate2307Service;
import com.iortatechnxt.brokerverse.tax.service.Certificate2307Service.BatchResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** BIR Form 2307 certificates: batch generation, register, cancellation and PDF download. */
@RestController
@RequestMapping("/api/v1/tax/2307")
public class Certificate2307Controller {

  private final Certificate2307Service service;

  /**
   * Creates the controller.
   *
   * @param service certificates
   */
  public Certificate2307Controller(Certificate2307Service service) {
    this.service = service;
  }

  /**
   * Certificate register of a year.
   *
   * @param companyId company
   * @param year year
   * @return certificates
   */
  @GetMapping("/certificates")
  @PreAuthorize(TaxAccess.VIEW)
  public List<CertificateResponse> register(@RequestParam Long companyId, @RequestParam int year) {
    return service.register(companyId, year).stream().map(CertificateResponse::from).toList();
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @return batches
   */
  @GetMapping("/batches")
  @PreAuthorize(TaxAccess.VIEW)
  public List<CertificateBatchResponse> batches(@RequestParam Long companyId) {
    return service.batches(companyId).stream()
        .map(b -> CertificateBatchResponse.from(b, List.of()))
        .toList();
  }

  /**
   * Issues the certificates of a quarter.
   *
   * @param request company and quarter
   * @return batch
   */
  @PostMapping("/batches")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(TaxAccess.MANAGE)
  public CertificateBatchResponse generate(@Valid @RequestBody CertificateBatchRequest request) {
    BatchResult result = service.generate(request.companyId(), request.year(), request.quarter());
    return CertificateBatchResponse.from(result.batch(), result.skippedPayees());
  }

  /**
   * Cancels a certificate.
   *
   * @param id id
   * @param request reason
   * @return certificate
   */
  @PostMapping("/certificates/{id}/cancel")
  @PreAuthorize(TaxAccess.MANAGE)
  public CertificateResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return CertificateResponse.from(service.cancel(id, request.reason()));
  }

  /**
   * PDF of one certificate.
   *
   * @param id id
   * @return PDF
   */
  @GetMapping("/certificates/{id}/pdf")
  @PreAuthorize(TaxAccess.VIEW)
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    return TaxAccess.file(
        "BIR2307-" + service.get(id).getCertificateNo() + ".pdf",
        MediaType.APPLICATION_PDF_VALUE,
        service.pdf(id));
  }

  /**
   * PDF of all issued certificates of a batch.
   *
   * @param id batch id
   * @return PDF
   */
  @GetMapping("/batches/{id}/pdf")
  @PreAuthorize(TaxAccess.VIEW)
  public ResponseEntity<byte[]> batchPdf(@PathVariable Long id) {
    return TaxAccess.file(
        "BIR2307-batch-" + id + ".pdf", MediaType.APPLICATION_PDF_VALUE, service.batchPdf(id));
  }
}
