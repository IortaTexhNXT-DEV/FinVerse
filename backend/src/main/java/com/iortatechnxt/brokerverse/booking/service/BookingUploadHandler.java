package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Booking by upload (BRNB.036/061, handler {@code BOOKING_UPLOAD}): one ARN per row, with an
 * optional booking date, cost center and insurer billing number (BRID-020). Each valid row is
 * booked in its own transaction, exactly as an individual booking.
 */
@Component
public class BookingUploadHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "BOOKING_UPLOAD";

  static final String ARN = "ARN";
  static final String BOOKING_DATE = "Booking Date";
  static final String COST_CENTER = "Cost Center";
  static final String BILLING_NO = "Insurer Billing No";

  private final BookingService booking;
  private final DimensionService dimensions;
  private final InsurerBillingNumbers billing;
  private final TransactionTemplate checks;

  /**
   * Creates the handler.
   *
   * @param booking booking service
   * @param dimensions cost center validation
   * @param billing insurer billing number checks (BRID-020)
   * @param txManager transaction manager (checks run in their own read-only transaction, so a
   *     refused row does not mark the upload's transaction for rollback)
   */
  public BookingUploadHandler(
      BookingService booking,
      DimensionService dimensions,
      InsurerBillingNumbers billing,
      PlatformTransactionManager txManager) {
    this.booking = booking;
    this.dimensions = dimensions;
    this.billing = billing;
    this.checks = new TransactionTemplate(txManager);
    this.checks.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.checks.setReadOnly(true);
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Booking upload";
  }

  @Override
  public String permission() {
    return "BOOKING_PROCESS";
  }

  @Override
  public String instructions() {
    return "One row per account to book: its Account Reference Number (ARN) must be in status"
        + " POLICY_ISSUED. Booking Date defaults to the upload date; Cost Center defaults to the"
        + " account's (sales organisation). Insurer Billing No is required for the product lines"
        + " of parameter BOOKING_BILLING_NO_LINES (Employee Benefits) and unique per insurer.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(ARN, "Account Reference Number", "ARN-2026-000123"),
        new BulkColumn(
            BOOKING_DATE, "Booking date (optional)", false, BulkColumn.Type.DATE, "2026-09-15"),
        BulkColumn.optional(COST_CENTER, "Cost center (optional)", "NB-CBG-M"),
        BulkColumn.optional(
            BILLING_NO, "Insurer billing number (required for EB lines)", "HMO-BILL-2026-0001"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(ARN);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    try {
      checks.executeWithoutResult(
          s -> {
            Account account = booking.requireBookable(row.text(ARN));
            if (!account.getCompanyId().equals(context.companyId())) {
              errors.add("Account " + row.text(ARN) + " belongs to another company");
            }
            dimensions.validateOptional(
                context.companyId(), DimensionType.COST_CENTER, row.text(COST_CENTER));
            billing.check(account, row.text(BILLING_NO));
          });
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      errors.add(e.getMessage());
    }
    LocalDate date = row.date(BOOKING_DATE);
    if (date != null && date.isAfter(context.businessDate())) {
      errors.add("The booking date " + date + " is in the future");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    LocalDate date = row.date(BOOKING_DATE);
    return booking
        .book(
            row.text(ARN),
            BookingOptions.of(date == null ? context.businessDate() : date, row.text(COST_CENTER))
                .withBillingNo(row.text(BILLING_NO)),
            BookingSource.UPLOAD)
        .getInvoiceNo();
  }
}
