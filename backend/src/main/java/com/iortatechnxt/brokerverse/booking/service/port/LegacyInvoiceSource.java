package com.iortatechnxt.brokerverse.booking.service.port;

import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port to the migrated invoices (DATA_MIGRATION_DESIGN 14.4 H): an account migrated with its open
 * legacy invoice has no booked original in BIBS, so an endorsement or cancellation of the policy
 * year starts from the legacy invoice held by Operations. The default has none; Operations
 * implements it from the legacy invoice and its origin snapshot.
 */
public interface LegacyInvoiceSource {

  /**
   * The legacy original of the policy year of an account covering a date.
   *
   * @param companyId company
   * @param arn account
   * @param effectiveDate effective date of the endorsement
   * @return the original, empty when the account has none
   */
  Optional<LegacyOriginal> original(Long companyId, String arn, LocalDate effectiveDate);

  /**
   * The facts of a legacy original.
   *
   * @param invoiceNo number of the migrated invoice in BIBS
   * @param branchId invoicing branch
   * @param policyYear policy year
   * @param policyNo policy number
   * @param currency currency
   * @param costCenter cost center of the legacy invoice (null for the account's)
   * @param bookingDate booking date (legacy invoice date)
   * @param inceptionDate inception of the policy year
   * @param expiryDate expiry of the policy year
   * @param premium premium by component (booked and adjusted)
   * @param commission commission terms
   * @param directPayment direct payment to the insurer
   * @param cwt2Percent 2% creditable withholding tax
   * @param shares insurer shares
   */
  record LegacyOriginal(
      String invoiceNo,
      Long branchId,
      int policyYear,
      String policyNo,
      String currency,
      String costCenter,
      LocalDate bookingDate,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      PremiumComponents premium,
      CommissionTerms commission,
      boolean directPayment,
      boolean cwt2Percent,
      List<InsurerShare> shares) {

    /** Defensive copy. */
    public LegacyOriginal {
      shares = List.copyOf(shares);
    }
  }
}
