package com.iortatechnxt.finverse.fixedasset.api;

import com.iortatechnxt.finverse.fixedasset.api.dto.AssetCategoryRequest;
import com.iortatechnxt.finverse.fixedasset.api.dto.AssetCategoryResponse;
import com.iortatechnxt.finverse.fixedasset.service.AssetCategoryService;
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

/** REST API for fixed asset categories. */
@RestController
@RequestMapping("/api/v1/assets/categories")
public class AssetCategoryController {

  private final AssetCategoryService service;

  /**
   * Creates the controller.
   *
   * @param service category service
   */
  public AssetCategoryController(AssetCategoryService service) {
    this.service = service;
  }

  /**
   * Lists categories.
   *
   * @param companyId company
   * @return categories
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<AssetCategoryResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(AssetCategoryResponse::from).toList();
  }

  /**
   * Creates a category.
   *
   * @param request request
   * @return category
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public AssetCategoryResponse create(@Valid @RequestBody AssetCategoryRequest request) {
    return AssetCategoryResponse.from(service.create(request));
  }

  /**
   * Updates a category.
   *
   * @param id id
   * @param request request
   * @return category
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public AssetCategoryResponse update(
      @PathVariable Long id, @Valid @RequestBody AssetCategoryRequest request) {
    return AssetCategoryResponse.from(service.update(id, request));
  }

  /**
   * Authorizes a category.
   *
   * @param id id
   * @return category
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public AssetCategoryResponse authorize(@PathVariable Long id) {
    return AssetCategoryResponse.from(service.authorize(id));
  }
}
