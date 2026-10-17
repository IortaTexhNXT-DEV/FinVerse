package com.iortatechnxt.brokerverse.cashiering;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.EntryType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordParty;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender.RecordCheck;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster.Outcome;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordService;
import com.iortatechnxt.brokerverse.cashiering.service.RecordValidation.Draft;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/** Builds and posts BDOI's AR / OR creation records for the cashiering tests. */
@Component
public class RecordFixtures {

  /** The PHP bank account of BDOI's list. */
  public static final String PHP_BANK = "DFLB_SAVINGS_ORTIGAS";

  private final CashFixtures fx;
  private final ReceiptRecordService records;
  private final ReceiptRecordPoster poster;
  private final SystemParameterService parameters;
  private final AsUser as;

  RecordFixtures(
      CashFixtures fx,
      ReceiptRecordService records,
      ReceiptRecordPoster poster,
      SystemParameterService parameters,
      AsUser as) {
    this.fx = fx;
    this.records = records;
    this.poster = poster;
    this.parameters = parameters;
    this.as = as;
  }

  /** A premium AR record of a client paying in cash for some accounts. */
  public Draft ar(BigDecimal amount, RecordAccount... accounts) {
    return ar(TenderType.CASH, null, amount, accounts);
  }

  /** A premium AR record with a payment type and a check date. */
  public Draft ar(
      TenderType type, LocalDate checkDate, BigDecimal amount, RecordAccount... accounts) {
    return new Draft(
        ReceiptKind.AR,
        "PREMIUM",
        fx.ho(),
        new RecordParty(
            EntryType.CLIENT, null, "Record Client", null, null, null, "Record Client Payor"),
        new RecordTender(
            type,
            "PHP",
            PHP_BANK,
            amount,
            null,
            null,
            null,
            type == TenderType.CHECK ? new RecordCheck("CHK-77001", checkDate, "BDO") : null,
            today(),
            "Payment received at the counter",
            null),
        List.of(accounts));
  }

  /** An OR record of an insurer. */
  public Draft or(String type, String insurer, BigDecimal amount, BigDecimal vat, BigDecimal wtax) {
    return new Draft(
        ReceiptKind.OR,
        type,
        fx.ho(),
        new RecordParty(
            EntryType.INSURER,
            null,
            null,
            insurer,
            "Insurer " + insurer,
            "BDO",
            "Insurer " + insurer),
        new RecordTender(
            TenderType.CASH,
            "PHP",
            PHP_BANK,
            amount,
            vat,
            wtax,
            wtax.signum() > 0 ? "2307-REC-1" : null,
            null,
            today(),
            "Official receipt for " + type,
            null),
        List.of());
  }

  /** Saves and submits a record as the cashier. */
  public ReceiptRecord submitted(Draft draft) {
    ReceiptRecord saved = as.run("cashier", () -> records.create(fx.company(), draft));
    return as.run("cashier", () -> records.submit(saved.getId()));
  }

  /** Posts records as the Cashiering Team Leader. */
  public List<Outcome> post(Long... ids) {
    return as.run("cashtl", () -> poster.post(List.of(ids)));
  }

  /** Changes a setting as the System Administrator. */
  public void setting(String key, String value) {
    as.run("admin", () -> parameters.update(key, value));
  }

  /** An account of a record. */
  public static RecordAccount account(String reference, BigDecimal amount) {
    return new RecordAccount(reference, amount);
  }

  /** The business day. */
  public static LocalDate today() {
    return com.iortatechnxt.brokerverse.common.time.BusinessClock.today(Clock.systemUTC());
  }
}
