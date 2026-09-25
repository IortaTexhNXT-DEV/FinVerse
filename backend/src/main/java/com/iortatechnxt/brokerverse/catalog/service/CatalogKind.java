package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.CatalogRate;
import com.iortatechnxt.brokerverse.catalog.domain.Clause;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate;
import com.iortatechnxt.brokerverse.catalog.domain.CoverType;
import com.iortatechnxt.brokerverse.catalog.domain.Coverage;
import com.iortatechnxt.brokerverse.catalog.domain.DocumentRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRate;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import java.util.List;

/**
 * The kinds of catalog master records, with their entity, the label shown in the approval inbox and
 * audit trail, the screen where they are maintained and who maintains and authorises them. The
 * product areas of Product Maintenance (BRD-3, PMADD05) are maintained with PRODUCT_MAINTAIN and
 * authorised with PRODUCT_AUTHORIZE; the older product-area kinds keep accepting the generic
 * MASTER_* permissions as well.
 */
public enum CatalogKind {
  /** Product line. */
  PRODUCT_LINE(ProductLine.class, "Product line", Screens.PRODUCTS, Area.PRODUCT_LEGACY),
  /** Cover type. */
  COVER_TYPE(CoverType.class, "Cover type", Screens.PRODUCTS, Area.PRODUCT_LEGACY),
  /** Risk product. */
  PRODUCT(RiskProduct.class, "Product", Screens.PRODUCTS, Area.PRODUCT_LEGACY),
  /** Minimum-field rule. */
  FIELD_RULE(FieldRule.class, "Minimum-field rule", Screens.PRODUCTS, Area.PRODUCT_LEGACY),
  /** Document rule. */
  DOCUMENT_RULE(DocumentRule.class, "Document rule", Screens.PRODUCTS, Area.PRODUCT_LEGACY),
  /** TSU routing rule. */
  TSU_RULE(TsuRule.class, "TSU routing rule", Screens.PRODUCTS, Area.PRODUCT_LEGACY),
  /** Coverage / peril (PMADD01). */
  COVERAGE(Coverage.class, "Coverage", Screens.COVERAGES, Area.PRODUCT),
  /** Clause library entry (PMADD02). */
  CLAUSE(Clause.class, "Clause", Screens.COVERAGES, Area.PRODUCT),
  /** Incentive criterion (PMADD07/08). */
  INCENTIVE_CRITERIA(
      IncentiveCriteria.class, "Incentive criterion", Screens.INCENTIVES, Area.INCENTIVES),
  /** Rate-scheme exception (BRPM.007). */
  RATE_SCHEME_EXCEPTION(
      RateOverride.class, "Rate-scheme exception", Screens.PRODUCTS, Area.EXCEPTIONS),
  /** Insurer profile. */
  INSURER(InsurerProfile.class, "Insurer", Screens.INSURERS, Area.MASTER),
  /** Insurer branch. */
  INSURER_BRANCH(InsurerBranch.class, "Insurer branch", Screens.INSURERS, Area.MASTER),
  /** Commission rate. */
  COMMISSION_RATE(CommissionRate.class, "Commission rate", Screens.INSURERS, Area.MASTER),
  /** Tax or rating factor. */
  RATE(CatalogRate.class, "Tax / rating rate", Screens.RATES, Area.MASTER),
  /** Short-period table row. */
  SHORT_PERIOD_RATE(ShortPeriodRate.class, "Short-period rate", Screens.RATES, Area.MASTER),
  /** Motor BI / PD limit. */
  MOTOR_LIMIT(MotorLimit.class, "Motor limit", Screens.RATES, Area.MASTER),
  /** Sales organisation unit. */
  SALES_UNIT(SalesUnit.class, "Sales unit", Screens.SALES, Area.MASTER),
  /** Account officer of a team. */
  SALES_OFFICER(SalesOfficer.class, "Sales officer", Screens.SALES, Area.MASTER);

  private final Class<? extends AuthorizableEntity> type;
  private final String label;
  private final String link;
  private final Area area;

  CatalogKind(Class<? extends AuthorizableEntity> type, String label, String link, Area area) {
    this.type = type;
    this.label = label;
    this.link = link;
    this.area = area;
  }

  /**
   * Permissions that may authorise a record of this kind (checker).
   *
   * @return permissions, any one is enough
   */
  public List<String> authorizers() {
    return area.authorizers;
  }

  /**
   * Permissions that may maintain (and deactivate) a record of this kind (maker).
   *
   * @return permissions, any one is enough
   */
  public List<String> maintainers() {
    return area.maintainers;
  }

  /**
   * Module code shown in the approval inbox.
   *
   * @return module
   */
  public String inboxModule() {
    return area.module;
  }

  public Class<? extends AuthorizableEntity> type() {
    return type;
  }

  public String label() {
    return label;
  }

  public String link() {
    return link;
  }

  /** Who maintains and authorises a group of kinds. */
  private enum Area {
    MASTER(Permissions.MASTER_MODULE, List.of("MASTER_MAINTAIN"), List.of("MASTER_AUTHORIZE")),
    PRODUCT_LEGACY(
        Permissions.MASTER_MODULE,
        List.of("MASTER_MAINTAIN", Permissions.PRODUCT_MAINTAIN),
        List.of("MASTER_AUTHORIZE", Permissions.PRODUCT_AUTHORIZE)),
    PRODUCT(
        Permissions.PM_MODULE,
        List.of(Permissions.PRODUCT_MAINTAIN),
        List.of(Permissions.PRODUCT_AUTHORIZE)),
    INCENTIVES(
        Permissions.PM_MODULE,
        List.of("INCENTIVE_CRITERIA_MAINTAIN"),
        List.of(Permissions.PRODUCT_AUTHORIZE)),
    /** Rate-scheme exceptions: the authoriser rejects by deactivating. */
    EXCEPTIONS(
        Permissions.PM_MODULE,
        List.of(Permissions.PRODUCT_MAINTAIN, Permissions.PRODUCT_AUTHORIZE),
        List.of(Permissions.PRODUCT_AUTHORIZE));

    private final String module;
    private final List<String> maintainers;
    private final List<String> authorizers;

    Area(String module, List<String> maintainers, List<String> authorizers) {
      this.module = module;
      this.maintainers = maintainers;
      this.authorizers = authorizers;
    }
  }

  /** Permission and inbox module names. */
  private static final class Permissions {
    static final String PRODUCT_AUTHORIZE = "PRODUCT_AUTHORIZE";
    static final String PRODUCT_MAINTAIN = "PRODUCT_MAINTAIN";
    static final String PM_MODULE = "PRODUCT_MAINTENANCE";
    static final String MASTER_MODULE = "MASTER_DATA";

    private Permissions() {}
  }

  /** Screen routes. */
  private static final class Screens {
    static final String PRODUCTS = "/catalog/products";
    static final String COVERAGES = "/catalog/coverages";
    static final String INCENTIVES = "/catalog/incentives";
    static final String INSURERS = "/catalog/insurers";
    static final String RATES = "/catalog/rates";
    static final String SALES = "/catalog/sales-organisation";

    private Screens() {}
  }
}
