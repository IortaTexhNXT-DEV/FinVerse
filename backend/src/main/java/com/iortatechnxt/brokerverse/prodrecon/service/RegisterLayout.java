package com.iortatechnxt.brokerverse.prodrecon.service;

import java.util.List;

/**
 * Columns of the production register sent to insurers and read back from them (Annex IV #5
 * Production Register, PRCID.006; template per insurer parked, OQ29). Every column is locked except
 * {@link #REMARKS} (PRCID.002). The insurer returns the same layout with remarks, and adds its own
 * production below the register lines (PRCID.009/022); {@link #INCENTIVE} is optional. Sum insured
 * is not part of the Operations ledger and is left out until the template is agreed.
 */
public final class RegisterLayout {

  /** Production month, yyyy-MM. */
  public static final String MONTH = "Month of Production";

  /** Insurer code. */
  public static final String INSURER = "Insurer";

  /** Invoice / reference number. */
  public static final String INVOICE = "Invoice Number";

  /** Booking date. */
  public static final String BOOKING_DATE = "Booking Date";

  /** Period start. */
  public static final String INCEPTION = "Inception Date";

  /** Period end. */
  public static final String EXPIRY = "Expiry Date";

  /** Policy number. */
  public static final String POLICY = "Policy No.";

  /** Endorsement number. */
  public static final String ENDORSEMENT = "Endorsement No.";

  /** PN number(s). */
  public static final String PN = "PN No.";

  /** Assured name. */
  public static final String ASSURED = "Assured Name";

  /** Risk code. */
  public static final String RISK = "Risk Type";

  /** Basic premium. */
  public static final String BASIC = "Basic Premium";

  /** Gross commission. */
  public static final String COMMISSION = "Gross Commission";

  /** Client code. */
  public static final String CLIENT = "A/R Client";

  /** Gross premium. */
  public static final String GROSS = "Gross Premium";

  /** VAT on commission. */
  public static final String VAT = "Booked VAT";

  /** Premium paid so far. */
  public static final String PAID = "Amount Paid";

  /** Date of the last payment. */
  public static final String DATE_PAID = "Date Paid";

  /** Receipt of the last payment. */
  public static final String RECEIPT = "AR / OR Number";

  /** Booking, endorsement or cancellation. */
  public static final String ADJUSTMENT = "Adjustment Type";

  /** Remittance status. */
  public static final String REMITTANCE = "Remittance Status";

  /** Early incentive reported by the insurer (optional, PRCID.028). */
  public static final String INCENTIVE = "Incentive";

  /** The insurer's remarks: the only editable column (PRCID.002). */
  public static final String REMARKS = "Remarks";

  /** Every column in order. */
  public static final List<String> HEADERS =
      List.of(
          MONTH,
          INSURER,
          INVOICE,
          BOOKING_DATE,
          INCEPTION,
          EXPIRY,
          POLICY,
          ENDORSEMENT,
          PN,
          ASSURED,
          RISK,
          BASIC,
          COMMISSION,
          CLIENT,
          GROSS,
          VAT,
          PAID,
          DATE_PAID,
          RECEIPT,
          ADJUSTMENT,
          REMITTANCE,
          INCENTIVE,
          REMARKS);

  private RegisterLayout() {}
}
