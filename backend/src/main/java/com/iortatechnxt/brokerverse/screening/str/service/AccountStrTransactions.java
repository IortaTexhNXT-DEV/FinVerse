package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link StrTransactionSource} (SNSRP-705): the client's placed, issued and booked accounts
 * with a gross premium, as reference (ARN), period start, gross premium and currency.
 */
@Component
@Transactional(readOnly = true)
public class AccountStrTransactions implements StrTransactionSource {

  private static final Set<AccountStatus> REPORTED =
      Set.of(
          AccountStatus.AWAITING_PAYMENT,
          AccountStatus.READY_FOR_PLACEMENT,
          AccountStatus.PLACED,
          AccountStatus.POLICY_ISSUED,
          AccountStatus.BOOKED);

  private final AccountQueryService accounts;

  /**
   * Creates the adapter.
   *
   * @param accounts account reads
   */
  public AccountStrTransactions(AccountQueryService accounts) {
    this.accounts = accounts;
  }

  @Override
  public List<Line> transactionsOf(Long clientId) {
    return accounts.byClient(clientId).stream()
        .filter(a -> REPORTED.contains(a.getStatus()))
        .filter(a -> a.getPremium() != null && positive(a.getPremium().grossPremium()))
        .map(AccountStrTransactions::line)
        .toList();
  }

  private static boolean positive(BigDecimal amount) {
    return amount != null && amount.signum() > 0;
  }

  private static Line line(Account a) {
    LocalDate date =
        Objects.requireNonNullElse(
            a.getPeriodFrom(), LocalDate.ofInstant(a.getCreatedAt(), ZoneOffset.UTC));
    return new Line(
        a.getArn(),
        date,
        a.getPremium().grossPremium().setScale(2, RoundingMode.HALF_UP),
        a.getCurrency() == null ? "PHP" : a.getCurrency(),
        "ACCOUNT",
        a.getProductCode() + (a.getInsurerCode() == null ? "" : " - " + a.getInsurerCode()));
  }
}
