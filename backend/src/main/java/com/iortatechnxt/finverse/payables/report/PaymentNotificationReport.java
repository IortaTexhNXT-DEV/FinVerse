package com.iortatechnxt.finverse.payables.report;

import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.finverse.payables.service.NotificationRecord;
import com.iortatechnxt.finverse.payables.service.PaymentNotificationFormatter;
import com.iortatechnxt.finverse.payables.service.PaymentNotificationService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-BRS-PAYNOTIFY – Payment Notification to the Bank: listing of the payments contained in the
 * notification file (record 01 details and the record 02 trailer in the notes). The file itself is
 * downloaded from {@code GET /api/v1/payables/payment-notifications/file}.
 */
@Component
public class PaymentNotificationReport implements ReportDefinition {

  private static final String BANK = "bankAccountCode";
  private static final String INCLUDE_CHEQUES = "includeCheques";
  private static final String FILE_NAME = "fileName";

  private final PaymentNotificationService notifications;
  private final BankAccountQueryService banks;

  /**
   * Creates the report.
   *
   * @param notifications notification service
   * @param banks bank accounts
   */
  public PaymentNotificationReport(
      PaymentNotificationService notifications, BankAccountQueryService banks) {
    this.notifications = notifications;
    this.banks = banks;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-BRS-PAYNOTIFY",
        "Payment Notification to the Bank",
        ReportCategory.RECONCILIATION,
        "Payments advised to the bank in the notification file (record 01 / trailer 02)",
        List.of(
            GlReportSupport.companyParam(),
            ParameterSpec.required(BANK, "Bank Code", ParameterType.TEXT).withDefault("BDO-CA"),
            GlReportSupport.fromParam(),
            GlReportSupport.toParam(),
            ParameterSpec.optional(
                INCLUDE_CHEQUES, "Include cheques and PDCs", ParameterType.BOOLEAN),
            ParameterSpec.optional(FILE_NAME, "File Name", ParameterType.TEXT)),
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    BankAccount bank = banks.getByCode(p.longValue(GlReportSupport.COMPANY), p.text(BANK));
    List<NotificationRecord> records =
        notifications.records(
            bank.getId(),
            p.date(GlReportSupport.FROM),
            p.date(GlReportSupport.TO),
            p.flag(INCLUDE_CHEQUES));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (NotificationRecord r : records) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("recordType", "01");
      row.put("vendorCode", r.vendorCode());
      row.put("vendorName", r.vendorName());
      row.put("vendorAccount", r.vendorAccountNo());
      row.put("voucherNo", r.voucherNo());
      row.put("chequeNo", r.chequeNo());
      row.put("date", r.paymentDate());
      row.put("currency", r.currency());
      row.put("amount", r.amount());
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("recordType", "Rec"),
            ReportColumn.text("vendorCode", "Vendor Code"),
            ReportColumn.text("vendorName", "Vendor Name"),
            ReportColumn.text("vendorAccount", "Bank Account Number"),
            ReportColumn.text("voucherNo", "Document No."),
            ReportColumn.text("chequeNo", "Cheque No."),
            ReportColumn.date("date", "Date"),
            ReportColumn.text("currency", "Currency"),
            ReportColumn.amount("amount", "Amount"))
        .rows(rows)
        .note(
            "Record 02 (trailer): company account "
                + bank.getAccountNo()
                + ", "
                + records.size()
                + " payments, total "
                + PaymentNotificationFormatter.total(records).toPlainString())
        .note(
            "Layout "
                + bank.getNotificationFormat()
                + "; file "
                + p.optionalText(FILE_NAME).orElse("PAYNOTIFY-" + bank.getCode())
                + " is generated from the Payables > Payment Vouchers screen.")
        .build();
  }
}
