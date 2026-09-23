package com.iortatechnxt.finverse.receivables.demo;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo debit notes for the receivables module when no policy administration data exists: a
 * POLICY_ISSUE journal plus a DEBIT open item per note, January to September 2026.
 */
@Component
@Profile("demo")
public class DemoDebitNotes {

  /** Clients and intermediaries receiving PHP debit notes. */
  static final List<String> PHP_PARTIES =
      List.of("C-000101", "C-000102", "C-000201", "C-000203", "C-000204", "A-0001", "B-0001");

  /** Client billed in US dollars. */
  static final String USD_PARTY = "C-000202";

  private static final String MODULE = "RECEIVABLES_DEMO";
  private static final String PHP = "PHP";
  private static final String USD = "USD";
  private static final int YEAR = 2026;
  private static final int MONTHS = 9;
  private static final int QUARTER = 3;
  private static final int DAY_SPREAD = 3;
  private static final int DAY_RANGE = 20;
  private static final int USD_DAY = 5;
  private static final int MONTH_FACTOR = 31;
  private static final int PARTY_FACTOR = 17;
  private static final int MIN_OPEN = 30;
  private static final int BASE_PREMIUM = 8_000;
  private static final int PREMIUM_STEP = 1_250;
  private static final int STEPS = 40;
  private static final BigDecimal DST_RATE = new BigDecimal("0.125");
  private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
  private static final List<String> LINES = List.of("FIRE", "MOTOR", "MARINE", "ENGG", "CASUALTY");

  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final PartyService parties;
  private final CurrencyService currencies;
  private final TransactionTemplate tx;

  /**
   * Creates the builder.
   *
   * @param publisher accounting engine
   * @param openItems open item service
   * @param parties party service
   * @param currencies currency service
   * @param tx transaction template
   */
  public DemoDebitNotes(
      AccountingEventPublisher publisher,
      OpenItemService openItems,
      PartyService parties,
      CurrencyService currencies,
      TransactionTemplate tx) {
    this.publisher = publisher;
    this.openItems = openItems;
    this.parties = parties;
    this.currencies = currencies;
    this.tx = tx;
  }

  /**
   * Creates debit notes unless enough open client debit items already exist (e.g. from the
   * underwriting demo).
   *
   * @param ctx demo context
   * @return number of debit notes created
   */
  public int ensure(DemoContext ctx) {
    long open =
        openItems.outstanding(ctx.companyId(), DemoContext.LAST_DATE).stream()
            .filter(i -> i.getDirection() == ItemDirection.DEBIT)
            .filter(i -> PHP_PARTIES.contains(i.getPartyCode()))
            .count();
    if (open >= MIN_OPEN) {
      return 0;
    }
    int created = 0;
    for (int m = 1; m <= MONTHS; m++) {
      for (int j = 0; j < PHP_PARTIES.size(); j++) {
        LocalDate date = LocalDate.of(YEAR, m, 1 + (j * DAY_SPREAD) % DAY_RANGE);
        create(ctx, created, PHP_PARTIES.get(j), date, PHP, premium(m, j));
        created++;
      }
      if (m % QUARTER == 0) {
        create(
            ctx,
            created,
            USD_PARTY,
            LocalDate.of(YEAR, m, USD_DAY),
            USD,
            premium(m, PHP_PARTIES.size()).movePointLeft(2));
        created++;
      }
    }
    return created;
  }

  private static BigDecimal premium(int month, int party) {
    return BigDecimal.valueOf(
        BASE_PREMIUM
            + (long) ((month * MONTH_FACTOR + party * PARTY_FACTOR) % STEPS) * PREMIUM_STEP);
  }

  private void create(
      DemoContext ctx, int n, String partyCode, LocalDate date, String currency, BigDecimal gross) {
    tx.executeWithoutResult(
        status -> {
          Party party = parties.getByCode(ctx.companyId(), partyCode);
          BigDecimal dst = Money.round(gross.multiply(DST_RATE));
          BigDecimal vat = Money.round(gross.multiply(VAT_RATE));
          BigDecimal total = gross.add(dst).add(vat);
          String no = String.format("DN-DEMO-2026-%04d", n + 1);
          String key = "DEMO-DN:" + (n + 1);
          JournalBatch batch =
              publisher.publish(
                  new BusinessEvent(
                      "POLICY_ISSUE",
                      ctx.companyId(),
                      ctx.branch(n),
                      date,
                      currency,
                      MODULE,
                      key,
                      no,
                      partyCode,
                      LINES.get(n % LINES.size()),
                      null,
                      "Premium debit note " + no + " - " + party.getName(),
                      Map.of("GROSS_PREMIUM", gross, "DST", dst, "VAT", vat, "TOTAL_DUE", total),
                      Map.of()));
          openItems.record(
              new OpenItemValues(
                  ctx.companyId(),
                  ctx.branch(n),
                  party.getId(),
                  partyCode,
                  ItemDirection.DEBIT,
                  "DEBIT_NOTE",
                  no,
                  date,
                  date.plusDays(party.getCreditDays()),
                  currency,
                  total,
                  currencies.toBase(PHP, currency, total, date),
                  MODULE,
                  key,
                  batch.getBatchNo(),
                  "Premium debit note " + no));
        });
  }
}
