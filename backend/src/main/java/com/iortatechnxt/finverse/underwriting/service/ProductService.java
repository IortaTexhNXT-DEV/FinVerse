package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.dimension.domain.DimensionType;
import com.iortatechnxt.finverse.dimension.service.DimensionService;
import com.iortatechnxt.finverse.underwriting.api.dto.ProductRequest;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.ProductRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Product master maintenance under maker-checker control, and lookups for transactions. */
@Service
@Transactional
public class ProductService {

  static final String ENTITY = "Product";

  private final ProductRepository products;
  private final DimensionService dimensions;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param products repository
   * @param dimensions dimension validation (line of business)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ProductService(
      ProductRepository products,
      DimensionService dimensions,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.products = products;
    this.dimensions = dimensions;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists a company's products.
   *
   * @param companyId company
   * @return products by code
   */
  @Transactional(readOnly = true)
  public List<Product> list(Long companyId) {
    return products.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Gets a product.
   *
   * @param id id
   * @return product
   */
  @Transactional(readOnly = true)
  public Product get(Long id) {
    return products.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Returns an authorized product of the company, or fails.
   *
   * @param companyId company
   * @param id product id
   * @return product
   */
  @Transactional(readOnly = true)
  public Product requireActive(Long companyId, Long id) {
    Product product = get(id);
    if (!product.getCompanyId().equals(companyId)) {
      throw new ResourceNotFoundException(ENTITY, id);
    }
    if (!product.isActive()) {
      throw new BusinessRuleException(
          "INACTIVE_PRODUCT", "Product " + product.getCode() + " is not authorized");
    }
    return product;
  }

  /**
   * Creates a product (pending authorization).
   *
   * @param r request
   * @return product
   */
  public Product create(ProductRequest r) {
    if (products.existsByCompanyIdAndCode(r.companyId(), r.code())) {
      throw new DuplicateResourceException(ENTITY, r.code());
    }
    requireBusinessLine(r);
    Product saved = products.save(new Product(r.companyId(), r.code(), r.toTerms()));
    audit.record(ENTITY, saved.getCode(), AuditAction.CREATE, "Created product " + r.name());
    return saved;
  }

  /**
   * Updates a product; it returns to pending authorization.
   *
   * @param id id
   * @param r request
   * @return product
   */
  public Product update(Long id, ProductRequest r) {
    Product product = get(id);
    requireBusinessLine(r);
    product.update(r.toTerms());
    audit.record(ENTITY, product.getCode(), AuditAction.UPDATE, "Updated product");
    return product;
  }

  /**
   * Authorizes a product (checker).
   *
   * @param id id
   * @return product
   */
  public Product authorize(Long id) {
    Product product = get(id);
    product.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, product.getCode(), AuditAction.AUTHORIZE, "Authorized product");
    return product;
  }

  private void requireBusinessLine(ProductRequest r) {
    dimensions.validateOptional(r.companyId(), DimensionType.BUSINESS_LINE, r.businessLine());
  }
}
