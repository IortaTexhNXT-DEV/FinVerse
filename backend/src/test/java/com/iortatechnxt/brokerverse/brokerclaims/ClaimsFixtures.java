package com.iortatechnxt.brokerverse.brokerclaims;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService.NewClaim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimSource;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService.NewLine;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService.LocationPick;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.RemittanceFixtures;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Claims Handling test data: covers issued and booked through the real account and booking
 * services (a motor cover, a property cover with several locations and co-insurance shares, a
 * direct-payment cover), payments applied on the ledger as cashiering posts them, and claims
 * recorded through the service as the Claims Officer.
 */
@Component
public class ClaimsFixtures {

  /** The demo Claims Officer (Motor HO). */
  public static final String OFFICER = "clmofficer";

  /** The demo Claims Team Lead. */
  public static final String TL = "clmtl";

  /** Lead insurer of the test covers. */
  public static final String LEAD = "INS-MGIC";

  /** Co-insurer of the property covers. */
  public static final String CO_INSURER = "INS-LAC";

  private final BookingFixtures booking;
  private final RemittanceFixtures remittance;
  private final AccountService accounts;
  private final AccountLifecycleService lifecycle;
  private final AccountQueryService queries;
  private final ClientService clients;
  private final DocumentService documents;
  private final BookingService bookings;
  private final InvoiceLedgerQueryService ledger;
  private final ClaimRecordingService recording;
  private final AsUser as;

  ClaimsFixtures(
      BookingFixtures booking,
      RemittanceFixtures remittance,
      AccountService accounts,
      AccountLifecycleService lifecycle,
      AccountQueryService queries,
      ClientService clients,
      DocumentService documents,
      BookingService bookings,
      InvoiceLedgerQueryService ledger,
      ClaimRecordingService recording,
      AsUser as) {
    this.booking = booking;
    this.remittance = remittance;
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.queries = queries;
    this.clients = clients;
    this.documents = documents;
    this.bookings = bookings;
    this.ledger = ledger;
    this.recording = recording;
    this.as = as;
  }

  /** The demo company. */
  public Long company() {
    return booking.company();
  }

  /** Start of the test covers: two months ago, so that a loss of this week is covered. */
  public static LocalDate coverFrom() {
    return LocalDate.now().minusMonths(2);
  }

  /** A booked motor cover paid via BDOI, its invoice still unpaid. */
  public OpsInvoice motorInvoice() {
    Account account =
        booking.issued(
            new BookingFixtures.Spec(
                "MTR10", "CBG", PaymentArrangement.VIA_BDOI, coverFrom(), coverFrom().plusYears(1), 1));
    return book(account, List.of());
  }

  /** A booked motor cover paid directly to the insurer (BRNB.114). */
  public OpsInvoice directPaymentInvoice() {
    Account account =
        booking.issued(
            new BookingFixtures.Spec(
                "MTR10",
                "CBG",
                PaymentArrangement.DIRECT_TO_INSURER,
                coverFrom(),
                coverFrom().plusYears(1),
                1));
    return book(account, List.of());
  }

  /** A booked property cover with several locations, 60 % lead insurer and 40 % co-insurer. */
  public OpsInvoice propertyInvoice(int locations) {
    Account account = property(locations);
    return book(
        account,
        List.of(
            new InsurerShare(LEAD, new BigDecimal("60.0000")),
            new InsurerShare(CO_INSURER, new BigDecimal("40.0000"))));
  }

  /** An issued property cover with several locations, not booked. */
  public Account property(int locations) {
    Long clientId = clients.requireByCode(company(), BookingFixtures.CLIENT).getId();
    String id = BookingFixtures.token();
    List<RiskItemData> items = new ArrayList<>();
    for (int i = 1; i <= locations; i++) {
      items.add(location(i + " Claims Test St " + id, i == 1 ? "Makati" : "Pasig"));
    }
    AccountDraft draft =
        new AccountDraft(
            clientId,
            "PAR01",
            "CBG",
            "EMAIL",
            LEAD,
            "MKT",
            coverFrom(),
            coverFrom().plusYears(1),
            false,
            1,
            "PHP",
            PaymentArrangement.VIA_BDOI,
            Mortgage.NONE,
            null,
            items,
            null,
            null,
            null);
    Account account = as.run("ao", () -> accounts.createDraft(NewAccount.direct(company(), draft)));
    attach("Account", account.getId(), "IDF");
    attach("Account", account.getId(), "EPOLICY");
    as.run("ao", () -> accounts.submit(account.getId(), null));
    as.run("proc", () -> accounts.directBooking(account.getId(), null));
    as.run(
        "proc",
        () -> lifecycle.recordPolicy(account.getArn(), List.of("FI-" + id), LocalDate.of(2026, 9, 1)));
    return queries.get(account.getId());
  }

  /** Books an issued account into the ledger. */
  public OpsInvoice book(Account account, List<InsurerShare> shares) {
    var booked =
        as.run(
            "proc",
            () ->
                bookings.book(
                    account.getArn(),
                    new BookingOptions(BookingFixtures.BOOKED_ON, null, null, shares),
                    BookingSource.INDIVIDUAL));
    return ledger.require(booked.getInvoiceNo());
  }

  /** Pays an invoice in full as cashiering would. */
  public void pay(OpsInvoice invoice) {
    remittance.payInFull(ledger.require(invoice.getInvoiceNo()), RemittanceFixtures.PAID_ON);
  }

  /** The ledger invoice now. */
  public OpsInvoice reload(String invoiceNo) {
    return ledger.require(invoiceNo);
  }

  /** Loss data of a loss two days ago reported yesterday. */
  public static LossDetails.Loss loss(String nature) {
    LocalDate today = LocalDate.now();
    return new LossDetails.Loss(
        today.minusDays(2), today.minusDays(1), nature, nature, "Test loss", "EDSA", null, null);
  }

  /** A claim request on a cover. */
  public static NewClaim request(
      String arn, LossDetails.Loss loss, List<LocationPick> locations, List<NewLine> lines) {
    return new NewClaim(
        arn,
        1,
        ClaimSource.BDOI_NOTICE,
        null,
        loss,
        new LossDetails.Amounts(new BigDecimal("150000.00"), new BigDecimal("5000.00"), null),
        locations,
        lines,
        false,
        false);
  }

  /** Records a motor claim on a cover as the Claims Officer. */
  public Claim motorClaim(String arn) {
    return as.run(
        OFFICER,
        () -> recording.record(company(), request(arn, loss("MOTOR_OWN_DAMAGE"), List.of(), List.of())));
  }

  /** Uploads a PDF to a record and returns its id. */
  public Long attach(String entityType, Long entityId, String documentType) {
    return as.run(
            "ao",
            () ->
                documents.upload(
                    new AttachmentTarget(entityType, String.valueOf(entityId)),
                    List.of(
                        new UploadedFile(
                            "doc.pdf", "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII))),
                    new UploadOptions(documentType, false, null, null)))
        .get(0)
        .getId();
  }

  private static RiskItemData location(String address, String city) {
    return new RiskItemData(
        null,
        null,
        null,
        null,
        null,
        null,
        new Location(
            address,
            city,
            "Metro Manila",
            "DWELLING",
            "CLASS_1",
            List.of(new InsuredItem("Building", new BigDecimal("4000000")))),
        null);
  }
}
