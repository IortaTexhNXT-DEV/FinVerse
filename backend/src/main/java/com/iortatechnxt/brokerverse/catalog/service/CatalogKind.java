package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.CatalogRate;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate;
import com.iortatechnxt.brokerverse.catalog.domain.CoverType;
import com.iortatechnxt.brokerverse.catalog.domain.DocumentRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRate;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;

/**
 * The kinds of catalog master records, with their entity, the label shown in the approval inbox and
 * audit trail, and the screen where they are maintained.
 */
public enum CatalogKind {
  /** Product line. */
  PRODUCT_LINE(ProductLine.class, "Product line", Screens.PRODUCTS),
  /** Cover type. */
  COVER_TYPE(CoverType.class, "Cover type", Screens.PRODUCTS),
  /** Risk product. */
  PRODUCT(RiskProduct.class, "Product", Screens.PRODUCTS),
  /** Minimum-field rule. */
  FIELD_RULE(FieldRule.class, "Minimum-field rule", Screens.PRODUCTS),
  /** Document rule. */
  DOCUMENT_RULE(DocumentRule.class, "Document rule", Screens.PRODUCTS),
  /** TSU routing rule. */
  TSU_RULE(TsuRule.class, "TSU routing rule", Screens.PRODUCTS),
  /** Insurer profile. */
  INSURER(InsurerProfile.class, "Insurer", Screens.INSURERS),
  /** Insurer branch. */
  INSURER_BRANCH(InsurerBranch.class, "Insurer branch", Screens.INSURERS),
  /** Commission rate. */
  COMMISSION_RATE(CommissionRate.class, "Commission rate", Screens.INSURERS),
  /** Tax or rating factor. */
  RATE(CatalogRate.class, "Tax / rating rate", Screens.RATES),
  /** Short-period table row. */
  SHORT_PERIOD_RATE(ShortPeriodRate.class, "Short-period rate", Screens.RATES),
  /** Motor BI / PD limit. */
  MOTOR_LIMIT(MotorLimit.class, "Motor limit", Screens.RATES),
  /** Sales organisation unit. */
  SALES_UNIT(SalesUnit.class, "Sales unit", Screens.SALES),
  /** Account officer of a team. */
  SALES_OFFICER(SalesOfficer.class, "Sales officer", Screens.SALES);

  private final Class<? extends AuthorizableEntity> type;
  private final String label;
  private final String link;

  CatalogKind(Class<? extends AuthorizableEntity> type, String label, String link) {
    this.type = type;
    this.label = label;
    this.link = link;
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

  /** Screen routes. */
  private static final class Screens {
    static final String PRODUCTS = "/catalog/products";
    static final String INSURERS = "/catalog/insurers";
    static final String RATES = "/catalog/rates";
    static final String SALES = "/catalog/sales-organisation";

    private Screens() {}
  }
}
