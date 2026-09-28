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
  BASIC("Basic Premium"),
  /** PR - documentary stamp tax. */
  DST("Documentary Stamp Tax"),
  /** PR - premium tax or VAT on premium. */
  PREMIUM_TAX_VAT("Premium Tax / VAT"),
  /** PR - local government tax. */
  LGT("Local Government Tax"),
  /** PR - fire service tax. */
  FST("Fire Service Tax"),
  /** PR - other charges. */
  OTHER("Other Charges"),
  /** Due to insurer (premium payable to the insurer). */
  DTIP("Due to Insurer (Gross)"),
  /** Commission receivable from the insurer. */
  COMMISSION("Commission"),
  /** Output VAT on the commission. */
  COMMISSION_VAT("VAT on Commission"),
  /** Withholding tax the insurer withholds on the commission. */
  WTAX("Withholding Tax on Commission"),
  /** PR covered by the client's 2% creditable withholding tax certificate (CSHID.027). */
  PR2307("Premium Receivable - BIR 2307");

  private static final List<LedgerComponent> HIERARCHY =
      List.of(DST, PREMIUM_TAX_VAT, LGT, FST, OTHER, BASIC);

  private final String label;

  LedgerComponent(String label) {
    this.label = label;
  }

  /**
   * The name of the component on screens and documents (the labels of the recompute screen).
   *
   * @return name, e.g. "Documentary Stamp Tax"
   */
  public String label() {
    return label;
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
