package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Person;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.DraftItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.Terms;
import com.iortatechnxt.brokerverse.quotation.service.QuotationProspects.Prospect;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Bulk quotations (BRNB.024/028/042/063): one quotation with one risk item per row, for the product
 * chosen on screen (parameters {@code product} and {@code segment}). The client is found by code or
 * by name and birth date, else created as a prospect: a quotation does not need a confirmed client
 * code (Q13). Each quotation gets its number, ARN and version 1 and waits in DRAFT.
 */
@Component
public class QuotationCreateBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "QUOTATION_CREATE";

  /** Parameter: product of the quotations. */
  public static final String PRODUCT = "product";

  /** Parameter: default market segment. */
  public static final String SEGMENT = "segment";

  static final String CLIENT_CODE = "Client Code";
  static final String CLIENT_NAME = "Client Name";
  static final String BIRTH_DATE = "Birth Date";
  static final String EMAIL = "Client Email";
  static final String MOBILE = "Client Mobile";
  static final String MARKET = "Market Segment";
  static final String FROM = "Period From";
  static final String TO = "Period To";
  static final String VALID = "Valid Until";
  static final String INSURER = "Insurer Code";
  static final String BRANCH = "Insurer Branch";
  static final String DP = "Direct Payment";
  static final String SI = "Sum Insured";
  static final String RATE = "Rate %";
  static final String DESCRIPTION = "Description";
  static final String PLATE = "Plate No";
  static final String ENGINE = "Engine No";
  static final String CHASSIS = "Chassis No";
  static final String MAKE = "Make";
  static final String MODEL = "Model";
  static final String YEAR = "Year Model";
  static final String ADDRESS = "Address";
  static final String CITY = "City";
  static final String OCCUPANCY = "Occupancy";
  static final String CONSTRUCTION = "Construction Class";
  static final String PERSON = "Insured Person";

  private final QuotationService quotations;
  private final QuotationRules rules;
  private final QuotationProspects prospects;
  private final QuotationBulkSupport support;

  /**
   * Creates the handler.
   *
   * @param quotations quotations
   * @param rules draft validation
   * @param prospects client look-up and prospect creation
   * @param support shared bulk helpers
   */
  public QuotationCreateBulkHandler(
      QuotationService quotations,
      QuotationRules rules,
      QuotationProspects prospects,
      QuotationBulkSupport support) {
    this.quotations = quotations;
    this.rules = rules;
    this.prospects = prospects;
    this.support = support;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Bulk quotations";
  }

  @Override
  public String permission() {
    return "QUOTE_MAINTAIN";
  }

  @Override
  public String instructions() {
    return "Choose the product (and optionally the market segment) on screen. One row = one"
        + " quotation with one risk item. Identify the client by Client Code, or give the Client"
        + " Name ('Last, First' with the Birth Date for a person) to find or create a prospect."
        + " Fill the vehicle, location, person or description columns of the product line.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(CLIENT_CODE, "Client or prospect code", "CL-2026-000001"),
        BulkColumn.optional(CLIENT_NAME, "Prospect name when no code", "Dela Cruz, Juan"),
        QuotationBulkSupport.day(BIRTH_DATE, "Birth date of a person prospect", false),
        BulkColumn.optional(EMAIL, "E-mail of a new prospect", "juan@example.ph"),
        BulkColumn.optional(MOBILE, "Mobile of a new prospect", "09171234567"),
        BulkColumn.optional(MARKET, "Market segment (default: on screen)", "CBG"),
        QuotationBulkSupport.day(FROM, "Period from", true),
        QuotationBulkSupport.day(TO, "Period to", true),
        QuotationBulkSupport.day(VALID, "Valid until (default: configured validity)", false),
        BulkColumn.optional(INSURER, "Insurer party code", ""),
        BulkColumn.optional(BRANCH, "Insurer branch code (LGT)", ""),
        QuotationBulkSupport.flag(DP, "Premium paid directly to the insurer (MKTID.011)"),
        QuotationBulkSupport.numeric(SI, "Sum insured", true, "850000"),
        QuotationBulkSupport.numeric(RATE, "Rate in percent (empty = product default)", false, ""),
        BulkColumn.optional(DESCRIPTION, "Description of the risk (other lines)", ""),
        BulkColumn.optional(PLATE, "Motor: plate number", "NAB 1234"),
        BulkColumn.optional(ENGINE, "Motor: engine number", ""),
        BulkColumn.optional(CHASSIS, "Motor: chassis number", ""),
        BulkColumn.optional(MAKE, "Motor: make", "Toyota"),
        BulkColumn.optional(MODEL, "Motor: model", "Vios"),
        QuotationBulkSupport.numeric(YEAR, "Motor: year model", false, "2025"),
        BulkColumn.optional(ADDRESS, "Fire: address of risk", ""),
        BulkColumn.optional(CITY, "Fire: city", ""),
        BulkColumn.optional(OCCUPANCY, "Fire: occupancy (list OCCUPANCY)", ""),
        BulkColumn.optional(CONSTRUCTION, "Fire: construction class", ""),
        BulkColumn.optional(PERSON, "Personal accident: insured person", ""));
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    return List.of(PLATE, ENGINE, CHASSIS).contains(header)
        ? BulkImportHandler.identifier(clean)
        : clean;
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return support.check(
        () -> {
          Optional<Client> client = client(row, context);
          QuotationDraft draft = draft(row, context, client.map(Client::getId).orElse(null));
          if (client.isPresent()) {
            rules.resolve(context.companyId(), draft);
          } else {
            rules.offer(context.companyId(), draft);
          }
          return List.of();
        });
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Client client =
        client(row, context)
            .orElseGet(
                () ->
                    prospects.create(
                        context.companyId(),
                        new Prospect(
                            row.text(CLIENT_NAME),
                            row.date(BIRTH_DATE),
                            row.text(EMAIL),
                            row.text(MOBILE),
                            segment(row, context))));
    Quotation q = quotations.create(context.companyId(), draft(row, context, client.getId()));
    return q.getQuotationNo() + " / " + q.getArn();
  }

  private Optional<Client> client(BulkRow row, BulkContext context) {
    return prospects.find(
        context.companyId(), row.text(CLIENT_CODE), row.text(CLIENT_NAME), row.date(BIRTH_DATE));
  }

  private static String segment(BulkRow row, BulkContext context) {
    String given = row.text(MARKET);
    return given != null ? given : context.parameter(SEGMENT);
  }

  private static QuotationDraft draft(BulkRow row, BulkContext context, Long clientId) {
    String product = context.parameter(PRODUCT);
    if (product == null || product.isBlank()) {
      throw new BusinessRuleException("BULK_PRODUCT_REQUIRED", "Choose the product of the upload");
    }
    return new QuotationDraft(
        clientId,
        product,
        segment(row, context),
        "UPLOAD",
        null,
        null,
        new Terms(
            row.text(INSURER),
            row.text(BRANCH),
            row.date(FROM),
            row.date(TO),
            row.date(VALID),
            row.yes(DP),
            null,
            "Bulk upload " + context.jobNo()),
        List.of(
            new DraftItem(1, item(row::text, row.number(SI), row.number(RATE), row.number(YEAR)))));
  }

  private static RiskItemData item(
      Function<String, String> cell, BigDecimal sum, BigDecimal rate, BigDecimal year) {
    String address = cell.apply(ADDRESS);
    List<InsuredItem> insured =
        address == null || sum == null ? List.of() : List.of(new InsuredItem("Building", sum));
    return new RiskItemData(
        cell.apply(DESCRIPTION),
        sum,
        rate,
        null,
        null,
        new Vehicle(
            cell.apply(PLATE),
            null,
            cell.apply(ENGINE),
            cell.apply(CHASSIS),
            cell.apply(MAKE),
            cell.apply(MODEL),
            year == null ? null : year.intValue(),
            null,
            null,
            null),
        new Location(
            address,
            cell.apply(CITY),
            null,
            cell.apply(OCCUPANCY),
            cell.apply(CONSTRUCTION),
            insured),
        new Person(cell.apply(PERSON), null, null));
  }
}
