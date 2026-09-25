package com.iortatechnxt.brokerverse.collections.demo;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The three-year demo account of the Collections billing storyline (BRCLXN.058, demo profile only):
 * a PAR01 property policy of 2026-09-01 to 2029-09-01 for demo client CL-2026-000005, brought to
 * POLICY_ISSUED through the real account services by the account officer ({@code ao}) and the
 * processor ({@code proc}), then booked by the processor. Booking books policy year 1 in the ledger
 * and schedules years 2 and 3 (BRNB.112).
 */
@Component
@Profile("demo")
public class DemoMultiYearAccount {

  /** Client of the account. */
  static final String CLIENT = "CL-2026-000005";

  private static final LocalDate FROM = LocalDate.parse("2026-09-01");
  private static final int YEARS = 3;
  private static final String AO = "ao";
  private static final String PROCESSOR = "proc";

  private final AccountService accounts;
  private final AccountLifecycleService lifecycle;
  private final ClientService clients;
  private final DocumentService documents;
  private final BookingService booking;
  private final DemoUsers users;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param accounts accounts
   * @param lifecycle policy numbers
   * @param clients demo client
   * @param documents IDF and e-policy
   * @param booking booking
   * @param users demo sign-in
   * @param clock clock
   */
  public DemoMultiYearAccount(
      AccountService accounts,
      AccountLifecycleService lifecycle,
      ClientService clients,
      DocumentService documents,
      BookingService booking,
      DemoUsers users,
      Clock clock) {
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.clients = clients;
    this.documents = documents;
    this.booking = booking;
    this.users = users;
    this.clock = clock;
  }

  /**
   * Creates, issues and books the account.
   *
   * @param companyId company
   * @return the booked invoice of policy year 1
   */
  public BookedInvoice book(Long companyId) {
    Long clientId = users.as(AO, () -> clients.requireByCode(companyId, CLIENT).getId());
    AccountDraft draft =
        new AccountDraft(
            clientId,
            "PAR01",
            "CBG",
            "EMAIL",
            "INS-MGIC",
            "MKT",
            FROM,
            FROM.plusYears(YEARS),
            true,
            YEARS,
            "PHP",
            PaymentArrangement.VIA_BDOI,
            Mortgage.NONE,
            null,
            List.of(location()),
            null,
            null,
            null);
    Account account = users.as(AO, () -> accounts.createDraft(NewAccount.direct(companyId, draft)));
    attach(account.getId(), "IDF");
    attach(account.getId(), "EPOLICY");
    users.run(AO, () -> accounts.submit(account.getId(), null));
    users.run(PROCESSOR, () -> accounts.directBooking(account.getId(), null));
    String arn = account.getArn();
    users.run(
        PROCESSOR,
        () ->
            lifecycle.recordPolicy(
                arn,
                List.of("MGIC-FI-2026-91901", "MGIC-FI-2027-91901", "MGIC-FI-2028-91901"),
                FROM));
    return users.as(
        PROCESSOR,
        () ->
            booking.book(
                arn, BookingOptions.of(LocalDate.now(clock), null), BookingSource.INDIVIDUAL));
  }

  private void attach(Long accountId, String documentType) {
    users.run(
        AO,
        () ->
            documents.upload(
                new AttachmentTarget("Account", String.valueOf(accountId)),
                List.of(
                    new UploadedFile(
                        documentType.toLowerCase(Locale.ROOT) + ".pdf",
                        "%PDF-1.4 demo".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions(documentType, false, null, null)));
  }

  private static RiskItemData location() {
    return new RiskItemData(
        null,
        null,
        null,
        null,
        null,
        null,
        new Location(
            "41 Installment Demo Street, Kapitolyo",
            "Pasig",
            "Metro Manila",
            "DWELLING",
            "CLASS_1",
            List.of(new InsuredItem("Building", new BigDecimal("6000000")))),
        null);
  }
}
