package com.iortatechnxt.brokerverse.receivables.seed;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.receivables.api.dto.DateRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.DepositSlipRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.brokerverse.receivables.domain.DepositSlip;
import com.iortatechnxt.brokerverse.receivables.domain.PdcStatus;
import com.iortatechnxt.brokerverse.receivables.domain.PostDatedCheque;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.brokerverse.receivables.service.DepositService;
import com.iortatechnxt.brokerverse.receivables.service.PdcService;
import com.iortatechnxt.brokerverse.receivables.service.ReceiptPostingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seed banking activity: weekly deposit slips, one bounced cheque, post-dated cheques banked and
 * cleared at maturity (one returned), and monthly rent cheques paid from the main bank account (the
 * September cheque is still unpresented at the bank).
 */
@Component
@Profile("seed")
public class SeedBanking {

  /** Cheque number of the rent paid in month m is {@code RENT_CHEQUE + m}. */
  static final int RENT_CHEQUE = 700_200;

  /** Day of the month the rent cheque is issued. */
  static final int RENT_DAY = 18;

  private static final Logger LOG = LoggerFactory.getLogger(SeedBanking.class);
  private static final LocalDate FIRST_SLIP = LocalDate.of(2026, 1, 9);
  private static final LocalDate LAST_SLIP = LocalDate.of(2026, 9, 16);
  private static final int YEAR = 2026;
  private static final int WEEK = 7;
  private static final int CLEARING_DAYS = 3;
  private static final int BOUNCE_MONTH = 7;
  private static final int MONTHS = 9;
  private static final BigDecimal RENT = new BigDecimal("185000.00");

  private final DepositService deposits;
  private final ReceiptPostingService posting;
  private final PdcService pdcs;
  private final AccountingEventPublisher publisher;

  /**
   * Creates the builder.
   *
   * @param deposits deposit service
   * @param posting receipt posting service
   * @param pdcs PDC service
   * @param publisher accounting engine
   */
  public SeedBanking(
      DepositService deposits,
      ReceiptPostingService posting,
      PdcService pdcs,
      AccountingEventPublisher publisher) {
    this.deposits = deposits;
    this.posting = posting;
    this.pdcs = pdcs;
    this.publisher = publisher;
  }

  /**
   * Prepares and confirms a deposit slip every week per bank account and currency; receipts of the
   * last week of September stay undeposited.
   *
   * @param ctx seed context
   * @return slips deposited
   */
  public int depositWeekly(SeedContext ctx) {
    int slips = 0;
    for (LocalDate d = FIRST_SLIP; !d.isAfter(LAST_SLIP); d = d.plusDays(WEEK)) {
      LocalDate slipDate = d;
      Map<String, List<Receipt>> byBank =
          deposits.undeposited(ctx.companyId(), null).stream()
              .filter(r -> !r.getReceiptDate().isAfter(slipDate))
              .collect(Collectors.groupingBy(r -> r.getBankAccountCode() + "|" + r.getCurrency()));
      for (List<Receipt> group : byBank.values()) {
        SeedContext.as(
            SeedContext.MAKER,
            () -> {
              DepositSlip slip =
                  deposits.create(
                      new DepositSlipRequest(
                          ctx.companyId(),
                          ctx.headOfficeId(),
                          group.get(0).getBankAccountCode(),
                          slipDate,
                          group.stream().map(Receipt::getId).toList()));
              return deposits.confirm(slip.getId(), new DateRequest(slipDate, "Deposited"));
            });
        slips++;
      }
    }
    return slips;
  }

  /**
   * Records one bounced cheque: the first cheque receipt deposited in July.
   *
   * @param receipts seed receipts
   * @return the bounced receipt, if any
   */
  public Receipt bounceOne(List<Receipt> receipts) {
    return receipts.stream()
        .filter(r -> r.getMode() == ReceiptMode.CHEQUE)
        .filter(r -> r.getReceiptDate().getMonthValue() == BOUNCE_MONTH)
        .filter(r -> r.getPartyCode() != null)
        .findFirst()
        .map(
            r ->
                SeedContext.as(
                    SeedContext.CHECKER,
                    () ->
                        posting.bounce(
                            r.getId(),
                            new ReversalRequest(
                                r.getReceiptDate().plusDays(WEEK + CLEARING_DAYS),
                                "Drawer's account closed (seed)"))))
        .orElse(null);
  }

  /**
   * Banks the post-dated cheques that matured before the seed cut-off, approves their receipts and
   * clears them; the first one is returned to the customer instead.
   *
   * @param ctx seed context
   * @return cheques processed
   */
  public int processPdcs(SeedContext ctx) {
    List<PostDatedCheque> held =
        pdcs.list(ctx.companyId(), null).stream()
            .filter(p -> p.getStatus() == PdcStatus.ON_HAND)
            .filter(p -> !p.getChequeDate().isAfter(SeedContext.LAST_DATE))
            .sorted((a, b) -> a.getChequeDate().compareTo(b.getChequeDate()))
            .toList();
    int processed = 0;
    for (PostDatedCheque p : held) {
      try {
        bank(p, processed == 0);
        processed++;
      } catch (BusinessRuleException ex) {
        LOG.warn("SIT PDC {} skipped: {}", p.getPdcNo(), ex.getMessage());
      }
    }
    return processed;
  }

  private void bank(PostDatedCheque p, boolean returnIt) {
    LocalDate due = p.getChequeDate();
    if (returnIt) {
      SeedContext.as(
          SeedContext.MAKER,
          () ->
              pdcs.returnCheque(p.getId(), new ReversalRequest(due, "Customer paid by transfer")));
      return;
    }
    SeedContext.as(SeedContext.MAKER, () -> pdcs.markDue(p.getCompanyId(), due));
    Receipt receipt =
        SeedContext.as(
            SeedContext.MAKER, () -> pdcs.deposit(p.getId(), new DateRequest(due, null)));
    SeedContext.as(SeedContext.CHECKER, () -> posting.approve(receipt.getId()));
    LocalDate cleared = due.plusDays(CLEARING_DAYS);
    if (!cleared.isAfter(SeedContext.LAST_DATE)) {
      SeedContext.as(
          SeedContext.MAKER, () -> pdcs.clear(p.getId(), new DateRequest(cleared, "Realised")));
    }
  }

  /**
   * Pays the monthly office rent by cheque from the main bank account (MISC_PAYMENT events).
   *
   * @param ctx seed context
   */
  public void payRent(SeedContext ctx) {
    for (int m = 1; m <= MONTHS; m++) {
      String cheque = String.valueOf(RENT_CHEQUE + m);
      SeedContext.as(
          SeedContext.MAKER,
          () ->
              publisher.publish(
                  new BusinessEvent(
                      "MISC_PAYMENT",
                      ctx.companyId(),
                      ctx.headOfficeId(),
                      LocalDate.of(YEAR, Integer.parseInt(cheque) - RENT_CHEQUE, RENT_DAY),
                      "PHP",
                      "RECEIVABLES_SEED",
                      "SEED-RENT:" + cheque,
                      cheque,
                      null,
                      null,
                      "FIN",
                      "Office rent - cheque " + cheque,
                      Map.of("AMOUNT", RENT),
                      Map.of("BANK", SeedContext.BANK, "EXPENSE", "5603"))));
    }
  }
}
