package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A payment file handler (CSHID.008, BRQID.006): every valid row is a payment that gets its AR and
 * goes through the matching engine at once; the row's outcome category is the matching result
 * (applied, excess, pre-booked, unapplied, cancelled reference) for the run report. Identical files
 * are refused (OQ04: stored read-only with SHA-256). The TXT layout comes from the layout table
 * (OQ03).
 */
public class PaymentFileHandler implements BulkImportHandler {

  /** Permission of the payment uploads. */
  public static final String PERMISSION = "CASH_UPLOAD";

  private final Spec spec;
  private final PaymentIntakeService intake;
  private final PaymentFileLayouts layouts;
  private final CashieringSettings settings;

  /**
   * Creates a handler.
   *
   * @param spec code, title, channel, columns and row mapping
   * @param intake payment intake
   * @param layouts layout table
   * @param settings settings (Head Office)
   */
  public PaymentFileHandler(
      Spec spec,
      PaymentIntakeService intake,
      PaymentFileLayouts layouts,
      CashieringSettings settings) {
    this.spec = spec;
    this.intake = intake;
    this.layouts = layouts;
    this.settings = settings;
  }

  @Override
  public String code() {
    return spec.code();
  }

  @Override
  public String title() {
    return spec.title();
  }

  @Override
  public String permission() {
    return PERMISSION;
  }

  @Override
  public List<BulkColumn> columns() {
    return spec.columns();
  }

  @Override
  public String instructions() {
    return spec.instructions();
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    PaymentIntake payment = spec.mapping().apply(row, context.jobNo());
    if (payment.amount() == null || payment.amount().signum() <= 0) {
      errors.add("The amount must be above zero");
    }
    if (payment.references().isEmpty()) {
      errors.add("The row carries no invoice, ARN, policy or PN reference");
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    Long branch = branch(context);
    IntakeResult result =
        intake.receive(
            new IntakeTarget(context.companyId(), branch, spec.arClass(), ReceiptSource.UPLOAD),
            spec.mapping().apply(row, context.jobNo()));
    return new BulkOutcome(
        result.payment().getPaymentNo(), result.payment().getMatchCategory().name());
  }

  @Override
  public List<String> outcomeCategories() {
    return Arrays.stream(MatchCategory.values()).map(Enum::name).toList();
  }

  @Override
  public TextLayout textLayout() {
    return layouts.layout(spec.code());
  }

  @Override
  public boolean blocksDuplicateFiles() {
    return true;
  }

  private Long branch(BulkContext context) {
    String given = context.parameter("branchId");
    return given == null || given.isBlank()
        ? settings.headOffice(context.companyId()).getId()
        : settings.branch(context.companyId(), Long.valueOf(given)).getId();
  }

  /**
   * What a payment file handler reads.
   *
   * @param code handler code
   * @param title screen title
   * @param channel payment channel
   * @param arClass AR class of its receipts
   * @param columns template columns (BRD field list)
   * @param instructions template instructions
   * @param mapping row and job number to payment
   */
  public record Spec(
      String code,
      String title,
      PaymentChannel channel,
      String arClass,
      List<BulkColumn> columns,
      String instructions,
      BiFunction<BulkRow, String, PaymentIntake> mapping) {

    /** Defensive copy. */
    public Spec {
      columns = List.copyOf(columns);
    }
  }
}
