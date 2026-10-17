package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.MatrixAttributes;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds or changes a Product Matrix record with the attributes of BDOI's FRS (FRPM.003.01,
 * FRPM.003.02 and Annex A) in one transaction: the package description, Incentive Eligible with the
 * incentive amount or commission rate, Policy Type and Insured's Name; the record waits for
 * approval as every catalog record, and the approver the maker names is told. With the setting
 * {@value #DIRECT} off, new packages come only from package requests.
 */
@Service
@Transactional
public class ProductDescriptions {

  /** Setting: direct maintenance of Product Matrix records. */
  public static final String DIRECT = "PM_DIRECT_MAINTENANCE";

  private static final String APPROVER_PERMISSION = "PRODUCT_AUTHORIZE";

  private final ProductCatalogService catalog;
  private final LovService lov;
  private final UserDirectory directory;
  private final NoticeDelivery delivery;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param catalog products
   * @param lov lists of values (policy type)
   * @param directory approvers
   * @param delivery notice to the approver
   * @param currentUser maker
   * @param parameters business parameters (direct maintenance)
   * @param clock clock
   */
  public ProductDescriptions(
      ProductCatalogService catalog,
      LovService lov,
      UserDirectory directory,
      NoticeDelivery delivery,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock) {
    this.catalog = catalog;
    this.lov = lov;
    this.directory = directory;
    this.delivery = delivery;
    this.currentUser = currentUser;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Adds a product with its Product Matrix attributes, pending authorization.
   *
   * @param code risk code
   * @param details attributes
   * @param extras description, Annex A attributes and approver
   * @return product
   */
  public RiskProduct create(String code, ProductDetails details, Extras extras) {
    if (details.packaged() && !Boolean.parseBoolean(parameters.text(DIRECT, "true").strip())) {
      throw new BusinessRuleException(
          "PM_DIRECT_MAINTENANCE_OFF", "New packages are created through package requests");
    }
    check(extras);
    RiskProduct saved = catalog.createProduct(code, details);
    apply(saved, extras);
    return saved;
  }

  /**
   * Changes a product and its Product Matrix attributes, pending authorization.
   *
   * @param code risk code
   * @param details attributes
   * @param extras description, Annex A attributes and approver
   * @return product
   */
  public RiskProduct update(String code, ProductDetails details, Extras extras) {
    check(extras);
    RiskProduct product = catalog.updateProduct(code, details);
    apply(product, extras);
    return product;
  }

  private void check(Extras e) {
    MatrixAttributes m = e.matrix();
    IncentiveRules.check(m.incentiveEligible(), m.incentiveAmount(), m.incentiveRate());
    if (m.policyType() != null && !m.policyType().isBlank()) {
      lov.requireValid("PKG_POLICY_TYPE", m.policyType().strip(), BusinessClock.today(clock));
    }
    String approver = e.approver();
    if (approver != null
        && !approver.isBlank()
        && (CurrentUser.sameUser(approver, currentUser.username())
            || !directory.usersWithPermission(APPROVER_PERMISSION).contains(approver.strip()))) {
      throw new BusinessRuleException(
          "PM_APPROVER", "Select an approver, other than you, who may approve products");
    }
  }

  private void apply(RiskProduct product, Extras e) {
    product.describe(e.description());
    product.matrixAttributes(e.matrix());
    if (e.approver() != null && !e.approver().isBlank()) {
      delivery.toUser(
          e.approver().strip(),
          new Notice(
              "Product Matrix record for approval: " + product.getCode(),
              directory.displayName(currentUser.username())
                  + " sent "
                  + product.getName()
                  + " for your approval.",
              "/catalog/products/" + product.getCode(),
              "Product",
              product.getCode()),
          "PM_PRODUCT_APPROVAL",
          true);
    }
  }

  /**
   * What the Product Matrix record adds to the catalog product.
   *
   * @param description package description
   * @param matrix Annex A attributes
   * @param approver approver to tell, may be null
   */
  public record Extras(String description, MatrixAttributes matrix, String approver) {}
}
