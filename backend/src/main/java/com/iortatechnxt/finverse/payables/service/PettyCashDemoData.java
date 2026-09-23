package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.payables.domain.PettyCashDisbursement;
import com.iortatechnxt.finverse.payables.domain.PettyCashDisbursementValues;
import com.iortatechnxt.finverse.payables.domain.PettyCashFund;
import com.iortatechnxt.finverse.payables.domain.PettyCashReimbursement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Demo petty cash activity (called by {@link PayablesDemoData}): every fund is established in
 * January, pays three vouchers a month from January to September and is reimbursed at each month
 * end up to August, leaving September's vouchers pending reimbursement.
 */
@Component
public class PettyCashDemoData {

  private static final int YEAR = 2026;
  private static final int LAST_MONTH = 9;
  private static final int LAST_REIMBURSED_MONTH = 8;
  private static final int VOUCHERS_PER_MONTH = 3;
  private static final int FIRST_DAY = 6;
  private static final int DAY_STEP = 8;
  private static final int CLAIM_DAY = 28;
  private static final int ESTABLISH_DAY = 5;
  private static final long BASE_AMOUNT = 350;
  private static final long STEPS = 25;
  private static final long STEP = 100;
  private static final long MIX_MONTH = 31;
  private static final long MIX_VOUCHER = 17;
  private static final long MIX_FUND = 13;
  private static final long CENTS = 50;
  private static final List<String> ACCOUNTS = List.of("5606", "5608", "5604", "5607");
  private static final List<String> DESCRIPTIONS =
      List.of(
          "Taxi and jeepney fares",
          "Notarial and permit fees",
          "Mobile load and courier",
          "Minor office repairs");
  private static final List<String> PAYEES =
      List.of("Office messenger", "Admin assistant", "Courier rider", "Maintenance crew");
  private static final List<String> COST_CENTERS = List.of("FIN", "MKT", "UW");

  private final PettyCashFundService funds;
  private final PettyCashService pettyCash;
  private final DemoActor actor;

  /**
   * Creates the generator.
   *
   * @param funds fund service
   * @param pettyCash voucher service
   * @param actor demo users
   */
  public PettyCashDemoData(
      PettyCashFundService funds, PettyCashService pettyCash, DemoActor actor) {
    this.funds = funds;
    this.pettyCash = pettyCash;
    this.actor = actor;
  }

  /**
   * Generates the activity of every established-to-be fund of a company.
   *
   * @param companyId company
   * @return number of vouchers created
   */
  public int load(Long companyId) {
    int count = 0;
    List<PettyCashFund> all = funds.list(companyId);
    for (int f = 0; f < all.size(); f++) {
      PettyCashFund fund = all.get(f);
      if (!fund.isActive() || fund.getEstablishedOn() != null) {
        continue;
      }
      actor.as(
          DemoActor.CHECKER,
          () -> funds.establish(fund.getId(), LocalDate.of(YEAR, 1, ESTABLISH_DAY)));
      for (int month = 1; month <= LAST_MONTH; month++) {
        count += month(fund, f, month);
      }
    }
    return count;
  }

  private int month(PettyCashFund fund, int fundIndex, int month) {
    for (int k = 0; k < VOUCHERS_PER_MONTH; k++) {
      PettyCashDisbursementValues values = values(fundIndex, month, k);
      PettyCashDisbursement voucher =
          actor.as(DemoActor.MAKER, () -> pettyCash.disburse(fund.getId(), values));
      actor.as(DemoActor.CHECKER, () -> pettyCash.approveDisbursement(voucher.getId()));
    }
    if (month <= LAST_REIMBURSED_MONTH) {
      PettyCashReimbursement claim =
          actor.as(
              DemoActor.MAKER,
              () ->
                  pettyCash.claimReimbursement(
                      fund.getId(),
                      LocalDate.of(YEAR, month, CLAIM_DAY),
                      List.of(),
                      "Month-end replenishment " + YEAR + "-" + month));
      actor.as(DemoActor.CHECKER, () -> pettyCash.approveReimbursement(claim.getId()));
    }
    return VOUCHERS_PER_MONTH;
  }

  private static PettyCashDisbursementValues values(int fundIndex, int month, int k) {
    int kind = (month + k + fundIndex) % ACCOUNTS.size();
    long steps = (month * MIX_MONTH + k * MIX_VOUCHER + fundIndex * MIX_FUND) % STEPS;
    BigDecimal amount =
        BigDecimal.valueOf(BASE_AMOUNT + steps * STEP).add(BigDecimal.valueOf(k * CENTS, 2));
    return new PettyCashDisbursementValues(
        LocalDate.of(YEAR, month, FIRST_DAY + k * DAY_STEP),
        PAYEES.get(kind),
        ACCOUNTS.get(kind),
        COST_CENTERS.get(fundIndex % COST_CENTERS.size()),
        DESCRIPTIONS.get(kind),
        "OR-" + month + "-" + (fundIndex + 1) + (k + 1),
        amount);
  }
}
