package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.BookingEndorsement;
import com.iortatechnxt.brokerverse.booking.domain.BookingEndorsementRepository;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.BusinessType;
import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceDraft;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceFlags;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.booking.service.BookingPreviewService.PreviewLine;
import com.iortatechnxt.brokerverse.booking.service.EndorsementCalculator.Amounts;
import com.iortatechnxt.brokerverse.booking.service.EndorsementCalculator.PolicyYear;
import com.iortatechnxt.brokerverse.booking.service.port.LegacyInvoiceSource;
import com.iortatechnxt.brokerverse.booking.service.port.LegacyInvoiceSource.LegacyOriginal;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts endorsements and cancellations on booked accounts (BRNB.076/081/094) - mandatory contract
 * for Operations Adjustment (ADJID.001/011).
 *
 * <ul>
 *   <li>A positive financial endorsement books an additional-premium invoice (extends event 0,
 *       OPERATIONS_DESIGN section 5 row 19) with its service invoice.
 *   <li>A negative financial endorsement books a return invoice (negative amounts: reverses event
 *       0) and credits the service invoice.
 *   <li>A cancellation (FLAT, FLAT_RETAIN_DST, PARTIAL) books the return of the premium in force
 *       for the policy year (row 16), credits the service invoice, records the account's
 *       post-issuance cancellation ({@code AccountLifecycleService.recordCancellation}) and drops
 *       the scheduled later years. Pre-issuance reversal is the account's void or placement's
 *       cancel, not this.
 *   <li>A non-financial endorsement is recorded only (no GL entry).
 * </ul>
 *
 * Every financial posting publishes {@link InvoiceBooked} (negative amounts for returns) and is
 * idempotent on the caller's {@code sourceReference}.
 */
@Service
@Transactional
public class EndorsementPostingService {

  private static final String PREVIEW = "PREVIEW";
  private static final String ENTITY = "Endorsement";
  private static final String LEGACY_USER = "MIGRATION";

  private final AccountQueryService accounts;
  private final AccountLifecycleService lifecycle;
  private final BookedInvoiceRepository invoices;
  private final BookingEndorsementRepository endorsements;
  private final EndorsementCalculator calculator;
  private final InvoiceBooker booker;
  private final BookingPreviewService preview;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final InvoiceBuilder builder;
  private final LegacyInvoiceSource legacyInvoices;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts account reads
   * @param lifecycle account lifecycle (cancellation)
   * @param invoices booked invoices
   * @param endorsements endorsements
   * @param calculator amounts
   * @param booker invoice booker
   * @param preview journal preview
   * @param numbers document numbers
   * @param audit audit trail
   * @param builder invoice facts of an account
   * @param legacyInvoices migrated invoices (original of an endorsement of a migrated account)
   * @param clock clock
   */
  public EndorsementPostingService(
      AccountQueryService accounts,
      AccountLifecycleService lifecycle,
      BookedInvoiceRepository invoices,
      BookingEndorsementRepository endorsements,
      EndorsementCalculator calculator,
      InvoiceBooker booker,
      BookingPreviewService preview,
      DocumentNumberService numbers,
      AuditTrailService audit,
      InvoiceBuilder builder,
      LegacyInvoiceSource legacyInvoices,
      Clock clock) {
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.invoices = invoices;
    this.endorsements = endorsements;
    this.calculator = calculator;
    this.booker = booker;
    this.preview = preview;
    this.numbers = numbers;
    this.audit = audit;
    this.builder = builder;
    this.legacyInvoices = legacyInvoices;
    this.clock = clock;
  }

  /**
   * Posts an endorsement or cancellation (contract).
   *
   * @param posting endorsement
   * @return endorsement number, invoice number, journal batches and the published invoice
   */
  public EndorsementResult post(EndorsementPosting posting) {
    requireComplete(posting);
    Account account = requireBooked(posting.arn());
    if (posting.sourceReference() != null) {
      Optional<BookingEndorsement> existing =
          endorsements.findByCompanyIdAndSourceReference(
              account.getCompanyId(), posting.sourceReference());
      if (existing.isPresent()) {
        return resultOf(existing.get());
      }
    }
    BookedInvoice original = originalOf(account, posting.effectiveDate());
    PolicyYear year = policyYear(original);
    LocalDate date = bookingDate(posting);
    String number = numbers.next("EN-" + date.getYear());
    BookingEndorsement endorsement = endorsement(account, posting, number, year.year());
    if (posting.type() == EndorsementType.NON_FINANCIAL) {
      endorsements.save(endorsement);
      audit.record(ENTITY, number, AuditAction.CREATE, "Non-financial: " + posting.description());
      return new EndorsementResult(number, null, List.of(), null);
    }
    InvoiceDraft draft = draft(account, posting, original, year, number);
    BookedInvoice scheduled = BookedInvoice.draft(draft);
    if (original.isLegacy()) {
      scheduled.markLegacy();
    }
    BookedInvoice invoice = booker.book(scheduled, date, BookingSource.ENDORSEMENT);
    endorsement.linkInvoice(invoice.getInvoiceNo());
    endorsements.save(endorsement);
    if (posting.type() == EndorsementType.CANCELLATION) {
      lifecycle.recordCancellation(posting.arn(), posting.reasonCode(), posting.effectiveDate());
      invoices
          .findByArnOrderByPolicyYearAscIdAsc(posting.arn())
          .forEach(BookedInvoice::cancelSchedule);
    }
    audit.record(
        ENTITY,
        number,
        AuditAction.CREATE,
        posting.type()
            + " on "
            + posting.arn()
            + ": invoice "
            + invoice.getInvoiceNo()
            + ", gross "
            + invoice.getPremium().total().toPlainString());
    return new EndorsementResult(
        number, invoice.getInvoiceNo(), invoice.getJournalBatches(), InvoiceBooked.of(invoice));
  }

  /**
   * Live calculation and journal preview of an endorsement, without posting (BRNB.076 entry
   * screen).
   *
   * @param posting endorsement
   * @return amounts and journal lines
   */
  @Transactional(readOnly = true)
  public EndorsementPreview preview(EndorsementPosting posting) {
    requireComplete(posting);
    Account account = requireBooked(posting.arn());
    BookedInvoice original = originalOf(account, posting.effectiveDate());
    PolicyYear year = policyYear(original);
    if (posting.type() == EndorsementType.NON_FINANCIAL) {
      return new EndorsementPreview(year.year(), null, List.of());
    }
    InvoiceDraft draft = draft(account, posting, original, year, PREVIEW);
    return new EndorsementPreview(year.year(), draft, preview.journal(draft, bookingDate(posting)));
  }

  private InvoiceDraft draft(
      Account account,
      EndorsementPosting posting,
      BookedInvoice original,
      PolicyYear year,
      String number) {
    InvoiceKind kind = kindOf(posting.type());
    Amounts amounts =
        kind == InvoiceKind.CANCELLATION
            ? calculator.cancellation(
                year, posting.cancellationKind(), posting.basisOrDefault(), posting.effectiveDate())
            : calculator.financial(account, year, posting);
    requireSign(kind, amounts, year);
    return new InvoiceDraft(
        original.getCompanyId(),
        original.getBranchId(),
        original.getArn(),
        original.getAccountId(),
        number,
        kind,
        year.year(),
        original.getPolicyNo(),
        original.getFacts(),
        original.getCurrency(),
        posting.effectiveDate(),
        year.expiry(),
        amounts.premium(),
        amounts.commission(),
        original.getFlags(),
        original.getShares(),
        number,
        original.getInvoiceNo());
  }

  private static void requireSign(InvoiceKind kind, Amounts amounts, PolicyYear year) {
    int sign = amounts.premium().total().signum();
    boolean ok = kind == InvoiceKind.ENDORSEMENT_PLUS ? sign > 0 : sign < 0;
    if (!ok) {
      throw new BusinessRuleException(
          "ENDORSEMENT_SIGN_MISMATCH",
          kind == InvoiceKind.ENDORSEMENT_PLUS
              ? "A positive endorsement must add premium"
              : "A negative endorsement or cancellation must return premium");
    }
    if (sign < 0 && amounts.premium().total().negate().compareTo(year.premium().total()) > 0) {
      throw new BusinessRuleException(
          "RETURN_EXCEEDS_PREMIUM",
          "The return premium exceeds the premium in force of policy year " + year.year());
    }
  }

  private static InvoiceKind kindOf(EndorsementType type) {
    return switch (type) {
      case POSITIVE -> InvoiceKind.ENDORSEMENT_PLUS;
      case NEGATIVE -> InvoiceKind.ENDORSEMENT_MINUS;
      default -> InvoiceKind.CANCELLATION;
    };
  }

  private BookingEndorsement endorsement(
      Account account, EndorsementPosting posting, String number, int policyYear) {
    boolean calculated = posting.premium() == null && posting.sumInsuredChange() != null;
    boolean withBasis = calculated || posting.cancellationKind() == CancellationKind.PARTIAL;
    return new BookingEndorsement(
        account.getCompanyId(),
        number,
        account.getArn(),
        account.getId(),
        posting.type(),
        posting.type() == EndorsementType.CANCELLATION ? posting.cancellationKind() : null,
        withBasis ? posting.basisOrDefault().name() : null,
        posting.effectiveDate(),
        policyYear,
        calculated
            ? new BookingEndorsement.SumInsuredChange(
                posting.sumInsuredChange(), posting.ratePercent())
            : null,
        posting.description(),
        posting.reasonCode(),
        posting.sourceReference());
  }

  private EndorsementResult resultOf(BookingEndorsement endorsement) {
    if (endorsement.getInvoiceNo() == null) {
      return new EndorsementResult(endorsement.getEndorsementNo(), null, List.of(), null);
    }
    BookedInvoice invoice =
        invoices
            .findByInvoiceNo(endorsement.getInvoiceNo())
            .orElseThrow(
                () -> new IllegalStateException("No invoice " + endorsement.getInvoiceNo()));
    return new EndorsementResult(
        endorsement.getEndorsementNo(),
        invoice.getInvoiceNo(),
        invoice.getJournalBatches(),
        InvoiceBooked.of(invoice));
  }

  private BookedInvoice originalOf(Account account, LocalDate effective) {
    String arn = account.getArn();
    return invoices.findByArnOrderByPolicyYearAscIdAsc(arn).stream()
        .filter(i -> i.getKind() == InvoiceKind.BOOKING && i.isBooked())
        .filter(
            i -> !effective.isBefore(i.getInceptionDate()) && effective.isBefore(i.getExpiryDate()))
        .findFirst()
        .or(
            () ->
                legacyInvoices
                    .original(account.getCompanyId(), arn, effective)
                    .map(o -> legacyOriginal(account, o)))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "ENDORSEMENT_DATE_OUTSIDE_TERM",
                    "The effective date "
                        + effective
                        + " is not within a booked policy year of "
                        + arn));
  }

  /**
   * The original of a migrated account: the legacy invoice held by Operations, as a booked invoice
   * that is never saved (DATA_MIGRATION_DESIGN 14.4 H).
   */
  private BookedInvoice legacyOriginal(Account account, LegacyOriginal o) {
    BookedInvoice original =
        BookedInvoice.draft(
            new InvoiceDraft(
                account.getCompanyId(),
                o.branchId(),
                account.getArn(),
                account.getId(),
                o.invoiceNo(),
                InvoiceKind.BOOKING,
                o.policyYear(),
                o.policyNo(),
                builder.facts(account, o.costCenter()),
                o.currency(),
                o.inceptionDate(),
                o.expiryDate(),
                o.premium(),
                o.commission(),
                new InvoiceFlags(
                    o.directPayment(),
                    o.cwt2Percent(),
                    false,
                    BusinessType.of(account.getBusinessType())),
                o.shares(),
                null,
                null));
    original.book(
        o.invoiceNo(), o.bookingDate(), BookingSource.ENDORSEMENT, LEGACY_USER, clock.instant());
    original.markLegacy();
    return original;
  }

  private PolicyYear policyYear(BookedInvoice original) {
    List<BookedInvoice> year =
        invoices.findByArnOrderByPolicyYearAscIdAsc(original.getArn()).stream()
            .filter(i -> i.getStatus() == InvoiceStatus.BOOKED)
            .filter(i -> i.getPolicyYear() == original.getPolicyYear())
            .collect(Collectors.toCollection(ArrayList::new));
    if (original.getId() == null) {
      year.add(0, original);
    }
    PremiumComponents premium =
        year.stream()
            .map(BookedInvoice::getPremium)
            .reduce(PremiumComponents.ZERO, PremiumComponents::plus);
    CommissionTerms commission =
        CommissionTerms.of(
            original.getCommission().rate(),
            year.stream()
                .map(i -> i.getCommission().commission())
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            year.stream()
                .map(i -> i.getCommission().vatOnCommission())
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            original.getCommission().wtaxRate());
    return new PolicyYear(
        original.getPolicyYear(),
        original.getInceptionDate(),
        original.getExpiryDate(),
        premium,
        commission);
  }

  private Account requireBooked(String arn) {
    Account account = accounts.requireByArn(arn);
    if (account.getStatus() != AccountStatus.BOOKED) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_BOOKED",
          "Account " + arn + " is " + account.getStatus() + ": only booked accounts are endorsed");
    }
    return account;
  }

  private LocalDate bookingDate(EndorsementPosting posting) {
    return posting.bookingDate() == null ? BusinessClock.today(clock) : posting.bookingDate();
  }

  private static void requireComplete(EndorsementPosting posting) {
    boolean missing =
        posting.arn() == null
            || posting.type() == null
            || posting.effectiveDate() == null
            || posting.description() == null
            || posting.description().isBlank();
    if (missing) {
      throw new BusinessRuleException(
          "ENDORSEMENT_INCOMPLETE", "Enter the account, type, effective date and description");
    }
    if ((posting.type() == EndorsementType.CANCELLATION) != (posting.cancellationKind() != null)) {
      throw new BusinessRuleException(
          "CANCELLATION_KIND_REQUIRED", "A cancellation, and only a cancellation, has a kind");
    }
  }

  /**
   * Preview of an endorsement.
   *
   * @param policyYear policy year concerned
   * @param invoice invoice that would be booked, null for a non-financial endorsement
   * @param journal journal lines
   */
  public record EndorsementPreview(
      int policyYear, InvoiceDraft invoice, List<PreviewLine> journal) {

    /** Defensive copy. */
    public EndorsementPreview {
      journal = List.copyOf(journal);
    }
  }
}
