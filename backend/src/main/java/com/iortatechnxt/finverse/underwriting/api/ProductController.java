package com.iortatechnxt.finverse.underwriting.api;

import com.iortatechnxt.finverse.underwriting.api.dto.ProductRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.ProductResponse;
import com.iortatechnxt.finverse.underwriting.service.ProductService;
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

/** REST API for insurance products (maker-checker master data). */
@RestController
@RequestMapping("/api/v1/underwriting/products")
public class ProductController {

  private final ProductService service;

  /**
   * Creates the controller.
   *
   * @param service product service
   */
  public ProductController(ProductService service) {
    this.service = service;
  }

  /**
   * Lists products.
   *
   * @param companyId company
   * @return products
   */
  @GetMapping
  @PreAuthorize("hasAuthority('POLICY_VIEW')")
  public List<ProductResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(ProductResponse::from).toList();
  }

  /**
   * Gets a product.
   *
   * @param id id
   * @return product
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('POLICY_VIEW')")
  public ProductResponse get(@PathVariable Long id) {
    return ProductResponse.from(service.get(id));
  }

  /**
   * Creates a product.
   *
   * @param request request
   * @return product
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('POLICY_MAINTAIN')")
  public ProductResponse create(@Valid @RequestBody ProductRequest request) {
    return ProductResponse.from(service.create(request));
  }

  /**
   * Updates a product.
   *
   * @param id id
   * @param request request
   * @return product
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('POLICY_MAINTAIN')")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return ProductResponse.from(service.update(id, request));
  }

  /**
   * Authorizes a product.
   *
   * @param id id
   * @return product
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('POLICY_AUTHORIZE')")
  public ProductResponse authorize(@PathVariable Long id) {
    return ProductResponse.from(service.authorize(id));
  }
}
