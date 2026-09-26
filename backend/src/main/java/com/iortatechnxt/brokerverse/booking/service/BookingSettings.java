package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Booking parameters (sys_parameter, V870) and the booking branch: commission realization {@code
 * OPS_COMMISSION_REALIZATION}, withholding tax rate on commission {@code BOOKING_WTAX_RATE} and the
 * CWT 2 % segments {@code BOOKING_CWT2_SEGMENTS}; the product lines that need the insurer billing
 * number {@code BOOKING_BILLING_NO_LINES} (V1030, BRID-020).
 */
@Component
public class BookingSettings {

  /** ON_COLLECTION (default) or ON_BOOKING. */
  public static final String COMMISSION_REALIZATION = "OPS_COMMISSION_REALIZATION";

  /** Commission realized when booked: income and output VAT at booking. */
  public static final String ON_BOOKING = "ON_BOOKING";

  /** Withholding tax rate on commission, in percent. */
  public static final String WTAX_RATE = "BOOKING_WTAX_RATE";

  /** Segments whose clients withhold 2 % creditable tax on premium. */
  public static final String CWT2_SEGMENTS = "BOOKING_CWT2_SEGMENTS";

  /** Product lines whose bookings need the insurer billing number (BRID-020). */
  public static final String BILLING_NO_LINES = "BOOKING_BILLING_NO_LINES";

  private static final String DEFAULT_WTAX = "10";

  private final SystemParameterService parameters;
  private final OrganizationService organization;

  /**
   * Creates the settings.
   *
   * @param parameters business parameters
   * @param organization companies and branches
   */
  public BookingSettings(SystemParameterService parameters, OrganizationService organization) {
    this.parameters = parameters;
    this.organization = organization;
  }

  /**
   * Whether commission is realized at booking (income and output VAT) instead of on collection.
   *
   * @return true for ON_BOOKING
   */
  public boolean realizeCommissionOnBooking() {
    return ON_BOOKING.equals(parameters.text(COMMISSION_REALIZATION, "ON_COLLECTION").strip());
  }

  /**
   * Withholding tax rate on the broker commission.
   *
   * @return rate in percent
   */
  public BigDecimal wtaxRate() {
    String value = parameters.text(WTAX_RATE, DEFAULT_WTAX).strip();
    return new BigDecimal(value.isEmpty() ? DEFAULT_WTAX : value);
  }

  /**
   * Market segments whose clients withhold 2 % creditable tax (CWT 2 % flag default).
   *
   * @return segment codes
   */
  public List<String> cwt2Segments() {
    return parameters.items(CWT2_SEGMENTS);
  }

  /**
   * Product lines whose bookings need the insurer billing number (BRID-020; EB lines by default).
   *
   * @return product line codes
   */
  public List<String> billingNoLines() {
    return parameters.items(BILLING_NO_LINES);
  }

  /**
   * The branch that books and numbers invoices: the company's head office (BIR series per branch).
   *
   * @param companyId company
   * @return branch
   */
  public BranchRef branch(Long companyId) {
    List<Branch> branches = organization.listBranches(companyId);
    Branch branch =
        branches.stream()
            .filter(Branch::isHeadOffice)
            .findFirst()
            .or(() -> branches.stream().findFirst())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "BOOKING_BRANCH_MISSING", "The company has no branch to book invoices"));
    return new BranchRef(branch.getId(), branch.getCode());
  }

  /**
   * The company (letterhead, base currency).
   *
   * @param companyId company
   * @return company
   */
  public Company company(Long companyId) {
    return organization.getCompany(companyId);
  }

  /**
   * The booking branch.
   *
   * @param id branch id
   * @param code branch code (document number series)
   */
  public record BranchRef(Long id, String code) {}
}
