package com.iortatechnxt.finverse.party.api;

import com.iortatechnxt.finverse.party.api.dto.PartyRequest;
import com.iortatechnxt.finverse.party.api.dto.PartyResponse;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for business partners. */
@RestController
@RequestMapping("/api/v1/parties")
public class PartyController {

  private final PartyService service;

  /**
   * Creates the controller.
   *
   * @param service party service
   */
  public PartyController(PartyService service) {
    this.service = service;
  }

  /**
   * Lists or searches parties.
   *
   * @param companyId company
   * @param types optional type filter
   * @param q optional search term
   * @return parties
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<PartyResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) Set<PartyType> types,
      @RequestParam(required = false) String q) {
    return service.search(companyId, types == null ? Set.of() : types, q).stream()
        .map(PartyResponse::from)
        .toList();
  }

  /**
   * Gets a party.
   *
   * @param id id
   * @return party
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public PartyResponse get(@PathVariable Long id) {
    return PartyResponse.from(service.get(id));
  }

  /**
   * Creates a party.
   *
   * @param request request
   * @return party
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public PartyResponse create(@Valid @RequestBody PartyRequest request) {
    return PartyResponse.from(service.create(request));
  }

  /**
   * Updates a party.
   *
   * @param id id
   * @param request request
   * @return party
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public PartyResponse update(@PathVariable Long id, @Valid @RequestBody PartyRequest request) {
    return PartyResponse.from(service.update(id, request));
  }

  /**
   * Authorizes a party.
   *
   * @param id id
   * @return party
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public PartyResponse authorize(@PathVariable Long id) {
    return PartyResponse.from(service.authorize(id));
  }
}
