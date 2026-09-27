package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk acceptance of quotations (BRNB.024/042/044): the ARNs of quotations sent to clients and
 * accepted by them, optionally with the accepted risk groups; with the parameter {@code
 * createAccounts} the accounts are created at once (confirmed clients only). The uploaded list is
 * the acceptance evidence: the job number is recorded on each quotation.
 */
@Component
public class QuotationAcceptanceBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "QUOTATION_ACCEPTANCE";

  /** Parameter: create the accounts after the acceptance. */
  public static final String CREATE_ACCOUNTS = "createAccounts";

  static final String ARN = "ARN";
  static final String GROUPS = "Risk Groups";
  static final String REMARKS = "Remarks";

  private final QuotationQueryService queries;
  private final QuotationAcceptanceService acceptance;
  private final ClientService clients;
  private final QuotationBulkSupport support;

  /**
   * Creates the handler.
   *
   * @param queries quotation reads
   * @param acceptance acceptance and conversion
   * @param clients clients
   * @param support shared bulk helpers
   */
  public QuotationAcceptanceBulkHandler(
      QuotationQueryService queries,
      QuotationAcceptanceService acceptance,
      ClientService clients,
      QuotationBulkSupport support) {
    this.queries = queries;
    this.acceptance = acceptance;
    this.clients = clients;
    this.support = support;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Bulk quotation acceptance";
  }

  @Override
  public String permission() {
    return "QUOTE_MAINTAIN";
  }

  @Override
  public String instructions() {
    return "One row per quotation accepted by the client, identified by its ARN. Leave Risk"
        + " Groups empty when the client accepts every group, or list them separated by ';'."
        + " Tick 'Create accounts' on screen to create the accounts of confirmed clients at once.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(ARN, "ARN of the quotation", "ARN-2026-000123"),
        BulkColumn.optional(GROUPS, "Accepted risk groups, e.g. 1;2 (empty = all)", ""),
        BulkColumn.optional(REMARKS, "Remarks recorded with the acceptance", ""));
  }

  /** Upper-cases an ARN; ARNs are plain ASCII, so no locale-dependent case mapping is needed. */
  private static String asciiUpper(String value) {
    if (value == null) {
      return null;
    }
    StringBuilder out = new StringBuilder(value.length());
    for (char ch : value.toCharArray()) {
      out.append(ch >= 'a' && ch <= 'z' ? (char) (ch - ('a' - 'A')) : ch);
    }
    return out.toString();
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    return ARN.equals(header) ? asciiUpper(clean) : clean;
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(ARN);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return support.check(
        () -> {
          QuotationSummary q = queries.getByArn(row.text(ARN));
          List<String> errors = new ArrayList<>();
          if (q.status() != QuotationStatus.SENT_TO_CLIENT) {
            errors.add(q.quotationNo() + " is " + q.status() + ", not sent to the client");
          }
          groups(row);
          if (QuotationBulkSupport.yes(context.parameter(CREATE_ACCOUNTS))) {
            clients.requireConfirmed(q.clientId());
          }
          return errors;
        });
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Long id = queries.getByArn(row.text(ARN)).id();
    String note =
        "Accepted by bulk upload "
            + context.jobNo()
            + (row.text(REMARKS) == null ? "" : ": " + row.text(REMARKS));
    Quotation q = acceptance.acceptListed(id, groups(row), note);
    if (QuotationBulkSupport.yes(context.parameter(CREATE_ACCOUNTS))) {
      q = acceptance.createAccounts(id, "Bulk upload " + context.jobNo());
      return q.getArn() + " -> " + String.join(", ", q.getAccountArns());
    }
    return q.getArn();
  }

  private static List<Integer> groups(BulkRow row) {
    String cell = row.text(GROUPS);
    if (cell == null) {
      return List.of();
    }
    try {
      return Arrays.stream(cell.split("[;,|]"))
          .map(String::strip)
          .filter(s -> !s.isEmpty())
          .map(Integer::valueOf)
          .toList();
    } catch (NumberFormatException e) {
      throw new BusinessRuleException(
          "ACCEPTANCE_GROUP_INVALID", "Risk groups are numbers separated by ';'", e);
    }
  }
}
