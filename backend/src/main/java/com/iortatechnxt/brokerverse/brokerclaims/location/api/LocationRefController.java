package com.iortatechnxt.brokerverse.brokerclaims.location.api;

import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.LocationRefRequest;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.LocationRefResponse;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
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

/**
 * Insurer location references (BRCLM.042; FR-CL-023): search and maintenance ({@code
 * BCL_LOCATION_REF_MAINTAIN}) and the references of a cover (Cover Lookup, claim locations).
 */
@RestController
@RequestMapping("/api/v1/broker-claims/location-refs")
public class LocationRefController {

  private static final int MAX_PAGE = 200;

  private final LocationRefService refs;

  /**
   * Creates the controller.
   *
   * @param refs references
   */
  public LocationRefController(LocationRefService refs) {
    this.refs = refs;
  }

  /**
   * References by ARN, insurer, reference or location key.
   *
   * @param companyId company
   * @param q text, may be empty
   * @param page page
   * @param size size
   * @return references
   */
  @GetMapping
  @PreAuthorize("hasAuthority('BCL_LOCATION_REF_MAINTAIN')")
  public PageResponse<LocationRefResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        refs.search(
            companyId, q, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE))),
        LocationRefResponse::from);
  }

  /**
   * Every reference of a cover, current and past.
   *
   * @param arn cover
   * @param companyId company
   * @return references
   */
  @GetMapping("/by-cover/{arn}")
  @PreAuthorize("hasAnyAuthority('BCL_VIEW', 'BCL_COVER_VIEW', 'BCL_LOCATION_REF_MAINTAIN')")
  public List<LocationRefResponse> ofCover(@PathVariable String arn, @RequestParam Long companyId) {
    return refs.ofCover(companyId, arn).stream().map(LocationRefResponse::from).toList();
  }

  /**
   * Records a reference; the open one of the location and insurer is end-dated.
   *
   * @param request reference
   * @return the new reference
   */
  @PostMapping
  @PreAuthorize("hasAuthority('BCL_LOCATION_REF_MAINTAIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public LocationRefResponse maintain(@Valid @RequestBody LocationRefRequest request) {
    return LocationRefResponse.from(refs.maintain(request.companyId(), request.toRef()));
  }
}
