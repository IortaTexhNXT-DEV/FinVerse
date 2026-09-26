package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.Reversal;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's implementation of the ledger port {@code PaymentReapplier} (ADJID.009/012/013,
 * OPERATIONS_DESIGN section 5 row 17): after an endorsement changed an invoice's premium, every
 * active application of the invoice is reversed and the same money is applied again, receipt by
 * receipt, through the component hierarchy (CSHID.022). What no longer fits becomes one unapplied
 * item (origin REAPPLY) for disposition: net Dr PR (1210.x) / Cr unapplied collections (2205), with
 * the realized commission of the reversed applications reversed too. Runs inside the adjustment's
 * posting transaction and is idempotent on (source module, source reference, invoice).
 *
 * <p>Adjustment calls it as {@code ReapplyRequest(invoiceNo, "ADJUSTMENT", "ADJ:<requestNo>",
 * valueDate, reason)} once the original invoice is unlocked and its ADJUSTED movement posted, so
 * the balances are read from the original invoice. {@link ReapplyResult#none(String)} is returned
 * when the invoice has no active application; this implementation never throws {@code
 * PAYMENT_REAPPLIER_UNAVAILABLE}.
 */
@Service
@Transactional
public class CashieringPaymentReapplier implements PaymentReapplier {

  private static final String FIND =
      "select unapplied, excess, receipt_nos, unapplied_ref from csh_reapplication"
          + " where source_module = ? and source_ref = ? and invoice_no = ?";
  private static final String SAVE =
      "insert into csh_reapplication (company_id, invoice_no, source_module, source_ref, reason,"
          + " unapplied, excess, receipt_nos, unapplied_ref, created_at, created_by)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private final ApplicationRepository applications;
  private final CashReceiptRepository receipts;
  private final ApplicationService applier;
  private final UnappliedService unapplied;
  private final InvoiceLedgerQueryService ledger;
  private final JdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the re-applier.
   *
   * @param applications applications
   * @param receipts receipts
   * @param applier application engine
   * @param unapplied unapplied workbench
   * @param ledger invoice ledger
   * @param jdbc JDBC
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CashieringPaymentReapplier(
      ApplicationRepository applications,
      CashReceiptRepository receipts,
      ApplicationService applier,
      UnappliedService unapplied,
      InvoiceLedgerQueryService ledger,
      JdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.applications = applications;
    this.receipts = receipts;
    this.applier = applier;
    this.unapplied = unapplied;
    this.ledger = ledger;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public ReapplyResult reapply(ReapplyRequest request) {
    Optional<ReapplyResult> earlier = earlier(request);
    if (earlier.isPresent()) {
      return earlier.get();
    }
    OpsInvoice invoice = ledger.require(request.invoiceNo());
    invoice.loadCollections();
    List<Application> active =
        applications.findByInvoiceNoAndStatusOrderByIdAsc(request.invoiceNo(), Application.ACTIVE);
    if (active.isEmpty()) {
      ReapplyResult none = ReapplyResult.none(request.invoiceNo());
      save(invoice, request, none);
      return none;
    }
    String tag = ":RA:" + request.sourceModule() + ":" + request.sourceRef();
    BigDecimal taken = BigDecimal.ZERO;
    for (Application app : active) {
      applier.reverse(app, invoice, new Reversal(app.reference() + tag, request.reason(), true));
      taken = taken.add(app.getAmount());
    }
    BigDecimal excess = BigDecimal.ZERO;
    Set<String> receiptNos = new LinkedHashSet<>();
    for (Application app : active) {
      BigDecimal applied =
          applier
              .apply(
                  invoice,
                  app.getAmount(),
                  new Application.Origin(
                      app.getReceiptId(),
                      app.getUnappliedId(),
                      ApplicationSource.REAPPLY,
                      request.sourceModule() + ":" + request.sourceRef()),
                  new ApplyOptions(request.valueDate(), false, null))
              .map(Application::getAmount)
              .orElse(BigDecimal.ZERO);
      excess = excess.add(app.getAmount().subtract(applied));
      receiptNo(app).ifPresent(receiptNos::add);
    }
    String unappliedRef =
        excess.signum() > 0 ? toUnapplied(invoice, request, excess).getReference() : null;
    ReapplyResult result =
        new ReapplyResult(
            request.invoiceNo(), taken, excess, new ArrayList<>(receiptNos), unappliedRef);
    save(invoice, request, result);
    audit.record(
        UnappliedService.ENTITY,
        request.invoiceNo(),
        AuditAction.REVERSE,
        "Re-applied "
            + taken
            + " for "
            + request.sourceModule()
            + " "
            + request.sourceRef()
            + ", excess "
            + excess);
    return result;
  }

  private Unapplied toUnapplied(OpsInvoice invoice, ReapplyRequest request, BigDecimal excess) {
    return unapplied.create(
        invoice.getCompanyId(),
        invoice.getBranchId(),
        new UnappliedSpec(
            UnappliedOrigin.REAPPLY,
            null,
            null,
            invoice.getInvoiceNo(),
            invoice.getClientCode(),
            invoice.getPayorName() != null ? invoice.getPayorName() : invoice.getAssuredName(),
            invoice.getClassification().salesUnit(),
            invoice.getCurrency(),
            excess,
            "REFUND",
            CashieringSettings.MODULE,
            "RA:"
                + request.sourceModule()
                + ":"
                + request.sourceRef()
                + ":"
                + invoice.getInvoiceNo(),
            request.reason()));
  }

  private Optional<String> receiptNo(Application app) {
    return app.getReceiptId() == null
        ? Optional.empty()
        : receipts.findById(app.getReceiptId()).map(r -> r.getReceiptNo());
  }

  private static List<String> split(String csv) {
    return csv == null || csv.isEmpty() ? List.of() : Arrays.asList(csv.split(","));
  }

  private Optional<ReapplyResult> earlier(ReapplyRequest r) {
    return jdbc
        .query(
            FIND,
            (rs, i) ->
                new ReapplyResult(
                    r.invoiceNo(),
                    rs.getBigDecimal("unapplied"),
                    rs.getBigDecimal("excess"),
                    split(rs.getString("receipt_nos")),
                    rs.getString("unapplied_ref")),
            r.sourceModule(),
            r.sourceRef(),
            r.invoiceNo())
        .stream()
        .findFirst();
  }

  private void save(OpsInvoice invoice, ReapplyRequest r, ReapplyResult result) {
    jdbc.update(
        SAVE,
        invoice.getCompanyId(),
        r.invoiceNo(),
        r.sourceModule(),
        r.sourceRef(),
        r.reason(),
        result.unapplied(),
        result.excess(),
        String.join(",", result.receiptNos()),
        result.unappliedRef(),
        Timestamp.from(clock.instant()),
        currentUser.username());
  }
}
