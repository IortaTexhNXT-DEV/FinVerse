package com.iortatechnxt.brokerverse.disbursement;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest.RequestFacts;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee.PayeeDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount.AccountDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeQueryService;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeService;
import com.iortatechnxt.brokerverse.disbursement.service.RequestIntakeService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherActions;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherService;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Test data of the Disbursement module: payees, encoded requests and vouchers by stage. */
@Component
public class DisbursementFixtures {

  /** The demo insurer of the motor fixtures (remittance batches). */
  public static final String INSURER = "INS-MGIC";

  private static final AtomicLong DAYS = new AtomicLong(1000 + System.nanoTime() % 20000);

  static final String PROCESSOR = "disb";
  static final String LEADER = "disbtl";
  static final String APPROVER = "disbappr";

  private final OpsLedgerFixtures ledger;
  private final PayeeService payees;
  private final PayeeQueryService payeeQuery;
  private final RequestIntakeService intake;
  private final VoucherService vouchers;
  private final VoucherActions actions;
  private final TransactionTemplate tx;
  private final AsUser as;

  DisbursementFixtures(
      OpsLedgerFixtures ledger,
      PayeeService payees,
      PayeeQueryService payeeQuery,
      RequestIntakeService intake,
      VoucherService vouchers,
      VoucherActions actions,
      TransactionTemplate tx,
      AsUser as) {
    this.ledger = ledger;
    this.payees = payees;
    this.payeeQuery = payeeQuery;
    this.intake = intake;
    this.vouchers = vouchers;
    this.actions = actions;
    this.tx = tx;
    this.as = as;
  }

  /** The demo company. */
  public Long company() {
    return ledger.company();
  }

  /** Runs work as a user in a transaction. */
  public <T> T as(String user, Supplier<T> work) {
    return as.run(user, () -> tx.execute(s -> work.get()));
  }

  /** The active payee of a code, maintained by the team leader and authorised by the approver. */
  public Payee payee(String code, String klass, DisbursementMode mode, String accountNo) {
    var existing = as(LEADER, () -> payeeQuery.byCode(company(), code));
    if (existing.isPresent() && existing.get().getStage().usable()) {
      return existing.get();
    }
    List<AccountDetails> accounts =
        accountNo == null
            ? List.of()
            : List.of(
                new AccountDetails(
                    "BDO Unibank, Inc.", "Makati", accountNo, "Payee " + code, "PHP", mode, true));
    Payee draft =
        as(
            LEADER,
            () ->
                payees.create(
                    company(),
                    code,
                    details(klass, "Payee " + code, mode),
                    mode == DisbursementMode.CHECK ? List.of() : accounts,
                    PayeeSource.MANUAL));
    as(LEADER, () -> payees.submit(draft.getId()));
    return as(APPROVER, () -> payees.authorize(draft.getId()));
  }

  /** Payee details. */
  public static PayeeDetails details(String klass, String name, DisbursementMode mode) {
    return new PayeeDetails(
        klass,
        name,
        "Makati City",
        "payee@brokerverse-demo.ph",
        null,
        mode,
        List.of(mode, DisbursementMode.CHECK, DisbursementMode.CTA).stream().distinct().toList(),
        List.of(),
        "PHP",
        null,
        null);
  }

  /** A client payee paid by credit to account. */
  public Payee clientPayee() {
    return payee("CLT-" + BookingFixtures.token(), "CLIENT", DisbursementMode.CTA, "001234567890");
  }

  /** A supplier payee paid by check. */
  public Payee supplierPayee() {
    return payee("SUP-" + BookingFixtures.token(), "SUPPLIER", DisbursementMode.CHECK, null);
  }

  /** A request encoded by the processor: its voucher is IN_PROCESS. */
  public IntakeRequest encoded(String type, Payee payee, String amount) {
    return as(
        PROCESSOR,
        () ->
            intake.register(
                new RequestFacts(
                    company(),
                    RequestSource.ENCODED,
                    "DISBURSEMENT",
                    null,
                    "RFP-" + BookingFixtures.token(),
                    type,
                    null,
                    payee.getPayeeCode(),
                    null,
                    "PHP",
                    new BigDecimal(amount),
                    "Test " + type,
                    null,
                    List.of(),
                    List.of(),
                    false,
                    null,
                    null,
                    null,
                    null)));
  }

  /** The voucher of a request. */
  public Voucher voucherOf(IntakeRequest request) {
    return as(PROCESSOR, () -> vouchers.get(intake.get(request.getId()).getVoucherId()));
  }

  /** A voucher reloaded. */
  public Voucher voucher(Long id) {
    return as(PROCESSOR, () -> vouchers.get(id));
  }

  /** An encoded voucher taken through review to approval. */
  public Voucher approved(String type, Payee payee, String amount) {
    Voucher v = voucherOf(encoded(type, payee, amount));
    as(PROCESSOR, () -> actions.submit(v.getId(), "Complete"));
    as(LEADER, () -> actions.submitForApproval(v.getId(), "Checked"));
    return as(APPROVER, () -> actions.approve(v.getId(), "Approved"));
  }

  /** A business date no other test uses (end of day runs once per date). */
  public static LocalDate uniqueDate() {
    return LocalDate.now().plusDays(DAYS.incrementAndGet());
  }
}
