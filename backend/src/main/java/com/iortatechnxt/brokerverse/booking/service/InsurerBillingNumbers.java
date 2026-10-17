package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insurer's billing number on a booking (BRID-020, EMPLOYEE_BENEFITS_DESIGN section 11):
 * required for the product lines of {@code BOOKING_BILLING_NO_LINES} (Employee Benefits lines), and
 * unique per company and insurer - a second booking with the same number is refused ({@code
 * BILLING_NO_DUPLICATE}) and the booker and the account officer are notified. A successful booking
 * with a billing number notifies them too. Which number is the billing number is confirmed under
 * EBQ18.
 */
@Component
public class InsurerBillingNumbers {

  /** Refusal: billing number missing for a line that requires it. */
  public static final String REQUIRED = "BILLING_NO_REQUIRED";

  /** Refusal: billing number already on another invoice of the insurer. */
  public static final String DUPLICATE = "BILLING_NO_DUPLICATE";

  /** Notification event of a booking with a billing number. */
  public static final String EVENT_BOOKED = "BOOKING_BILLING_BOOKED";

  /** Notification event of a refused duplicate billing number. */
  public static final String EVENT_DUPLICATE = "BOOKING_BILLING_DUPLICATE";

  private static final int MAX_LENGTH = 60;
  private static final String ENTITY = "BookedInvoice";

  private final BookedInvoiceRepository invoices;
  private final BookingSettings settings;
  private final NotificationService notifications;
  private final CurrentUser currentUser;

  /**
   * Creates the checks.
   *
   * @param invoices invoices
   * @param settings booking parameters
   * @param notifications in-app notifications
   * @param currentUser current user
   */
  public InsurerBillingNumbers(
      BookedInvoiceRepository invoices,
      BookingSettings settings,
      NotificationService notifications,
      CurrentUser currentUser) {
    this.invoices = invoices;
    this.settings = settings;
    this.notifications = notifications;
    this.currentUser = currentUser;
  }

  /**
   * Whether the account's product line needs the insurer billing number.
   *
   * @param account account
   * @return true for a line of {@code BOOKING_BILLING_NO_LINES}
   */
  @Transactional(readOnly = true)
  public boolean required(Account account) {
    return settings.billingNoLines().contains(account.getLineCode());
  }

  /**
   * Checks a billing number before booking: present when the line requires it, at most 60
   * characters, and not already on an invoice of the same company and insurer.
   *
   * @param account account to book
   * @param billingNo billing number, null when none
   * @return the billing number, null when none
   */
  @Transactional(readOnly = true)
  public String check(Account account, String billingNo) {
    if (billingNo == null) {
      if (required(account)) {
        throw new BusinessRuleException(REQUIRED, "Enter the insurer billing number");
      }
      return null;
    }
    if (billingNo.length() > MAX_LENGTH) {
      throw new BusinessRuleException(
          "BILLING_NO_TOO_LONG", "The insurer billing number has at most 60 characters");
    }
    invoices
        .findFirstByCompanyIdAndFactsInsurerCodeAndInsurerBillingNo(
            account.getCompanyId(), account.getInsurerCode(), billingNo)
        .ifPresent(
            existing -> {
              throw new BusinessRuleException(
                  DUPLICATE,
                  "Billing number "
                      + billingNo
                      + " of "
                      + account.getInsurerCode()
                      + " is already on invoice "
                      + existing.getInvoiceNo());
            });
    return billingNo;
  }

  /**
   * Notifies the booker and the account officer of a booking with a billing number.
   *
   * @param invoice booked invoice
   */
  public void notifyBooked(BookedInvoice invoice) {
    if (invoice.getInsurerBillingNo() == null) {
      return;
    }
    Notice notice =
        new Notice(
            "Booked " + invoice.getInvoiceNo() + " with billing number",
            invoice.getArn()
                + " was booked as "
                + invoice.getInvoiceNo()
                + " with insurer billing number "
                + invoice.getInsurerBillingNo()
                + ".",
            "/booking/invoices/" + invoice.getId(),
            ENTITY,
            invoice.getInvoiceNo());
    recipients(invoice.getFacts().accountOfficer())
        .forEach(u -> notifications.notifyUser(u, notice, EVENT_BOOKED));
  }

  /**
   * Notifies the booker and the account officer that a billing number was refused as a duplicate.
   * Runs in its own transaction: the booking itself is rolled back.
   *
   * @param account account whose booking was refused
   * @param message refusal message
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void notifyDuplicate(Account account, String message) {
    Notice notice =
        new Notice(
            "Duplicate billing number on " + account.getArn(),
            message,
            "/accounts/" + account.getId(),
            "Account",
            String.valueOf(account.getId()));
    recipients(account.getSales().accountOfficer())
        .forEach(u -> notifications.notifyUser(u, notice, EVENT_DUPLICATE));
  }

  private Set<String> recipients(String accountOfficer) {
    Set<String> users = new LinkedHashSet<>();
    currentUser.optionalUsername().ifPresent(users::add);
    if (accountOfficer != null) {
      users.add(accountOfficer);
    }
    return users;
  }
}
