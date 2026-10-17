package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The guided Excel templates of the remittance uploads: the insurer OR schedule, the Collection
 * hold file and the special remittance file (column names as the feeds read them).
 */
@Service
@Transactional(readOnly = true)
public class RemittanceTemplates {

  private static final String INVOICE = "invoiceNo";
  private static final String INVOICE_TEXT = "Invoice of the Operations ledger";
  private static final String INVOICE_EXAMPLE = "BI-HO-2026-000001";
  private static final String EXISTING_INVOICE = "Number of an existing booked invoice";
  private static final String REMARKS = "remarks";
  private static final List<String> RULES =
      List.of(
          "One row per invoice; the file is processed at once and every row is logged with its"
              + " result on the upload run.",
          "Dates as dd-MMM-yyyy (e.g. 15-Jan-2026) or as Excel dates; amounts without thousands"
              + " separators, or as Excel numbers.",
          "Keep the header texts; also accepted: the same columns as CSV, semicolon or tab"
              + " separated text with the headers in the first line.");

  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the templates.
   *
   * @param lovs lists of values (hold reasons, special remittance conditions)
   * @param clock clock
   */
  public RemittanceTemplates(LovService lovs, Clock clock) {
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * The insurer OR schedule template.
   *
   * @return xlsx
   */
  public byte[] insurerOr() {
    List<GuideColumn> columns =
        List.of(
            GuideColumn.of("batchNo", Kind.TEXT, "Remittance batch the OR is for")
                .mandatory()
                .allowed("Number of a remittance batch sent to the insurer")
                .example("RMB-2026-000001"),
            GuideColumn.of(INVOICE, Kind.TEXT, "Invoice of the batch the OR covers")
                .mandatory()
                .allowed("Number of an invoice of the batch")
                .example(INVOICE_EXAMPLE),
            GuideColumn.of("orNo", Kind.TEXT, "Official receipt number of the insurer")
                .mandatory()
                .example("OR-778812"),
            GuideColumn.of("orDate", Kind.DATE, "Date of the official receipt")
                .mandatory()
                .example("2026-09-25"),
            GuideColumn.of("orAmount", Kind.AMOUNT, "Amount of the official receipt")
                .mandatory()
                .example("12500.00"));
    return GuidedTemplateWriter.write(
        GuidedTemplate.single(
            "Insurer OR schedule",
            "Records the official receipts the insurers issued for the remitted premiums.",
            "Remittance officers, from the insurers' OR schedules",
            "Remittance > Insurer OR Upload, box Upload Insurer Schedule",
            RULES,
            GuidedSheet.of(
                "ORs", "Insurer ORs", "One row per invoice of a remittance batch.", columns)));
  }

  /**
   * The Collection hold file template.
   *
   * @return xlsx
   */
  public byte[] holds() {
    List<GuideColumn> columns =
        List.of(
            GuideColumn.of(INVOICE, Kind.TEXT, INVOICE_TEXT + " to hold")
                .mandatory()
                .allowed(EXISTING_INVOICE)
                .example(INVOICE_EXAMPLE),
            GuideColumn.of("reasonCode", Kind.TEXT, "Why the remittance is held")
                .mandatory()
                .choices(choices("HOLD_REASON"))
                .example(first("HOLD_REASON")),
            GuideColumn.of("holdUntil", Kind.DATE, "Last day of the hold")
                .mandatory()
                .example("2026-10-31"),
            GuideColumn.of(REMARKS, Kind.TEXT, "Remarks of the request"));
    return GuidedTemplateWriter.write(
        GuidedTemplate.single(
            "Remittance hold requests",
            "Raises hold requests on the remittance of invoices; each request goes for approval.",
            "Marketing and Collection officers",
            "Remittance > Remittance Holds, button Upload Hold File",
            RULES,
            GuidedSheet.of("Holds", "Hold requests", "One row per invoice to hold.", columns)));
  }

  /**
   * The special remittance file template.
   *
   * @return xlsx
   */
  public byte[] specials() {
    List<GuideColumn> columns =
        List.of(
            GuideColumn.of(INVOICE, Kind.TEXT, INVOICE_TEXT + " to remit specially")
                .mandatory()
                .allowed(EXISTING_INVOICE)
                .example(INVOICE_EXAMPLE),
            GuideColumn.of("conditionCode", Kind.TEXT, "Condition of the special remittance")
                .mandatory()
                .choices(choices("SPECIAL_REMIT_CONDITION"))
                .example(first("SPECIAL_REMIT_CONDITION")),
            GuideColumn.of(REMARKS, Kind.TEXT, "Remarks of the request"));
    return GuidedTemplateWriter.write(
        GuidedTemplate.single(
            "Special remittance requests",
            "Raises special remittance requests on invoices; each request goes for approval.",
            "Marketing Collection officers",
            "Remittance > Special Remittance, button Upload Request File",
            RULES,
            GuidedSheet.of(
                "Requests", "Special remittance requests", "One row per invoice.", columns)));
  }

  private List<Choice> choices(String type) {
    return lovs.options(type, BusinessClock.today(clock)).stream()
        .map(e -> new Choice(e.code(), e.label()))
        .toList();
  }

  private String first(String type) {
    List<Choice> all = choices(type);
    return all.isEmpty() ? "" : all.get(0).code();
  }
}
