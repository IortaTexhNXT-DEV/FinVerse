package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RefundBasis;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Batch of cancellation and adjustment requests by upload (ADJID.006, handler {@code ADJ_BATCH}):
 * one request per row, validated like a request raised on screen (duplicates are refused unless the
 * row gives a justification), raised and submitted for validation. A row that repeats an earlier
 * row of the file is refused, so a re-upload does not raise the same request twice.
 */
@Component
public class AdjustmentBatchHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "ADJ_BATCH";

  static final String INVOICE = "Invoice No";
  static final String TYPE = "Endorsement Type";
  static final String REQUEST_TYPE = "Request Type";
  static final String REASON = "Reason";
  static final String REFERENCE = "Endorsement Ref";
  static final String EFFECTIVE = "Effective Date";
  static final String BASIS = "Refund Basis";
  static final String TSI = "TSI Change";
  static final String BASIC = "Basic Premium";
  static final String DST = "DST";
  static final String PTAX = "Premium Tax VAT";
  static final String LGT = "LGT";
  static final String FST = "FST";
  static final String OTHER = "Other Charges";
  static final String COMMISSION = "Commission";
  static final String DESCRIPTION = "Description";
  static final String JUSTIFICATION = "Duplicate Justification";

  private final EndorsementRequestService requests;
  private final RequestWorkflowService workflow;
  private final TransactionTemplate checks;

  /**
   * Creates the handler.
   *
   * @param requests raise and preview
   * @param workflow submit
   * @param txManager transaction manager (checks in their own read-only transaction)
   */
  public AdjustmentBatchHandler(
      EndorsementRequestService requests,
      RequestWorkflowService workflow,
      PlatformTransactionManager txManager) {
    this.requests = requests;
    this.workflow = workflow;
    this.checks = new TransactionTemplate(txManager);
    this.checks.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.checks.setReadOnly(true);
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Endorsement and cancellation requests";
  }

  @Override
  public String permission() {
    return "ADJ_POST";
  }

  @Override
  public String instructions() {
    return "One request per row on a booked invoice. Endorsement Type is a code of the list"
        + " ENDORSEMENT_TYPE (FIN_, NF_, INT_); Request Type, Reason and Refund Basis (PRO_RATA or"
        + " SHORT_PERIOD) are codes of their lists. Give the TSI change for a change of sum"
        + " insured, or the premium component changes for an amount change. Each request is raised"
        + " and submitted for validation.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            INVOICE, "Invoice number of the Operations ledger", "BI-HO-2026-000001"),
        BulkColumn.required(TYPE, "Endorsement type code", "FIN_CHANGE_COVER"),
        BulkColumn.optional(REQUEST_TYPE, "Annex V request type code", "FLAT_CANCELLATION"),
        BulkColumn.optional(REASON, "Cancellation reason code", "UNIT_SOLD"),
        BulkColumn.optional(REFERENCE, "Insurer endorsement reference", "END-2026-0001"),
        new BulkColumn(EFFECTIVE, "Effective date", true, BulkColumn.Type.DATE, "2026-10-01"),
        BulkColumn.optional(BASIS, "PRO_RATA or SHORT_PERIOD", "PRO_RATA"),
        number(TSI, "-100000"),
        number(BASIC, "-1000.00"),
        number(DST, ""),
        number(PTAX, ""),
        number(LGT, ""),
        number(FST, ""),
        number(OTHER, ""),
        number(COMMISSION, ""),
        BulkColumn.required(DESCRIPTION, "Description of the change", "Unit sold"),
        BulkColumn.optional(JUSTIFICATION, "Why a possible duplicate proceeds", ""));
  }

  private static BulkColumn number(String header, String example) {
    return new BulkColumn(
        header, header + " change (signed)", false, BulkColumn.Type.NUMBER, example);
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return String.join(
        "|",
        row.text(INVOICE),
        row.text(TYPE),
        String.valueOf(row.text(REQUEST_TYPE)),
        String.valueOf(row.text(REASON)),
        String.valueOf(row.text(REFERENCE)));
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    try {
      checks.executeWithoutResult(
          s -> {
            EndorsementRequestService.Preview preview = requests.preview(draft(row));
            if (!preview.duplicates().isEmpty() && row.text(JUSTIFICATION) == null) {
              errors.add("Possible duplicate of " + String.join(", ", preview.duplicates()));
            }
          });
    } catch (BusinessRuleException | ResourceNotFoundException | IllegalArgumentException e) {
      errors.add(e.getMessage());
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    EndorsementRequest request = requests.create(draft(row));
    workflow.submit(request.getId(), "Batch " + context.jobNo());
    return request.getRequestNo();
  }

  static RequestDraft draft(BulkRow row) {
    String basis = row.text(BASIS);
    return new RequestDraft(
        row.text(INVOICE),
        new RequestTerms(
            row.text(TYPE),
            row.text(REQUEST_TYPE),
            row.text(REASON),
            row.text(REFERENCE),
            row.date(EFFECTIVE),
            basis == null ? null : RefundBasis.valueOf(basis.toUpperCase(Locale.ROOT)),
            row.number(TSI),
            null,
            null,
            null,
            row.text(DESCRIPTION),
            null),
        new AmountInput(
            row.number(BASIC),
            row.number(DST),
            row.number(PTAX),
            row.number(LGT),
            row.number(FST),
            row.number(OTHER),
            row.number(COMMISSION),
            null),
        row.text(JUSTIFICATION),
        null);
  }
}
