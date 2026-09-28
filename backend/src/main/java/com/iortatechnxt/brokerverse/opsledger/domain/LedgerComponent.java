package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.booking.domain.PremiumComponent;
import java.util.List;

/**
 * Components of an Operations invoice (OPERATIONS_DESIGN 4.1): the premium receivable (PR) by
 * component, due to insurer (DTIP), commission, VAT on commission, withholding tax on commission
 * and the PR reclassified to the client's BIR 2307 (PR2307, CSHID.027).
 */
public enum LedgerComponent {
  /** PR - basic premium. */
  BASIC,
  /** PR - documentary stamp tax. */
  DST,
  /** PR - premium tax or VAT on premium. */
  PREMIUM_TAX_VAT,
  /** PR - local government tax. */
  LGT,
  /** PR - fire service tax. */
  FST,
  /** PR - other charges. */
  OTHER,
  /** Due to insurer (premium payable to the insurer). */
  DTIP,
  /** Commission receivable from the insurer. */
  COMMISSION,
  /** Output VAT on the commission. */
  COMMISSION_VAT,
  /** Withholding tax the insurer withholds on the commission. */
  WTAX,
  /** PR covered by the client's 2% creditable withholding tax certificate (CSHID.027). */
  PR2307;

  private static final List<LedgerComponent> HIERARCHY =
      List.of(DST, PREMIUM_TAX_VAT, LGT, FST, OTHER, BASIC);

  /**
   * The name of the component on screens and documents (the labels of the recompute screen).
   *
   * @return name, e.g. "Documentary Stamp Tax"
   */
  public String label() {
    return switch (this) {
      case BASIC -> "Basic Premium";
      case DST -> "Documentary Stamp Tax";
      case PREMIUM_TAX_VAT -> "Premium Tax / VAT";
      case LGT -> "Local Government Tax";
      case FST -> "Fire Service Tax";
      case OTHER -> "Other Charges";
      case DTIP -> "Due to Insurer (Gross)";
      case COMMISSION -> "Commission";
      case COMMISSION_VAT -> "VAT on Commission";
      case WTAX -> "Withholding Tax on Commission";
      case PR2307 -> "Premium Receivable - BIR 2307";
    };
  }

  /**
   * Whether the component is part of the client's premium receivable.
   *
   * @return true for the six premium components
   */
  public boolean isPremiumReceivable() {
    return HIERARCHY.contains(this);
  }

  /**
   * The premium components in the order payments are applied (CSHID.022: DST, premium tax / VAT,
   * LGT, other charges - fire service tax first - then basic premium).
   *
   * @return components in application order
   */
  public static List<LedgerComponent> applicationHierarchy() {
    return HIERARCHY;
  }

  /**
   * The ledger component of a booked premium component.
   *
   * @param component booking component
   * @return ledger component
   */
  public static LedgerComponent of(PremiumComponent component) {
    return switch (component) {
      case BASIC -> BASIC;
      case DST -> DST;
      case PREMIUM_TAX_OR_VAT -> PREMIUM_TAX_VAT;
      case LGT -> LGT;
      case FST -> FST;
      case OTHER -> OTHER;
    };
  }
}
