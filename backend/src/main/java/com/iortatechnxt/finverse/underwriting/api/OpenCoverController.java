package com.iortatechnxt.finverse.underwriting.api;

import com.iortatechnxt.finverse.underwriting.api.dto.CertificateRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.OpenCoverRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.OpenCoverResponse;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicyResponse;
import com.iortatechnxt.finverse.underwriting.service.OpenCoverService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for marine open covers and the certificates declared under them. */
@RestController
@RequestMapping("/api/v1/underwriting/open-covers")
public class OpenCoverController {

  private static final String VIEW = "hasAuthority('POLICY_VIEW')";
  private static final String MAINTAIN = "hasAuthority('POLICY_MAINTAIN')";

  private final OpenCoverService service;

  /**
   * Creates the controller.
   *
   * @param service open cover service
   */
  public OpenCoverController(OpenCoverService service) {
    this.service = service;
  }

  /**
   * Lists open covers.
   *
   * @param companyId company
   * @return covers
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<OpenCoverResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(OpenCoverResponse::from).toList();
  }

  /**
   * Gets an open cover.
   *
   * @param id id
   * @return cover
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public OpenCoverResponse get(@PathVariable Long id) {
    return OpenCoverResponse.from(service.get(id));
  }

  /**
   * Certificates of an open cover.
   *
   * @param id id
   * @return certificates with shipment details
   */
  @GetMapping("/{id}/certificates")
  @PreAuthorize(VIEW)
  public List<PolicyResponse> certificates(@PathVariable Long id) {
    return service.certificates(id).stream().map(PolicyResponse::withRisks).toList();
  }

  /**
   * Creates an open cover.
   *
   * @param request request
   * @return cover
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public OpenCoverResponse create(@Valid @RequestBody OpenCoverRequest request) {
    return OpenCoverResponse.from(service.create(request));
  }

  /**
   * Authorizes an open cover.
   *
   * @param id id
   * @return cover
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('POLICY_AUTHORIZE')")
  public OpenCoverResponse authorize(@PathVariable Long id) {
    return OpenCoverResponse.from(service.authorize(id));
  }

  /**
   * Declares a shipment (creates a draft certificate).
   *
   * @param id open cover
   * @param request declaration
   * @return draft certificate
   */
  @PostMapping("/{id}/certificates")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public PolicyResponse declare(
      @PathVariable Long id, @Valid @RequestBody CertificateRequest request) {
    return PolicyResponse.withRisks(service.issueCertificate(id, request));
  }
}
