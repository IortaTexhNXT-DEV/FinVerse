package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.CoverTypeRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.CoverTypeResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.FieldRuleResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.LineRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.LineResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.ProductDetailResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.ProductRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.ProductResponse;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService.ProductFilter;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
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

/** Product lines, cover types and BDOI risk products (BRNB.001). */
@RestController
@RequestMapping("/api/v1/catalog")
public class ProductCatalogController {

  private final ProductCatalogService catalog;
  private final ProductRuleService rules;

  /**
   * Creates the controller.
   *
   * @param catalog product catalog
   * @param rules field and document rules
   */
  public ProductCatalogController(ProductCatalogService catalog, ProductRuleService rules) {
    this.catalog = catalog;
    this.rules = rules;
  }

  /**
   * Product lines.
   *
   * @return lines
   */
  @GetMapping("/lines")
  @PreAuthorize(CatalogAccess.READ)
  public List<LineResponse> lines() {
    return catalog.lines().stream().map(LineResponse::from).toList();
  }

  /**
   * Adds a product line.
   *
   * @param request line
   * @return line
   */
  @PostMapping("/lines")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public LineResponse createLine(@Valid @RequestBody LineRequest request) {
    return LineResponse.from(catalog.createLine(request.code(), request.details()));
  }

  /**
   * Changes a product line.
   *
   * @param code line
   * @param request attributes
   * @return line
   */
  @PutMapping("/lines/{code}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public LineResponse updateLine(
      @PathVariable String code, @Valid @RequestBody LineRequest request) {
    return LineResponse.from(catalog.updateLine(code, request.details()));
  }

  /**
   * Cover types of every line.
   *
   * @return cover types
   */
  @GetMapping("/cover-types")
  @PreAuthorize(CatalogAccess.READ)
  public List<CoverTypeResponse> coverTypes() {
    return catalog.coverTypes().stream().map(CoverTypeResponse::from).toList();
  }

  /**
   * Adds a cover type.
   *
   * @param request cover type
   * @return cover type
   */
  @PostMapping("/cover-types")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public CoverTypeResponse createCoverType(@Valid @RequestBody CoverTypeRequest request) {
    return CoverTypeResponse.from(
        catalog.createCoverType(
            request.lineCode(), request.code(), request.name().trim(), request.sortOrder()));
  }

  /**
   * Renames a cover type.
   *
   * @param id cover type
   * @param request name and order
   * @return cover type
   */
  @PutMapping("/cover-types/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public CoverTypeResponse updateCoverType(
      @PathVariable Long id, @Valid @RequestBody CoverTypeRequest request) {
    return CoverTypeResponse.from(
        catalog.updateCoverType(id, request.name().trim(), request.sortOrder()));
  }

  /**
   * Products matching the filters.
   *
   * @param line product line
   * @param packaged package flag
   * @param segment market segment
   * @param q risk code or name
   * @param activeOnly only usable products
   * @return products
   */
  @GetMapping("/products")
  @PreAuthorize(CatalogAccess.READ)
  public List<ProductResponse> products(
      @RequestParam(required = false) String line,
      @RequestParam(required = false) Boolean packaged,
      @RequestParam(required = false) String segment,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "false") boolean activeOnly) {
    return catalog.products(new ProductFilter(line, packaged, segment, q, activeOnly)).stream()
        .map(ProductResponse::from)
        .toList();
  }

  /**
   * A product with its line, minimum fields and mandatory documents.
   *
   * @param code risk code
   * @return detail
   */
  @GetMapping("/products/{code}")
  @PreAuthorize(CatalogAccess.READ)
  public ProductDetailResponse product(@PathVariable String code) {
    RiskProduct product = catalog.requireProduct(code);
    ProductLine line = catalog.requireLine(product.getLineCode());
    return new ProductDetailResponse(
        ProductResponse.from(product),
        line.getName(),
        line.getRiskItemKind(),
        line.getRatingMethod(),
        rules.effectiveFieldRules(product).stream().map(FieldRuleResponse::from).toList(),
        List.copyOf(rules.requiredDocuments(product)));
  }

  /**
   * Adds a product.
   *
   * @param request product
   * @return product
   */
  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
    return ProductResponse.from(catalog.createProduct(request.code(), request.details()));
  }

  /**
   * Changes a product.
   *
   * @param code risk code
   * @param request attributes
   * @return product
   */
  @PutMapping("/products/{code}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public ProductResponse updateProduct(
      @PathVariable String code, @Valid @RequestBody ProductRequest request) {
    return ProductResponse.from(catalog.updateProduct(code, request.details()));
  }
}
