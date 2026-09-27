package com.iortatechnxt.brokerverse.receivables.seed;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.receivables.api.dto.AllocationRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ApplyRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.PdcRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReceiptRequest;
import com.iortatechnxt.brokerverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.brokerverse.receivables.domain.PayerType;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.brokerverse.receivables.service.AllocationPlanner;
import com.iortatechnxt.brokerverse.receivables.service.PdcService;
import com.iortatechnxt.brokerverse.receivables.service.ReceiptPostingService;
import com.iortatechnxt.brokerverse.receivables.service.ReceiptService;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seed official receipts January to September 2026: full, partial, FIFO and on-account payments by
 * cheque, transfer, cash and card, US dollar collections, reinsurer and other income receipts, and
 * post-dated cheques. Receipts are entered by the accountant and approved by the checker.
 */
@Component
@Profile("seed")
public class SeedCollections {

  private static final Logger LOG = LoggerFactory.getLogger(SeedCollections.class);

  private static final int YEAR = 2026;
  private static final int MONTHS = 9;
  private static final int SEPTEMBER = 9;
  private static final int FIRST_DAY = 2;
  private static final int DAY_STEP = 3;
  private static final int SEPTEMBER_STEP = 2;
  private static final int LAST_DAY = 28;
  private static final int PENDING_IN_SEPTEMBER = 2;
  private static final int QUARTER = 3;
  private static final int PDC_DAYS = 30;
  private static final int CHEQUE_BASE = 104_200;
  private static final BigDecimal PARTIAL = new BigDecimal("0.60");
  private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
  private static final BigDecimal ON_ACCOUNT = BigDecimal.valueOf(10_000);
  private static final BigDecimal OTHER_INCOME = BigDecimal.valueOf(3_750);
  private static final BigDecimal RI_AMOUNT = BigDecimal.valueOf(45_000);
  private static final List<String> DRAWEES =
      List.of("BPI", "Metrobank", "Security Bank", "Landbank", "UnionBank", "RCBC");
  private static final String PHP = "PHP";

  /** Kinds of seed receipts entered each month, in date order. */
  private enum Kind {
    CHEQUE_FULL,
    TRANSFER_FULL,
    CASH_FULL,
    PARTIAL,
    FIFO,
    DOLLAR_OR_CARD,
    ON_ACCOUNT,
    POST_DATED,
    OTHER
  }

  private static final List<Kind> PLAN =
      List.of(
          Kind.CHEQUE_FULL,
          Kind.TRANSFER_FULL,
          Kind.CASH_FULL,
          Kind.PARTIAL,
          Kind.FIFO,
          Kind.DOLLAR_OR_CARD,
          Kind.ON_ACCOUNT,
          Kind.POST_DATED,
          Kind.OTHER,
          Kind.CHEQUE_FULL);

  private final ReceiptService receipts;
  private final ReceiptPostingService posting;
  private final PdcService pdcs;
  private final AllocationPlanner planner;
  private final PartyService parties;
  private final List<Receipt> created = new ArrayList<>();
  private int sequence;
  private SeedContext context;

  /**
   * Creates the builder.
   *
   * @param receipts receipt service
   * @param posting receipt posting service
   * @param pdcs PDC service
   * @param planner allocation planner
   * @param parties party service
   */
  public SeedCollections(
      ReceiptService receipts,
      ReceiptPostingService posting,
      PdcService pdcs,
      AllocationPlanner planner,
      PartyService parties) {
    this.receipts = receipts;
    this.posting = posting;
    this.pdcs = pdcs;
    this.planner = planner;
    this.parties = parties;
  }

  /**
   * Creates the receipts and post-dated cheques.
   *
   * @param ctx seed context
   * @return receipts created
   */
  public List<Receipt> run(SeedContext ctx) {
    context = ctx;
    created.clear();
    for (int m = 1; m <= MONTHS; m++) {
      for (int k = 0; k < PLAN.size(); k++) {
        int day = m == SEPTEMBER ? FIRST_DAY + SEPTEMBER_STEP * k : FIRST_DAY + DAY_STEP * k;
        LocalDate date = LocalDate.of(YEAR, m, Math.min(day, LAST_DAY));
        boolean approve = !(m == SEPTEMBER && k >= PLAN.size() - PENDING_IN_SEPTEMBER);
        try {
          receipt(PLAN.get(k), m, (m + k) % SeedDebitNotes.PHP_PARTIES.size(), date, approve);
        } catch (BusinessRuleException ex) {
          LOG.warn("Seed receipt {}/{} skipped: {}", m, k, ex.getMessage());
        }
      }
    }
    return List.copyOf(created);
  }

  private void receipt(Kind kind, int m, int partyIndex, LocalDate date, boolean approve) {
    String party = SeedDebitNotes.PHP_PARTIES.get(partyIndex);
    switch (kind) {
      case CHEQUE_FULL -> full(party, date, ReceiptMode.CHEQUE, SeedContext.BANK, approve);
      case TRANSFER_FULL -> full(party, date, ReceiptMode.BANK_TRANSFER, SeedContext.BANK, approve);
      case CASH_FULL -> full(party, date, ReceiptMode.CASH, SeedContext.SAVINGS, approve);
      case PARTIAL -> partial(party, date);
      case FIFO -> fifo(party, date);
      case DOLLAR_OR_CARD -> dollarOrCard(m, party, date);
      case ON_ACCOUNT -> onAccount(party, date);
      case POST_DATED -> pdc(party, date);
      default -> otherOrReinsurer(m, date, approve);
    }
  }

  private void full(String party, LocalDate date, ReceiptMode mode, String bank, boolean approve) {
    Optional<OpenItem> item = oldest(party, date, PHP);
    if (item.isEmpty()) {
      onAccount(party, date);
      return;
    }
    save(
        request(party, date, mode, bank, PHP, item.get().outstanding()).manual(item.get()),
        approve);
  }

  private void partial(String party, LocalDate date) {
    Optional<OpenItem> item = oldest(party, date, PHP);
    if (item.isEmpty()) {
      onAccount(party, date);
      return;
    }
    BigDecimal amount =
        item.get().outstanding().multiply(PARTIAL).setScale(2, RoundingMode.HALF_EVEN);
    save(
        request(party, date, ReceiptMode.CHEQUE, SeedContext.BANK, PHP, amount)
            .manualAmount(item.get(), amount),
        true);
  }

  private void fifo(String party, LocalDate date) {
    BigDecimal open =
        planner.openDebits(context.companyId(), party(party).getId(), PHP).stream()
            .filter(i -> !i.getDocumentDate().isAfter(date))
            .limit(2)
            .map(OpenItem::outstanding)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal amount = open.divide(THOUSAND, 0, RoundingMode.DOWN).multiply(THOUSAND);
    if (amount.signum() == 0) {
      onAccount(party, date);
      return;
    }
    save(
        request(party, date, ReceiptMode.BANK_TRANSFER, SeedContext.BANK, PHP, amount)
            .method(AllocationMethod.FIFO),
        true);
  }

  private void dollarOrCard(int m, String party, LocalDate date) {
    if (m % QUARTER == 0) {
      Optional<OpenItem> usd = oldest(SeedDebitNotes.USD_PARTY, date, "USD");
      if (usd.isPresent()) {
        save(
            request(
                    SeedDebitNotes.USD_PARTY,
                    date,
                    ReceiptMode.BANK_TRANSFER,
                    SeedContext.DOLLAR_BANK,
                    "USD",
                    usd.get().outstanding())
                .manual(usd.get()),
            true);
        return;
      }
    }
    full(party, date, ReceiptMode.CARD, SeedContext.BANK, true);
  }

  private void onAccount(String party, LocalDate date) {
    save(
        request(party, date, ReceiptMode.CHEQUE, SeedContext.BANK, PHP, ON_ACCOUNT)
            .method(AllocationMethod.NONE),
        true);
  }

  private void pdc(String party, LocalDate date) {
    Optional<OpenItem> item = oldest(party, date, PHP);
    BigDecimal amount = item.map(OpenItem::outstanding).orElse(ON_ACCOUNT);
    sequence++;
    SeedContext.as(
        SeedContext.MAKER,
        () ->
            pdcs.register(
                new PdcRequest(
                    context.companyId(),
                    context.branch(sequence),
                    date,
                    party,
                    "FND",
                    String.valueOf(CHEQUE_BASE + sequence),
                    date.plusDays(PDC_DAYS),
                    DRAWEES.get(sequence % DRAWEES.size()),
                    PHP,
                    amount,
                    SeedContext.BANK,
                    item.map(OpenItem::getId).orElse(null),
                    "Post-dated cheque for premium")));
  }

  private void otherOrReinsurer(int m, LocalDate date, boolean approve) {
    Draft draft =
        m % 2 == 0
            ? request("R-0001", date, ReceiptMode.BANK_TRANSFER, SeedContext.BANK, PHP, RI_AMOUNT)
                .method(AllocationMethod.FIFO)
            : request(null, date, ReceiptMode.CASH, SeedContext.BANK, PHP, OTHER_INCOME)
                .other("Sale of salvage (seed)", "4700");
    save(draft, approve);
  }

  /**
   * Applies money held on account of the first on-account receipt to the payer's debit notes.
   *
   * @param date application date
   */
  public void applyOnAccount(LocalDate date) {
    created.stream()
        .filter(r -> r.getAllocationMethod() == AllocationMethod.NONE)
        .filter(r -> r.getPayerType().usesPremiumDeposit())
        .findFirst()
        .ifPresent(
            r -> {
              try {
                SeedContext.as(
                    SeedContext.MAKER,
                    () ->
                        posting.applyUnapplied(
                            r.getId(), new ApplyRequest(date, AllocationMethod.FIFO, List.of())));
              } catch (BusinessRuleException ex) {
                LOG.warn("Seed application skipped: {}", ex.getMessage());
              }
            });
  }

  private void save(Draft draft, boolean approve) {
    Receipt receipt = SeedContext.as(SeedContext.MAKER, () -> receipts.create(draft.toRequest()));
    if (approve) {
      SeedContext.as(SeedContext.CHECKER, () -> posting.approve(receipt.getId()));
    }
    created.add(receipt);
  }

  private Optional<OpenItem> oldest(String party, LocalDate date, String currency) {
    return planner.openDebits(context.companyId(), party(party).getId(), currency).stream()
        .filter(i -> !i.getDocumentDate().isAfter(date))
        .findFirst();
  }

  private Party party(String code) {
    return parties.getByCode(context.companyId(), code);
  }

  private PartyType type(String code) {
    return party(code).getPartyType();
  }

  private Draft request(
      String party,
      LocalDate date,
      ReceiptMode mode,
      String bank,
      String currency,
      BigDecimal amount) {
    sequence++;
    boolean cheque = mode == ReceiptMode.CHEQUE;
    return new Draft(
        context.companyId(),
        context.branch(sequence),
        date,
        party == null ? PayerType.OTHER : PayerType.of(type(party)),
        party,
        mode,
        cheque ? String.valueOf(CHEQUE_BASE + sequence) : null,
        cheque ? DRAWEES.get(sequence % DRAWEES.size()) : null,
        bank,
        currency,
        amount,
        AllocationMethod.MANUAL,
        List.of(),
        null,
        null);
  }

  /** Receipt being prepared. */
  private record Draft(
      Long companyId,
      Long branchId,
      LocalDate date,
      PayerType payerType,
      String party,
      ReceiptMode mode,
      String chequeNo,
      String drawee,
      String bank,
      String currency,
      BigDecimal amount,
      AllocationMethod method,
      List<AllocationRequest> allocations,
      String payerName,
      String incomeAccount) {

    Draft manual(OpenItem item) {
      return manualAmount(item, item.outstanding().min(amount));
    }

    Draft manualAmount(OpenItem item, BigDecimal value) {
      return new Draft(
          companyId,
          branchId,
          date,
          payerType,
          party,
          mode,
          chequeNo,
          drawee,
          bank,
          currency,
          amount,
          AllocationMethod.MANUAL,
          List.of(new AllocationRequest(item.getId(), value)),
          payerName,
          incomeAccount);
    }

    Draft method(AllocationMethod m) {
      return new Draft(
          companyId,
          branchId,
          date,
          payerType,
          party,
          mode,
          chequeNo,
          drawee,
          bank,
          currency,
          amount,
          m,
          List.of(),
          payerName,
          incomeAccount);
    }

    Draft other(String name, String account) {
      return new Draft(
          companyId,
          branchId,
          date,
          payerType,
          party,
          mode,
          chequeNo,
          drawee,
          bank,
          currency,
          amount,
          AllocationMethod.NONE,
          List.of(),
          name,
          account);
    }

    ReceiptRequest toRequest() {
      return new ReceiptRequest(
          companyId,
          branchId,
          date,
          payerType,
          party,
          payerName,
          "FND",
          mode,
          chequeNo,
          chequeNo == null ? null : date,
          drawee,
          currency,
          amount,
          bank,
          incomeAccount,
          method,
          "Seed collection",
          allocations);
    }
  }
}
