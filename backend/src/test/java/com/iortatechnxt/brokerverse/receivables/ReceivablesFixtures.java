package com.iortatechnxt.brokerverse.receivables;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.receivables.api.dto.AllocationRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReceiptRequest;
import com.iortatechnxt.brokerverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.brokerverse.receivables.domain.PayerType;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.brokerverse.receivables.service.ReceiptPostingService;
import com.iortatechnxt.brokerverse.receivables.service.ReceiptService;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.support.TestParties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Test data builders for the receivables module (debit notes, receipts, balances). */
@Component
public class ReceivablesFixtures {

  private static final AtomicInteger SEQ = new AtomicInteger();

  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final PartyService parties;
  private final CurrencyService currencies;
  private final ReceiptService receipts;
  private final ReceiptPostingService posting;
  private final LedgerQueryService ledger;
  private final ChartOfAccountsService accounts;
  private final TransactionTemplate tx;
  private final AsUser as;
  private final TestData data;
  private final TestParties testParties;

  ReceivablesFixtures(
      AccountingEventPublisher publisher,
      OpenItemService openItems,
      PartyService parties,
      CurrencyService currencies,
      ReceiptService receipts,
      ReceiptPostingService posting,
      LedgerQueryService ledger,
      ChartOfAccountsService accounts,
      TransactionTemplate tx,
      AsUser as,
      TestData data,
      TestParties testParties) {
    this.publisher = publisher;
    this.openItems = openItems;
    this.parties = parties;
    this.currencies = currencies;
    this.receipts = receipts;
    this.posting = posting;
    this.ledger = ledger;
    this.accounts = accounts;
    this.tx = tx;
    this.as = as;
    this.data = data;
    this.testParties = testParties;
  }

  public Long company() {
    return data.company().getId();
  }

  public Long branch() {
    return data.branch("HO").getId();
  }

  /**
   * Creates and authorizes a new agent with no open items, for tests whose FIFO allocation must not
   * pick up items that other test classes left on the shared seed intermediaries.
   *
   * @return party code
   */
  public String newAgent() {
    return testParties.create(PartyType.AGENT).getCode();
  }

  /** Raises a debit note (POLICY_ISSUE journal + DEBIT open item). */
  public OpenItem debitNote(String partyCode, LocalDate date, String amount, String currency) {
    int n = SEQ.incrementAndGet();
    BigDecimal total = new BigDecimal(amount);
    return as.run(
        "uw",
        () ->
            tx.execute(
                s -> {
                  Party party = parties.getByCode(company(), partyCode);
                  String no = "DN-TEST-" + n + "-" + System.nanoTime() % 100_000;
                  var batch =
                      publisher.publish(
                          new BusinessEvent(
                              "POLICY_ISSUE",
                              company(),
                              branch(),
                              date,
                              currency,
                              "RCV_TEST",
                              no,
                              no,
                              partyCode,
                              "FIRE",
                              null,
                              "Test debit note",
                              Map.of("GROSS_PREMIUM", total, "TOTAL_DUE", total),
                              Map.of()));
                  return openItems.record(
                      new OpenItemValues(
                          company(),
                          branch(),
                          party.getId(),
                          partyCode,
                          ItemDirection.DEBIT,
                          "DEBIT_NOTE",
                          no,
                          date,
                          date.plusDays(party.getCreditDays()),
                          currency,
                          total,
                          currencies.toBase("PHP", currency, total, date),
                          "RCV_TEST",
                          no,
                          batch.getBatchNo(),
                          "Test debit note"));
                }));
  }

  /** Builds a receipt; complete it with {@link Draft#with}. */
  public Draft request(
      PayerType payerType,
      String partyCode,
      LocalDate date,
      ReceiptMode mode,
      String amount,
      String bank) {
    return new Draft(payerType, partyCode, date, mode, new BigDecimal(amount), bank);
  }

  /** Receipt being built by a test. */
  public final class Draft {
    private final PayerType payerType;
    private final String partyCode;
    private final LocalDate date;
    private final ReceiptMode mode;
    private final BigDecimal amount;
    private final String bank;

    Draft(
        PayerType payerType,
        String partyCode,
        LocalDate date,
        ReceiptMode mode,
        BigDecimal amount,
        String bank) {
      this.payerType = payerType;
      this.partyCode = partyCode;
      this.date = date;
      this.mode = mode;
      this.amount = amount;
      this.bank = bank;
    }

    /** Completes the request with its allocation. */
    public ReceiptRequest with(AllocationMethod method, List<AllocationRequest> allocations) {
      boolean cheque = mode == ReceiptMode.CHEQUE;
      boolean other = payerType == PayerType.OTHER;
      return new ReceiptRequest(
          company(),
          branch(),
          date,
          payerType,
          partyCode,
          other ? "Walk-in payer" : null,
          "FND",
          mode,
          cheque ? "CHQ" + SEQ.incrementAndGet() : null,
          cheque ? date : null,
          cheque ? "BPI" : null,
          "PHP",
          amount,
          bank,
          other ? "4700" : null,
          method,
          "test receipt",
          allocations);
    }
  }

  /** Creates a receipt as the accountant. */
  public Receipt create(ReceiptRequest request) {
    return as.run("accountant", () -> receipts.create(request));
  }

  /** Approves a receipt as the checker. */
  public Receipt approve(Receipt r) {
    return as.run("checker", () -> posting.approve(r.getId()));
  }

  /** Creates and approves. */
  public Receipt approved(ReceiptRequest request) {
    return approve(create(request));
  }

  /** Current outstanding amount of an open item. */
  public BigDecimal outstanding(OpenItem item) {
    return openItems.get(item.getId()).outstanding();
  }

  /** Net (debit positive) balance of a GL account as of the end of 2026. */
  public BigDecimal balance(String accountCode) {
    Long accountId = accounts.getByCode(company(), accountCode).getId();
    return ledger.netBalance(company(), accountId, null, LocalDate.of(2026, 12, 31));
  }

  /** Allocation list helper. */
  public static List<AllocationRequest> alloc(OpenItem item, String amount) {
    return List.of(new AllocationRequest(item.getId(), new BigDecimal(amount)));
  }
}
