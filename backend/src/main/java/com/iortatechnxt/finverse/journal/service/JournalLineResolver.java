package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.domain.JournalLineSpec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Turns line requests into resolved line values: looks up accounts, defaults currency/branch and
 * converts to base currency using the explicit rate or the SPOT rate on the value date.
 */
@Component
public class JournalLineResolver {

  private final GlAccountRepository accounts;
  private final CurrencyService currencies;

  /**
   * Creates the resolver.
   *
   * @param accounts account repository
   * @param currencies currency service
   */
  public JournalLineResolver(GlAccountRepository accounts, CurrencyService currencies) {
    this.accounts = accounts;
    this.currencies = currencies;
  }

  /**
   * Resolves lines.
   *
   * @param ctx header context
   * @param requests line requests
   * @return resolved lines
   */
  public List<JournalLineSpec> resolve(HeaderContext ctx, List<JournalLineRequest> requests) {
    Map<String, GlAccount> byCode =
        accounts
            .findByCompanyIdAndCodeIn(
                ctx.companyId(), requests.stream().map(JournalLineRequest::accountCode).toList())
            .stream()
            .collect(Collectors.toMap(GlAccount::getCode, Function.identity()));
    return requests.stream().map(r -> resolveLine(ctx, r, byCode)).toList();
  }

  private JournalLineSpec resolveLine(
      HeaderContext ctx, JournalLineRequest r, Map<String, GlAccount> byCode) {
    GlAccount account = byCode.get(r.accountCode());
    if (account == null) {
      throw new BusinessRuleException("UNKNOWN_ACCOUNT", "Unknown GL account " + r.accountCode());
    }
    String currency = r.currency() == null ? ctx.currency() : r.currency();
    BigDecimal rate =
        r.exchangeRate() != null
            ? r.exchangeRate()
            : currencies.rateOn(ctx.baseCurrency(), currency, RateType.SPOT, ctx.valueDate());
    if (currency.equals(ctx.baseCurrency())) {
      rate = BigDecimal.ONE;
    }
    BigDecimal amount = Money.round(r.amount());
    return new JournalLineSpec(
        account,
        r.branchId() == null ? ctx.branchId() : r.branchId(),
        r.side(),
        currency,
        amount,
        rate,
        Money.convert(amount, rate),
        blankToNull(r.costCenter()),
        blankToNull(r.businessLine()),
        blankToNull(r.partyCode()),
        blankToNull(r.reference()),
        blankToNull(r.narration()));
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  /**
   * Header facts needed to resolve lines.
   *
   * @param companyId company
   * @param branchId default branch
   * @param currency default currency
   * @param baseCurrency company base currency
   * @param valueDate value date for rate lookup
   */
  public record HeaderContext(
      Long companyId, Long branchId, String currency, String baseCurrency, LocalDate valueDate) {}
}
