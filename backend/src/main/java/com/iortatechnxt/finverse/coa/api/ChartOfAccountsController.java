package com.iortatechnxt.finverse.coa.api;

import com.iortatechnxt.finverse.coa.api.dto.GlAccountRequest;
import com.iortatechnxt.finverse.coa.api.dto.GlAccountResponse;
import com.iortatechnxt.finverse.coa.api.dto.GlCategoryRequest;
import com.iortatechnxt.finverse.coa.api.dto.GlCategoryResponse;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import java.util.List;
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

  /**
   * Creates the controller.
   *
   * @param service chart of accounts service
   */
  public ChartOfAccountsController(ChartOfAccountsService service) {
    this.service = service;
  }

  /**
   * Lists or searches accounts.
   *
   * @param companyId company
   * @param q optional search term (code prefix or name)
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
