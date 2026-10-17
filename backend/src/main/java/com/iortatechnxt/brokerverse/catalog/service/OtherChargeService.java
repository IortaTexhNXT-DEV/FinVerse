package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Scope;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Terms;
import com.iortatechnxt.brokerverse.catalog.domain.OtherChargeRepository;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Other charges billed with the premium (BDOI inputs TX-Q04, template PM-04 Charges): the
 * maintenance of the effective-dated rows (maker-checker; authorized on the catalog records) and
 * the charges that apply to a product on a date for the premium calculator. Nothing is billed while
 * the parameter {@code OTHER_CHARGES_ENABLED} is off, so BDOI can confirm the charges before they
 * reach a quotation or an invoice.
 */
@Service
@Transactional
public class OtherChargeService {

  /** Parameter that switches the billing of the other charges on. */
  public static final String ENABLED = "OTHER_CHARGES_ENABLED";

  private final OtherChargeRepository charges;
  private final ProductCatalogService products;
  private final ChartOfAccountsService chart;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param charges other charges
   * @param products products and lines
   * @param chart chart of accounts (GL account check)
   * @param parameters the billing switch
   * @param audit audit trail
   */
  public OtherChargeService(
      OtherChargeRepository charges,
      ProductCatalogService products,
      ChartOfAccountsService chart,
      SystemParameterService parameters,
      AuditTrailService audit) {
    this.charges = charges;
    this.products = products;
    this.chart = chart;
    this.parameters = parameters;
    this.audit = audit;
  }

  /**
   * Every charge row.
   *
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<OtherCharge> list() {
    return charges.findAllByOrderByChargeCodeAscLineCodeAscProductCodeAscEffectiveFromDesc();
  }

  /**
   * Whether the other charges are billed.
   *
   * @return the value of {@code OTHER_CHARGES_ENABLED}
   */
  @Transactional(readOnly = true)
  public boolean enabled() {
    return Boolean.parseBoolean(parameters.text(ENABLED, "false"));
  }

  /**
   * Adds a charge row, pending authorization.
   *
   * @param companyId company whose chart of accounts holds the GL account
   * @param chargeCode code of the charge
   * @param scope line and product, both blank for every product
   * @param terms name, basis, value, VAT treatment, GL account and dates
   * @return row
   */
  public OtherCharge create(Long companyId, String chargeCode, Scope scope, Terms terms) {
    if (scope.lineCode() != null) {
      products.requireLine(scope.lineCode());
    }
    if (scope.productCode() != null) {
      products.requireProduct(scope.productCode());
    }
    requireAccount(companyId, terms.glAccountCode());
    OtherCharge saved = charges.save(new OtherCharge(chargeCode, scope, terms));
    audit.record(
        CatalogKind.OTHER_CHARGE.label(),
        saved.catalogReference(),
        AuditAction.CREATE,
        saved.catalogDescription());
    return saved;
  }

  /**
   * Changes a charge row; it must be authorized again.
   *
   * @param companyId company whose chart of accounts holds the GL account
   * @param id row
   * @param terms new terms
   * @return row
   */
  public OtherCharge update(Long companyId, Long id, Terms terms) {
    OtherCharge charge =
        charges
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.OTHER_CHARGE.label(), id));
    requireAccount(companyId, terms.glAccountCode());
    charge.update(terms);
    audit.record(
        CatalogKind.OTHER_CHARGE.label(),
        charge.catalogReference(),
        AuditAction.UPDATE,
        charge.catalogDescription());
    return charge;
  }

  /**
   * The charges billed with a product on a date: for each charge code the most specific row in
   * force (product, then line, then every product). Empty while the billing switch is off.
   *
   * @param productCode product
   * @param lineCode line of the product
   * @param date rating date
   * @return charges in force, by code
   */
  @Transactional(readOnly = true)
  public List<OtherCharge> applicable(String productCode, String lineCode, LocalDate date) {
    if (!enabled()) {
      return List.of();
    }
    return applicable(list(), productCode, lineCode, date);
  }

  /**
   * Picks the charges in force for a product from a list of rows.
   *
   * @param rows charge rows
   * @param productCode product
   * @param lineCode line
   * @param date rating date
   * @return the most specific row in force per charge code
   */
  static List<OtherCharge> applicable(
      List<OtherCharge> rows, String productCode, String lineCode, LocalDate date) {
    Map<String, OtherCharge> byCode = new LinkedHashMap<>();
    rows.stream()
        .filter(r -> r.isEffectiveOn(date) && r.appliesTo(productCode, lineCode))
        .sorted(
            Comparator.comparing(OtherCharge::getChargeCode)
                .thenComparing(Comparator.comparingInt(OtherCharge::specificity).reversed())
                .thenComparing(OtherCharge::getEffectiveFrom, Comparator.reverseOrder()))
        .forEach(r -> byCode.putIfAbsent(r.getChargeCode(), r));
    return List.copyOf(byCode.values());
  }

  private void requireAccount(Long companyId, String code) {
    GlAccount account = chart.getByCode(companyId, Objects.requireNonNull(code));
    if (!account.isPostable()) {
      throw new BusinessRuleException(
          "CHARGE_ACCOUNT_NOT_POSTABLE",
          "GL account " + code + " of the charge must be a postable account");
    }
  }
}
