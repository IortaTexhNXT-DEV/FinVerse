package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition.Execution;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionTypeRule;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionTypeRuleRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPosting.PostingContext;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.IssuedReceipt;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.Payee;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.ReceiptLine;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.ReceiptRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.Source;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Income dispositions of unapplied payments (action INCOME; handling fee of submitted policies,
 * BRIDSP-31, SUBMITTED_POLICIES_DESIGN sections 3.6 and 5 rows 1-2): the payment leaves unapplied
 * collections as BDOI income through the event of its disposition type (for example {@code
 * SBM_HANDLING_FEE}: Dr unapplied collections, Cr income, Cr output VAT, the VAT included in the
 * amount at the rate of the type) and an official receipt of the type's OR type is issued as a
 * no-cash settlement document, so it posts no second income. A reversal posts the event back and
 * restores the item; the OR is then cancelled on the receipt screen.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class IncomeDispositions {

  private static final int RATE_SCALE = 10;

  private final DispositionTypeRuleRepository rules;
  private final CashieringPosting posting;
  private final ReceiptIssuer receipts;
  private final Clock clock;

  /**
   * Creates the component.
   *
   * @param rules disposition type rules
   * @param posting accounting events
   * @param receipts official receipts (cashiering's own issuer)
   * @param clock clock
   */
  public IncomeDispositions(
      DispositionTypeRuleRepository rules,
      CashieringPosting posting,
      ReceiptIssuer receipts,
      Clock clock) {
    this.rules = rules;
    this.posting = posting;
    this.receipts = receipts;
    this.clock = clock;
  }

  /**
   * Recognises the amount of the disposition as income and issues the official receipt.
   *
   * @param item unapplied item
   * @param d income disposition
   * @param ref source reference of the posting
   * @return execution with the journal and the OR number
   */
  public Execution recognise(Unapplied item, Disposition d, String ref) {
    DispositionTypeRule rule = rule(d);
    Split split = split(d.getAmount(), rule.getVatRate());
    LocalDate today = BusinessClock.today(clock);
    String batch =
        posting.publish(
            context(item, today, rule.getDescription()),
            rule.getIncomeEvent(),
            ref,
            Map.of(
                CashieringPosting.AMOUNT,
                d.getAmount(),
                "INCOME",
                split.income(),
                "OUTPUT_VAT",
                split.vat()));
    item.consume(d.getAmount());
    IssuedReceipt or =
        receipts.issueOfficialReceipt(
            new ReceiptRequest(
                item.getCompanyId(),
                rule.getOrType(),
                new Payee(item.getClientCode(), item.getPayorName()),
                item.getCurrency(),
                today,
                List.of(
                    new ReceiptLine(
                        null,
                        null,
                        split.income(),
                        split.vat(),
                        BigDecimal.ZERO,
                        rule.getDescription() + " " + item.getReference())),
                new Source(CashieringSettings.MODULE, ref, null, d.getRemarks())));
    return new Execution(null, null, batch, null, null, or.receiptNo());
  }

  /**
   * Undoes the income: the event is posted back and the item is unapplied again.
   *
   * @param item unapplied item
   * @param d completed income disposition
   * @param ref source reference of the reversal
   */
  public void reverse(Unapplied item, Disposition d, String ref) {
    DispositionTypeRule rule = rule(d);
    Split split = split(d.getAmount(), rule.getVatRate());
    posting.publish(
        context(item, BusinessClock.today(clock), rule.getDescription() + " reversed"),
        rule.getIncomeEvent(),
        ref,
        Map.of(
            CashieringPosting.AMOUNT,
            d.getAmount().negate(),
            "INCOME",
            split.income().negate(),
            "OUTPUT_VAT",
            split.vat().negate()));
    item.restore(d.getAmount());
  }

  private DispositionTypeRule rule(Disposition d) {
    DispositionTypeRule rule =
        rules
            .findById(d.getDispositionType())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "DISPOSITION_TYPE_NOT_CONFIGURED",
                        "Disposition type " + d.getDispositionType() + " has no processing rule"));
    if (rule.getIncomeEvent() == null || rule.getOrType() == null) {
      throw new BusinessRuleException(
          "INCOME_DISPOSITION_NOT_CONFIGURED",
          "The income disposition "
              + rule.getDescription()
              + " has no accounting event or receipt type; ask the administrator to set them up");
    }
    return rule;
  }

  /**
   * Income and output VAT of an amount that includes the VAT.
   *
   * @param amount amount received
   * @param rate VAT rate, null for none
   * @return split
   */
  static Split split(BigDecimal amount, BigDecimal rate) {
    if (rate == null || rate.signum() == 0) {
      return new Split(amount, BigDecimal.ZERO);
    }
    BigDecimal vat =
        Money.round(
            amount
                .multiply(rate)
                .divide(BigDecimal.ONE.add(rate), RATE_SCALE, RoundingMode.HALF_UP));
    return new Split(amount.subtract(vat), vat);
  }

  private static PostingContext context(Unapplied item, LocalDate date, String narration) {
    return new PostingContext(
        item.getCompanyId(),
        item.getBranchId(),
        date,
        item.getCurrency(),
        item.getReference(),
        item.getClientCode(),
        null,
        null,
        narration + " " + item.getReference(),
        null);
  }

  /**
   * Income and VAT of an amount.
   *
   * @param income income without VAT
   * @param vat output VAT
   */
  record Split(BigDecimal income, BigDecimal vat) {}
}
