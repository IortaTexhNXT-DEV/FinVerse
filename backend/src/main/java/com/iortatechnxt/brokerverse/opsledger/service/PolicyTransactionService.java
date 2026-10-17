package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.port.PolicyTransactionSource;
import com.iortatechnxt.brokerverse.opsledger.service.port.PolicyTransactionSource.Kind;
import com.iortatechnxt.brokerverse.opsledger.service.port.PolicyTransactionSource.SourcedTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The policy transaction history (ADJID.022/024, RMTID.038): the original booking, then every
 * endorsement, cancellation, adjustment, write-off and refund of the policy, in order, each with
 * its premium, tax and commission change, the position after it, its status and the GL journals it
 * posted with their lines. It reads the Operations invoice ledger, the booked invoices, the
 * transactions of the other modules ({@link PolicyTransactionSource}) and the journal batches of
 * the accounting engine: no second ledger is kept.
 */
@Service
@Transactional(readOnly = true)
@SuppressWarnings("PMD.GodClass") // assembles the history from the ledger, bookings and sources
public class PolicyTransactionService {

  /** Status of a booked invoice without a request behind it. */
  static final String BOOKED = "BOOKED";

  private static final List<LedgerComponent> TAXES =
      List.of(
          LedgerComponent.DST,
          LedgerComponent.PREMIUM_TAX_VAT,
          LedgerComponent.LGT,
          LedgerComponent.FST,
          LedgerComponent.OTHER);

  private final InvoiceLedgerQueryService ledger;
  private final BookingQueryService bookings;
  private final JournalBatchRepository journals;
  private final List<PolicyTransactionSource> sources;
  private final MigratedInvoiceHistory history;

  /**
   * Creates the service.
   *
   * @param ledger invoice ledger
   * @param bookings booked invoices (components and booking journals)
   * @param journals journal batches of the accounting engine
   * @param sources transactions of the other modules
   * @param history opening journals of migrated invoices
   */
  public PolicyTransactionService(
      InvoiceLedgerQueryService ledger,
      BookingQueryService bookings,
      JournalBatchRepository journals,
      List<PolicyTransactionSource> sources,
      MigratedInvoiceHistory history) {
    this.ledger = ledger;
    this.bookings = bookings;
    this.journals = journals;
    this.sources = sources;
    this.history = history;
  }

  /**
   * The history of the policy an invoice belongs to (its invoice family).
   *
   * @param invoiceNo any invoice of the family
   * @return history
   */
  public PolicyTransactions forInvoice(String invoiceNo) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    return build(invoice, ledger.family(invoiceNo));
  }

  /**
   * The history of an account: every invoice of the ARN (each policy year) with their endorsements.
   *
   * @param arn Account Reference Number
   * @return history, empty while the account is not booked
   */
  public PolicyTransactions forArn(String arn) {
    List<OpsInvoice> invoices = ledger.forArn(arn);
    if (invoices.isEmpty()) {
      return new PolicyTransactions(null, arn, null, null, List.of());
    }
    return build(invoices.get(0), invoices);
  }

  private PolicyTransactions build(OpsInvoice anchor, List<OpsInvoice> invoices) {
    Set<String> numbers = new LinkedHashSet<>();
    invoices.forEach(i -> numbers.add(i.getInvoiceNo()));
    List<SourcedTransaction> sourced = new ArrayList<>();
    sources.forEach(s -> sourced.addAll(s.transactionsFor(numbers)));
    Map<String, SourcedTransaction> byBooked = new HashMap<>();
    for (SourcedTransaction s : sourced) {
      if (s.kind() != Kind.REFUND
          && s.bookedInvoiceNo() != null
          && numbers.contains(s.bookedInvoiceNo())) {
        byBooked.putIfAbsent(s.bookedInvoiceNo(), s);
      }
    }
    List<Draft> drafts = new ArrayList<>();
    for (OpsInvoice invoice : invoices) {
      drafts.add(invoiceDraft(invoice, byBooked.get(invoice.getInvoiceNo()), drafts.size()));
    }
    for (SourcedTransaction s : sourced) {
      if (!byBooked.containsValue(s)) {
        drafts.add(sourcedDraft(s, drafts.size()));
      }
    }
    drafts.addAll(writeOffs(invoices, sourced, drafts.size()));
    drafts.sort(
        Comparator.comparing(Draft::date, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(d -> d.kind() == Kind.BOOKING ? 0 : 1)
            .thenComparingInt(Draft::order));
    return new PolicyTransactions(
        anchor.getRootInvoiceNo(),
        anchor.getArn(),
        anchor.getPolicyNo(),
        anchor.getCurrency(),
        rows(anchor.getCompanyId(), drafts));
  }

  private List<Row> rows(Long companyId, List<Draft> drafts) {
    Map<String, Optional<Journal>> loaded = new HashMap<>();
    Amounts position = Amounts.ZERO;
    List<Row> rows = new ArrayList<>();
    for (Draft d : drafts) {
      Amounts change = Amounts.of(d.changes());
      Amounts after = null;
      if (d.posted()) {
        position = position.plus(change);
        after = position;
      }
      List<Journal> posted = new ArrayList<>();
      for (String batchNo : d.journalBatchNos()) {
        loaded.computeIfAbsent(batchNo, no -> journal(companyId, no)).ifPresent(posted::add);
      }
      rows.add(
          new Row(
              rows.size() + 1,
              d.kind(),
              d.date(),
              d.effectiveDate(),
              d.typeLabel(),
              d.detail(),
              d.refs(),
              change,
              after,
              d.balanceChange(),
              d.status(),
              d.statusLabel(),
              d.posted(),
              posted));
    }
    return rows;
  }

  private Draft invoiceDraft(OpsInvoice invoice, SourcedTransaction request, int order) {
    Optional<BookedInvoice> booked = booked(invoice);
    Set<String> batches = new LinkedHashSet<>();
    booked.ifPresent(b -> batches.addAll(b.getJournalBatches()));
    ledger.movements(invoice.getInvoiceNo()).stream()
        .filter(m -> m.getMovementType() == MovementType.BOOKED && m.getJournalBatchNo() != null)
        .forEach(m -> batches.add(m.getJournalBatchNo()));
    Refs refs =
        new Refs(
            invoice.getInvoiceNo(),
            invoice.getEndorsementNo(),
            request == null ? null : request.reference(),
            request == null ? null : request.recordId());
    Map<LedgerComponent, BigDecimal> changes = invoiceChanges(invoice, booked.orElse(null));
    if (request == null) {
      return bookedDraft(invoice, refs, changes, batches, order);
    }
    batches.addAll(request.journalBatchNos());
    return new Draft(
        request.kind(),
        invoice.getClassification().bookingDate(),
        request.effectiveDate(),
        request.typeLabel(),
        request.detail(),
        refs,
        changes,
        null,
        request.status(),
        request.statusLabel(),
        true,
        List.copyOf(batches),
        order);
  }

  private static Draft sourcedDraft(SourcedTransaction s, int order) {
    return new Draft(
        s.kind(),
        s.date(),
        s.effectiveDate(),
        s.typeLabel(),
        s.detail(),
        new Refs(s.invoiceNo(), null, s.reference(), s.recordId()),
        s.changes(),
        null,
        s.status(),
        s.statusLabel(),
        s.posted(),
        s.journalBatchNos(),
        order);
  }

  /**
   * Write-offs of the minimal balance file (ADJID.026) and other ledger write-offs that no request
   * of another module already shows.
   */
  private List<Draft> writeOffs(
      List<OpsInvoice> invoices, List<SourcedTransaction> sourced, int first) {
    Set<String> covered = new LinkedHashSet<>();
    sourced.forEach(
        s -> {
          covered.add(s.reference());
          covered.addAll(s.journalBatchNos());
        });
    List<Draft> drafts = new ArrayList<>();
    for (OpsInvoice invoice : invoices) {
      Map<String, List<OpsInvoiceMovement>> groups = new LinkedHashMap<>();
      ledger.movements(invoice.getInvoiceNo()).stream()
          .filter(m -> m.getMovementType() == MovementType.WRITE_OFF)
          .filter(m -> !covered.contains(m.getBatchNo()))
          .filter(m -> !covered.contains(m.getJournalBatchNo()))
          .forEach(m -> groups.computeIfAbsent(m.getSourceRef(), k -> new ArrayList<>()).add(m));
      for (List<OpsInvoiceMovement> group : groups.values()) {
        OpsInvoiceMovement head = group.get(0);
        BigDecimal balance =
            group.stream()
                .map(OpsInvoiceMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .negate();
        List<String> batches =
            group.stream()
                .map(OpsInvoiceMovement::getJournalBatchNo)
                .filter(b -> b != null)
                .distinct()
                .toList();
        drafts.add(
            new Draft(
                Kind.ADJUSTMENT,
                head.getValueDate(),
                null,
                "Write-off of Minimal Balance",
                head.getBatchNo(),
                new Refs(invoice.getInvoiceNo(), null, head.getBatchNo(), null),
                Map.of(),
                balance,
                "POSTED",
                "Posted",
                true,
                batches,
                first + drafts.size()));
      }
    }
    return drafts;
  }

  /** An invoice without a request behind it: booked in BIBS, or migrated with its opening. */
  private Draft bookedDraft(
      OpsInvoice invoice,
      Refs refs,
      Map<LedgerComponent, BigDecimal> changes,
      Set<String> batches,
      int order) {
    boolean migrated = invoice.getRecordOrigin().isMigrated();
    if (migrated) {
      batches.addAll(history.openingJournals(invoice));
    }
    return new Draft(
        kindOf(invoice.getKind()),
        invoice.getClassification().bookingDate(),
        invoice.getKind() == InvoiceKind.BOOKING
            ? invoice.getClassification().inceptionDate()
            : null,
        kindLabel(invoice.getKind()) + (migrated ? " (Migrated)" : ""),
        migrated ? MigratedInvoiceHistory.detail(invoice) : invoice.getEndorsementNo(),
        refs,
        changes,
        null,
        migrated ? MigratedInvoiceHistory.MIGRATED : BOOKED,
        migrated ? "Migrated" : "Booked",
        true,
        List.copyOf(batches),
        order);
  }

  /** The booked invoice; none for a migrated invoice (a lookup would mark the transaction). */
  private Optional<BookedInvoice> booked(OpsInvoice invoice) {
    return invoice.getRecordOrigin().isMigrated()
        ? Optional.empty()
        : booked(invoice.getInvoiceNo());
  }

  private Optional<BookedInvoice> booked(String invoiceNo) {
    try {
      return Optional.of(bookings.byNo(invoiceNo));
    } catch (ResourceNotFoundException e) {
      return Optional.empty();
    }
  }

  /**
   * The signed amounts an invoice booked: premium and charges from the booked invoice (negative for
   * a return or cancellation invoice), the commission from its terms; the ledger components when
   * the booked invoice is not available (legacy invoices).
   */
  static Map<LedgerComponent, BigDecimal> invoiceChanges(OpsInvoice invoice, BookedInvoice booked) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    if (booked == null) {
      invoice.getComponents().forEach(c -> amounts.put(c.getComponent(), c.getBooked()));
      amounts.putIfAbsent(LedgerComponent.COMMISSION, invoice.getCommission());
      return signed(invoice.getKind(), amounts);
    }
    PremiumComponents p = booked.getPremium();
    amounts.put(LedgerComponent.BASIC, p.basic());
    amounts.put(LedgerComponent.DST, p.dst());
    amounts.put(LedgerComponent.PREMIUM_TAX_VAT, p.premiumTaxVat());
    amounts.put(LedgerComponent.LGT, p.lgt());
    amounts.put(LedgerComponent.FST, p.fst());
    amounts.put(LedgerComponent.OTHER, p.other());
    amounts.put(LedgerComponent.COMMISSION, booked.getCommission().commission());
    return signed(invoice.getKind(), amounts);
  }

  /** Return and cancellation invoices reduce the position, whatever sign they are stored with. */
  private static Map<LedgerComponent, BigDecimal> signed(
      InvoiceKind kind, Map<LedgerComponent, BigDecimal> amounts) {
    if (!kind.isNegative()) {
      return amounts;
    }
    Map<LedgerComponent, BigDecimal> negative = new EnumMap<>(LedgerComponent.class);
    amounts.forEach((c, v) -> negative.put(c, v == null ? BigDecimal.ZERO : v.abs().negate()));
    return negative;
  }

  private Optional<Journal> journal(Long companyId, String batchNo) {
    return journals.findByCompanyIdAndBatchNo(companyId, batchNo).map(PolicyTransactionService::of);
  }

  private static Journal of(JournalBatch batch) {
    List<JournalEntryLine> lines = new ArrayList<>();
    for (JournalLine l : batch.getLines()) {
      boolean debit = l.getSide() == BalanceSide.DEBIT;
      lines.add(
          new JournalEntryLine(
              l.getLineNo(),
              l.getAccount().getCode(),
              l.getAccount().getName(),
              debit ? l.getAmount() : null,
              debit ? null : l.getAmount(),
              l.getPartyCode(),
              l.getNarration()));
    }
    return new Journal(
        batch.getId(),
        batch.getBatchNo(),
        batch.getValueDate(),
        batch.getStatus().name(),
        batch.getNarration(),
        batch.getTotalDebit(),
        batch.getTotalCredit(),
        lines);
  }

  private static Kind kindOf(InvoiceKind kind) {
    return switch (kind) {
      case BOOKING -> Kind.BOOKING;
      case CANCELLATION -> Kind.CANCELLATION;
      case ENDORSEMENT_PLUS, ENDORSEMENT_MINUS -> Kind.ENDORSEMENT;
    };
  }

  private static String kindLabel(InvoiceKind kind) {
    return switch (kind) {
      case BOOKING -> "Original Booking";
      case ENDORSEMENT_PLUS -> "Endorsement – Additional Premium";
      case ENDORSEMENT_MINUS -> "Endorsement – Return Premium";
      case CANCELLATION -> "Cancellation";
    };
  }

  /**
   * A row before the position is computed.
   *
   * @param kind kind
   * @param date transaction date
   * @param effectiveDate effective date
   * @param typeLabel type as users read it
   * @param detail second line
   * @param refs references
   * @param changes signed change per component
   * @param balanceChange change of the outstanding balance only (write-offs), null otherwise
   * @param status status code
   * @param statusLabel status label
   * @param posted whether it moves the position
   * @param journalBatchNos GL journals
   * @param order order of discovery (ties)
   */
  private record Draft(
      Kind kind,
      LocalDate date,
      LocalDate effectiveDate,
      String typeLabel,
      String detail,
      Refs refs,
      Map<LedgerComponent, BigDecimal> changes,
      BigDecimal balanceChange,
      String status,
      String statusLabel,
      boolean posted,
      List<String> journalBatchNos,
      int order) {}

  /**
   * The history of a policy.
   *
   * @param rootInvoiceNo original invoice
   * @param arn Account Reference Number
   * @param policyNo insurer policy number, may be null
   * @param currency currency
   * @param rows transactions in order, the original booking first
   */
  public record PolicyTransactions(
      String rootInvoiceNo, String arn, String policyNo, String currency, List<Row> rows) {

    /** Defensive copy. */
    public PolicyTransactions {
      rows = List.copyOf(rows);
    }
  }

  /**
   * References of a transaction.
   *
   * @param invoiceNo invoice (booked, or the one the request is on)
   * @param endorsementNo booking endorsement number, may be null
   * @param requestNo request or file reference, may be null
   * @param requestId request id (screen link), may be null
   */
  public record Refs(String invoiceNo, String endorsementNo, String requestNo, Long requestId) {}

  /**
   * One transaction.
   *
   * @param seq position in the history, from 1
   * @param kind kind
   * @param date transaction date
   * @param effectiveDate effective date, may be null
   * @param typeLabel type as users read it
   * @param detail second line, may be null
   * @param refs references
   * @param change premium, tax, gross and commission change
   * @param after position after the transaction, null while it is not posted
   * @param balanceChange outstanding balance written off, null when not a write-off
   * @param status status code
   * @param statusLabel status label
   * @param posted whether the transaction is posted
   * @param journals GL journals with their lines
   */
  public record Row(
      int seq,
      Kind kind,
      LocalDate date,
      LocalDate effectiveDate,
      String typeLabel,
      String detail,
      Refs refs,
      Amounts change,
      Amounts after,
      BigDecimal balanceChange,
      String status,
      String statusLabel,
      boolean posted,
      List<Journal> journals) {

    /** Defensive copy. */
    public Row {
      journals = List.copyOf(journals);
    }
  }

  /**
   * Premium, taxes and charges, gross premium (premium plus taxes) and commission.
   *
   * @param premium basic premium
   * @param taxes DST, premium tax / VAT, LGT, FST and other charges
   * @param gross premium plus taxes
   * @param commission commission
   */
  public record Amounts(
      BigDecimal premium, BigDecimal taxes, BigDecimal gross, BigDecimal commission) {

    /** Nothing. */
    public static final Amounts ZERO =
        new Amounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    static Amounts of(Map<LedgerComponent, BigDecimal> changes) {
      BigDecimal premium = amount(changes, LedgerComponent.BASIC);
      BigDecimal taxes =
          TAXES.stream().map(c -> amount(changes, c)).reduce(BigDecimal.ZERO, BigDecimal::add);
      return new Amounts(
          premium, taxes, premium.add(taxes), amount(changes, LedgerComponent.COMMISSION));
    }

    private static BigDecimal amount(
        Map<LedgerComponent, BigDecimal> changes, LedgerComponent component) {
      BigDecimal value = changes.get(component);
      return value == null ? BigDecimal.ZERO : value;
    }

    Amounts plus(Amounts other) {
      return new Amounts(
          premium.add(other.premium),
          taxes.add(other.taxes),
          gross.add(other.gross),
          commission.add(other.commission));
    }
  }

  /**
   * A GL journal of a transaction.
   *
   * @param id journal id (journal page)
   * @param batchNo journal number
   * @param valueDate value date
   * @param status journal status
   * @param narration narration
   * @param totalDebit total debit
   * @param totalCredit total credit
   * @param lines lines
   */
  public record Journal(
      Long id,
      String batchNo,
      LocalDate valueDate,
      String status,
      String narration,
      BigDecimal totalDebit,
      BigDecimal totalCredit,
      List<JournalEntryLine> lines) {

    /** Defensive copy. */
    public Journal {
      lines = List.copyOf(lines);
    }
  }

  /**
   * A journal line.
   *
   * @param lineNo line number
   * @param accountCode GL account
   * @param accountName account name
   * @param debit debit amount, null on a credit line
   * @param credit credit amount, null on a debit line
   * @param partyCode sub-ledger party, may be null
   * @param narration narration, may be null
   */
  public record JournalEntryLine(
      int lineNo,
      String accountCode,
      String accountName,
      BigDecimal debit,
      BigDecimal credit,
      String partyCode,
      String narration) {}
}
