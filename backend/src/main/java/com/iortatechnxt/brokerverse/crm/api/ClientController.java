package com.iortatechnxt.brokerverse.crm.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientListItem;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientRequest;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.DuplicateMatchResponse;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.ClientCompleteness;
import com.iortatechnxt.brokerverse.crm.service.ClientSearch;
import com.iortatechnxt.brokerverse.crm.service.ClientSearchService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.DuplicateCheckService;
import com.iortatechnxt.brokerverse.crm.service.DuplicateProbe;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client master (BRNB.029/030/032/046/048/049): search, details, create and update, and duplicate
 * candidates for live warnings while the user types.
 */
@RestController
@RequestMapping("/api/v1/crm/clients")
public class ClientController {

  private static final String VIEW = "hasAuthority('CLIENT_VIEW')";
  private static final String MAINTAIN = "hasAuthority('CLIENT_MAINTAIN')";
  private static final int MAX_PAGE_SIZE = 100;

  private final ClientService clients;
  private final ClientSearchService search;
  private final DuplicateCheckService duplicates;
  private final ClientCompleteness completeness;

  /**
   * Creates the controller.
   *
   * @param clients client service
   * @param search client search
   * @param duplicates duplicate detection
   * @param completeness completeness rules
   */
  public ClientController(
      ClientService clients,
      ClientSearchService search,
      DuplicateCheckService duplicates,
      ClientCompleteness completeness) {
    this.clients = clients;
    this.search = search;
    this.duplicates = duplicates;
    this.completeness = completeness;
  }

  /**
   * Multi-criteria search (BRNB.046).
   *
   * @param f criteria
   * @param page page
   * @param size page size
   * @return clients by name
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<ClientListItem> search(
      @ModelAttribute SearchParams f,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    ClientSearch criteria =
        new ClientSearch(
            f.companyId(),
            f.code(),
            f.name(),
            f.tin(),
            f.idNumber(),
            f.email(),
            f.mobile(),
            f.status(),
            f.kycStatus(),
            f.marketSegment(),
            f.bankClient(),
            f.clientType(),
            Boolean.TRUE.equals(f.kycDue()));
    return PageResponse.of(
        search.search(
            criteria,
            PageRequest.of(
                page, Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("displayName"))),
        ClientListItem::from);
  }

  /**
   * Complete details of a client.
   *
   * @param id client
   * @return details
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public ClientResponse get(@PathVariable Long id) {
    return response(clients.get(id));
  }

  /**
   * Creates a prospect (save as prospect, BRNB.029); blocked on a hard duplicate (BRNB.032).
   *
   * @param request client
   * @return the prospect
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public ClientResponse create(@Valid @RequestBody ClientRequest request) {
    if (request.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company of the client");
    }
    return response(clients.create(request.companyId(), request.details(), request.profile()));
  }

  /**
   * Updates a client.
   *
   * @param id client
   * @param request new data
   * @return the client
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
    return response(clients.update(id, request.details(), request.profile()));
  }

  /**
   * Existing clients matching entered data on any duplicate key (live warnings, BRNB.032).
   *
   * @param p entered data
   * @return matches, hard matches first
   */
  @GetMapping("/duplicates")
  @PreAuthorize(VIEW)
  public List<DuplicateMatchResponse> duplicates(@ModelAttribute DuplicateParams p) {
    DuplicateProbe probe =
        new DuplicateProbe(
            p.clientType(),
            p.tin(),
            p.idType(),
            p.idNumber(),
            p.email(),
            p.mobile(),
            p.lastName(),
            p.firstName(),
            p.birthDate(),
            p.corporateName());
    return duplicates.candidates(p.companyId(), probe, p.excludeId()).stream()
        .map(DuplicateMatchResponse::from)
        .toList();
  }

  private ClientResponse response(Client c) {
    return ClientResponse.from(c, completeness.missing(c));
  }

  /**
   * Search criteria.
   *
   * @param companyId company
   * @param code code prefix
   * @param name name contains
   * @param tin TIN
   * @param idNumber ID number
   * @param email e-mail
   * @param mobile mobile
   * @param status status
   * @param kycStatus KYC status
   * @param marketSegment segment
   * @param bankClient bank client flag
   * @param clientType type
   * @param kycDue KYC due or overdue only
   */
  public record SearchParams(
      Long companyId,
      String code,
      String name,
      String tin,
      String idNumber,
      String email,
      String mobile,
      ClientStatus status,
      KycStatus kycStatus,
      String marketSegment,
      Boolean bankClient,
      ClientType clientType,
      Boolean kycDue) {}

  /**
   * Entered data to check for duplicates.
   *
   * @param companyId company
   * @param excludeId client being edited
   * @param clientType type
   * @param tin TIN
   * @param idType ID type
   * @param idNumber ID number
   * @param email e-mail
   * @param mobile mobile
   * @param lastName last name
   * @param firstName first name
   * @param birthDate birth date
   * @param corporateName corporate name
   */
  public record DuplicateParams(
      Long companyId,
      Long excludeId,
      ClientType clientType,
      String tin,
      String idType,
      String idNumber,
      String email,
      String mobile,
      String lastName,
      String firstName,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
      String corporateName) {}
}
