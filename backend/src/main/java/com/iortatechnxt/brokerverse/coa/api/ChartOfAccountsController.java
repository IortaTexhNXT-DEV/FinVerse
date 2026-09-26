package com.iortatechnxt.brokerverse.coa.api;

import com.iortatechnxt.brokerverse.coa.api.dto.CoaNumberingRequest;
import com.iortatechnxt.brokerverse.coa.api.dto.CoaNumberingResponse;
import com.iortatechnxt.brokerverse.coa.api.dto.GlAccountRequest;
import com.iortatechnxt.brokerverse.coa.api.dto.GlAccountResponse;
import com.iortatechnxt.brokerverse.coa.api.dto.GlCategoryRequest;
import com.iortatechnxt.brokerverse.coa.api.dto.GlCategoryResponse;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.coa.service.CoaNumberingService;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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

/** REST API for the chart of accounts and GL categories. */
@RestController
@RequestMapping("/api/v1/coa")
public class ChartOfAccountsController {

  private static final String VIEW = "hasAuthority('MASTER_VIEW')";
  private static final String MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('MASTER_AUTHORIZE')";

  private final ChartOfAccountsService service;
  private final CoaNumberingService numbering;

  /**
   * Creates the controller.
   *
   * @param service chart of accounts service
   * @param numbering account numbering schemes
   */
  public ChartOfAccountsController(ChartOfAccountsService service, CoaNumberingService numbering) {
    this.service = service;
    this.numbering = numbering;
  }

  /**
   * Finds an account by its code or short code (journal line entry, FRBS 2.8.1).
   *
   * @param companyId company
   * @param key account code or short code
   * @return account
   */
  @GetMapping("/accounts/lookup")
  @PreAuthorize(VIEW)
  public GlAccountResponse lookup(@RequestParam Long companyId, @RequestParam String key) {
    return GlAccountResponse.from(service.lookup(companyId, key));
  }

  /**
   * The next system-generated code under a parent (FRBS 2.3.2).
   *
   * @param companyId company
   * @param parentCode parent account code
   * @return {"code": proposed code}
   */
  @GetMapping("/accounts/next-code")
  @PreAuthorize(VIEW)
  public Map<String, String> nextCode(
      @RequestParam Long companyId, @RequestParam String parentCode) {
    return Map.of("code", numbering.nextCode(companyId, parentCode));
  }

  /**
   * Numbering schemes of a company.
   *
   * @param companyId company
   * @return schemes
   */
  @GetMapping("/numbering")
  @PreAuthorize(VIEW)
  public List<CoaNumberingResponse> numbering(@RequestParam Long companyId) {
    return numbering.list(companyId).stream().map(CoaNumberingResponse::from).toList();
  }

  /**
   * Creates or changes the numbering scheme of a parent account.
   *
   * @param request scheme
   * @return scheme
   */
  @PutMapping("/numbering")
  @PreAuthorize(MAINTAIN)
  public CoaNumberingResponse saveNumbering(@Valid @RequestBody CoaNumberingRequest request) {
    return CoaNumberingResponse.from(
        numbering.save(
            request.companyId(),
            request.parentCode(),
            request.separator(),
            request.width(),
            request.active()));
  }

  /**
   * Lists or searches accounts.
   *
   * @param companyId company
   * @param q optional search term (code prefix, name or short code)
   * @return accounts
   */
  @GetMapping("/accounts")
  @PreAuthorize(VIEW)
  public List<GlAccountResponse> accounts(
      @RequestParam Long companyId, @RequestParam(required = false) String q) {
    var result = q == null || q.isBlank() ? service.list(companyId) : service.search(companyId, q);
    return result.stream().map(GlAccountResponse::from).toList();
  }

  /**
   * Gets an account.
   *
   * @param id id
   * @return account
   */
  @GetMapping("/accounts/{id}")
  @PreAuthorize(VIEW)
  public GlAccountResponse account(@PathVariable Long id) {
    return GlAccountResponse.from(service.get(id));
  }

  /**
   * Creates an account.
   *
   * @param request request
   * @return account
   */
  @PostMapping("/accounts")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public GlAccountResponse create(@Valid @RequestBody GlAccountRequest request) {
    return GlAccountResponse.from(service.create(request));
  }

  /**
   * Updates an account.
   *
   * @param id id
   * @param request request
   * @return account
   */
  @PutMapping("/accounts/{id}")
  @PreAuthorize(MAINTAIN)
  public GlAccountResponse update(
      @PathVariable Long id, @Valid @RequestBody GlAccountRequest request) {
    return GlAccountResponse.from(service.update(id, request));
  }

  /**
   * Authorizes an account.
   *
   * @param id id
   * @return account
   */
  @PostMapping("/accounts/{id}/authorize")
  @PreAuthorize(AUTHORIZE)
  public GlAccountResponse authorize(@PathVariable Long id) {
    return GlAccountResponse.from(service.authorize(id));
  }

  /**
   * Freezes an account.
   *
   * @param id id
   * @param request reason
   * @return account
   */
  @PostMapping("/accounts/{id}/freeze")
  @PreAuthorize(AUTHORIZE)
  public GlAccountResponse freeze(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return GlAccountResponse.from(service.freeze(id, request.reason()));
  }

  /**
   * Unfreezes an account.
   *
   * @param id id
   * @return account
   */
  @PostMapping("/accounts/{id}/unfreeze")
  @PreAuthorize(AUTHORIZE)
  public GlAccountResponse unfreeze(@PathVariable Long id) {
    return GlAccountResponse.from(service.unfreeze(id));
  }

  /**
   * Closes an account (GL Closure).
   *
   * @param id id
   * @return account
   */
  @PostMapping("/accounts/{id}/close")
  @PreAuthorize(AUTHORIZE)
  public GlAccountResponse close(@PathVariable Long id) {
    return GlAccountResponse.from(service.close(id));
  }

  /**
   * Lists GL categories.
   *
   * @return categories
   */
  @GetMapping("/categories")
  @PreAuthorize(VIEW)
  public List<GlCategoryResponse> categories() {
    return service.listCategories().stream().map(GlCategoryResponse::from).toList();
  }

  /**
   * Creates a GL category.
   *
   * @param request request
   * @return category
   */
  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public GlCategoryResponse createCategory(@Valid @RequestBody GlCategoryRequest request) {
    return GlCategoryResponse.from(service.createCategory(request));
  }
}
