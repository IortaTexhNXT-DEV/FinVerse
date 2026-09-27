package com.iortatechnxt.brokerverse.eb.franchise.api;

import com.iortatechnxt.brokerverse.eb.document.api.EbUploads;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.franchise.api.dto.FranchiseDtos.FranchiseResponse;
import com.iortatechnxt.brokerverse.eb.franchise.api.dto.FranchiseDtos.InsurersRequest;
import com.iortatechnxt.brokerverse.eb.franchise.service.FranchiseService;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Franchise requests of a cycle (FR-EB-032, 033): request, record the decision, advise. */
@RestController
@RequestMapping("/api/v1/eb")
@Transactional
public class FranchiseController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MARKET = "hasAuthority('EB_MARKET')";

  private final FranchiseService franchises;
  private final EbParties parties;

  /**
   * Creates the controller.
   *
   * @param franchises franchise requests
   * @param parties insurer names
   */
  public FranchiseController(FranchiseService franchises, EbParties parties) {
    this.franchises = franchises;
    this.parties = parties;
  }

  /**
   * The franchise requests of a programme.
   *
   * @param id programme
   * @param companyId company
   * @return requests, latest first
   */
  @GetMapping("/programmes/{id}/franchise")
  @PreAuthorize(VIEW)
  public List<FranchiseResponse> list(@PathVariable Long id, @RequestParam Long companyId) {
    return franchises.ofProgramme(companyId, id).stream().map(f -> map(companyId, f)).toList();
  }

  /**
   * Requests the franchise from insurers.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param request insurers
   * @return the requests sent
   */
  @PostMapping("/cycles/{cycleId}/franchise")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public List<FranchiseResponse> request(
      @PathVariable Long cycleId, @RequestParam Long companyId, @RequestBody InsurersRequest request) {
    return franchises.request(companyId, cycleId, request.insurerCodes()).stream()
        .map(f -> map(companyId, f))
        .toList();
  }

  /**
   * Records the insurer's decision with its reply.
   *
   * @param requestId request
   * @param companyId company
   * @param approve true for an approval
   * @param decidedOn date of the reply
   * @param reasonCode rejection reason
   * @param remarks remarks
   * @param file the insurer's reply
   * @return the request
   */
  @PostMapping(value = "/franchise/{requestId}/decision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(MARKET)
  @SuppressWarnings("java:S107") // one parameter per form field
  public FranchiseResponse decide(
      @PathVariable Long requestId,
      @RequestParam Long companyId,
      @RequestParam boolean approve,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate decidedOn,
      @RequestParam(required = false) String reasonCode,
      @RequestParam(required = false) String remarks,
      @RequestParam(required = false) MultipartFile file) {
    return map(
        companyId,
        franchises.decide(
            companyId,
            requestId,
            new FranchiseService.DecisionInput(
                approve, decidedOn, reasonCode, remarks, EbUploads.file(file))));
  }

  /**
   * Advises the client of the decision.
   *
   * @param requestId request
   * @param companyId company
   * @return the request
   */
  @PostMapping("/franchise/{requestId}/advise")
  @PreAuthorize(MARKET)
  public FranchiseResponse advise(@PathVariable Long requestId, @RequestParam Long companyId) {
    return map(companyId, franchises.advise(companyId, requestId));
  }

  private FranchiseResponse map(Long companyId, EbFranchiseRequest f) {
    return FranchiseResponse.from(f, parties.insurerName(companyId, f.getInsurerCode()));
  }
}
