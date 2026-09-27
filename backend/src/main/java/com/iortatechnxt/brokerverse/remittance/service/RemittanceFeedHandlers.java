package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInContext;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine.InsurerOr;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Terms;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.service.CsvRows.Row;
import com.iortatechnxt.brokerverse.remittance.service.SpecialRemittanceService.NewRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The flow-in feeds owned by remittance (BRQID.004/005, manual upload until the interfaces are
 * specified, OQ01/OQ22/OQ45). Each record is processed once per idempotency key in its own
 * transaction; a refused record is kept with its reason and never stops the run.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of the handlers
public final class RemittanceFeedHandlers {

  private static final String INVOICE = "invoiceNo";
  private static final String REMARKS = "remarks";

  private RemittanceFeedHandlers() {}

  private static LocalDate date(Row row, String column) {
    try {
      return LocalDate.parse(row.require(column));
    } catch (DateTimeParseException ex) {
      throw new BusinessRuleException(
          "FEED_VALUE_INVALID", "Line " + row.lineNo() + ": " + column + " must be YYYY-MM-DD", ex);
    }
  }

  private static BigDecimal amount(Row row, String column) {
    try {
      return new BigDecimal(row.require(column).replace(",", ""))
          .setScale(2, java.math.RoundingMode.HALF_UP);
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException(
          "FEED_VALUE_INVALID", "Line " + row.lineNo() + ": " + column + " must be an amount", ex);
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  /**
   * {@code INSURER_REMIT_OR} (RMTID.012/013/016): columns batchNo, invoiceNo, orNo, orDate,
   * orAmount.
   */
  @Component
  public static class InsurerOrFeed implements FlowInHandler {

    private final InsurerOrService service;

    /**
     * Creates the handler.
     *
     * @param service OR update
     */
    public InsurerOrFeed(InsurerOrService service) {
      this.service = service;
    }

    @Override
    public String feedCode() {
      return InsurerOrService.FEED;
    }

    @Override
    public void handle(FlowInFile file, FlowInContext context) {
      for (Row row :
          CsvRows.parse(file.content(), Set.of("batchNo", INVOICE, "orNo", "orDate", "orAmount"))) {
        context.accept(
            row.get("batchNo") + "/" + row.get(INVOICE) + "/" + row.get("orNo"),
            row.raw(),
            () ->
                service.record(
                    row.require("batchNo"),
                    row.require(INVOICE),
                    new InsurerOr(
                        row.require("orNo"), date(row, "orDate"), amount(row, "orAmount")),
                    context.runNo()));
      }
    }
  }

  /**
   * {@code COLLECTION_HOLD} (RMTID.021, MKTID.003): hold requests from Marketing / Collection,
   * submitted for approval; columns invoiceNo, reasonCode, holdUntil, remarks.
   */
  @Component
  public static class HoldFeed implements FlowInHandler {

    private final HoldService holds;
    private final InvoiceLedgerQueryService ledger;

    /**
     * Creates the handler.
     *
     * @param holds hold requests
     * @param ledger ledger reads (company of the invoice)
     */
    public HoldFeed(HoldService holds, InvoiceLedgerQueryService ledger) {
      this.holds = holds;
      this.ledger = ledger;
    }

    @Override
    public String feedCode() {
      return "COLLECTION_HOLD";
    }

    @Override
    public void handle(FlowInFile file, FlowInContext context) {
      for (Row row : CsvRows.parse(file.content(), Set.of(INVOICE, "reasonCode", "holdUntil"))) {
        context.accept(
            "HOLD/" + row.get(INVOICE) + "/" + row.get("holdUntil"),
            row.raw(),
            () -> {
              String invoiceNo = row.require(INVOICE);
              return holds
                  .create(
                      ledger.require(invoiceNo).getCompanyId(),
                      invoiceNo,
                      new Terms(
                          row.require("reasonCode"),
                          blankToNull(row.get(REMARKS)),
                          date(row, "holdUntil")),
                      true,
                      RequestSource.COLLECTION_FEED)
                  .getRequestNo();
            });
      }
    }
  }

  /**
   * {@code COLLECTION_SPECIAL_REMIT} (RMTID.030, MKTID.009): special remittance requests from
   * Marketing Collection; columns invoiceNo, conditionCode, remarks.
   */
  @Component
  public static class SpecialRemitFeed implements FlowInHandler {

    private final SpecialRemittanceService specials;
    private final InvoiceLedgerQueryService ledger;

    /**
     * Creates the handler.
     *
     * @param specials special remittance requests
     * @param ledger ledger reads (company of the invoice)
     */
    public SpecialRemitFeed(SpecialRemittanceService specials, InvoiceLedgerQueryService ledger) {
      this.specials = specials;
      this.ledger = ledger;
    }

    @Override
    public String feedCode() {
      return "COLLECTION_SPECIAL_REMIT";
    }

    @Override
    public void handle(FlowInFile file, FlowInContext context) {
      for (Row row : CsvRows.parse(file.content(), Set.of(INVOICE, "conditionCode"))) {
        context.accept(
            "SPR/" + row.get(INVOICE) + "/" + row.get("conditionCode"),
            row.raw(),
            () -> {
              String invoiceNo = row.require(INVOICE);
              return specials
                  .request(
                      ledger.require(invoiceNo).getCompanyId(),
                      new NewRequest(
                          invoiceNo, row.require("conditionCode"), blankToNull(row.get(REMARKS))),
                      RequestSource.COLLECTION_FEED)
                  .getRequestNo();
            });
      }
    }
  }
}
