package com.iortatechnxt.brokerverse.tax.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.ReceivedCertificateDtos.CertificateBody;
import com.iortatechnxt.brokerverse.tax.api.dto.ReceivedCertificateDtos.CertificateResponse;
import com.iortatechnxt.brokerverse.tax.service.ReceivedCertificateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The register of BIR 2307 certificates received from withholding agents (DIS 2.11.0-2.11.2):
 * search, one certificate, record (posts {@code TAX_CWT_CERT_RECEIVED}) and cancel (reverses it).
 * Disbursement users (CWT tagging) and tax users record certificates.
 */
@RestController
@RequestMapping("/api/v1/tax/received-certificates")
public class ReceivedCertificateController {

  private static final String VIEW = "hasAnyAuthority('TAX_VIEW', 'DISB_TAG')";
  private static final String RECORD = "hasAnyAuthority('TAX_MANAGE', 'DISB_TAG')";
  private static final int MAX_PAGE = 200;

  private final ReceivedCertificateService service;

  /**
   * Creates the controller.
   *
   * @param service register
   */
  public ReceivedCertificateController(ReceivedCertificateService service) {
    this.service = service;
  }

  /**
   * Searches the register.
   *
   * @param companyId company
   * @param status RECORDED or CANCELLED
   * @param q certificate or agent contains
   * @param page page
   * @param size size
   * @return certificates, newest first
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<CertificateResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(
        service.search(companyId, status, q, pageable), CertificateResponse::from);
  }

  /**
   * One certificate.
   *
   * @param id id
   * @return certificate
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public CertificateResponse get(@PathVariable Long id) {
    return CertificateResponse.from(service.get(id));
  }

  /**
   * Records a certificate received (DIS 2.11.1-2.11.2).
   *
   * @param companyId company
   * @param body certificate
   * @return certificate
   */
  @PostMapping
  @PreAuthorize(RECORD)
  public CertificateResponse record(
      @RequestParam Long companyId, @Valid @RequestBody CertificateBody body) {
    return CertificateResponse.from(
        service.record(companyId, body.facts(), body.certificateLines()));
  }

  /**
   * Cancels a certificate recorded in error.
   *
   * @param id certificate
   * @param body reason
   * @return certificate
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(TaxAccess.MANAGE)
  public CertificateResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
    return CertificateResponse.from(service.cancel(id, body.reason()));
  }
}
