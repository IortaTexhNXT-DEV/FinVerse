package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload of quotation requests (BRNB.023/028/041): the manual stand-in for the HLS interface
 * (parked, Q11) and for lists of requests received by e-mail. Each row becomes a request in the
 * request inbox; a row whose source reference was already received is refused.
 */
@Component
public class QuotationRequestBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "QUOTATION_REQUEST";

  static final String CHANNEL = "Channel";
  static final String REFERENCE = "Source Reference";
  static final String CLIENT = "Client Code";
  static final String NAME = "Prospect Name";
  static final String EMAIL = "Prospect Email";
  static final String MOBILE = "Prospect Mobile";
  static final String PRODUCT = "Product Code";
  static final String SEGMENT = "Market Segment";
  static final String COVER = "Requested Cover";

  private static final String DEFAULT_CHANNEL = "UPLOAD";

  private final QuotationRequestService requests;
  private final ClientService clients;
  private final ProductCatalogService catalog;
  private final LovService lovs;
  private final QuotationBulkSupport support;
  private final Clock clock;

  /**
   * Creates the handler.
   *
   * @param requests request intake
   * @param clients clients
   * @param catalog products
   * @param lovs lists of values
   * @param support shared bulk helpers
   * @param clock clock
   */
  public QuotationRequestBulkHandler(
      QuotationRequestService requests,
      ClientService clients,
      ProductCatalogService catalog,
      LovService lovs,
      QuotationBulkSupport support,
      Clock clock) {
    this.requests = requests;
    this.clients = clients;
    this.catalog = catalog;
    this.lovs = lovs;
    this.support = support;
    this.clock = clock;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Quotation requests";
  }

  @Override
  public String permission() {
    return "QUOTE_MAINTAIN";
  }

  @Override
  public String instructions() {
    return "One row per quotation request (e.g. an HLS extract). Give the Client Code of an"
        + " existing client or the Prospect Name ('Last, First' for a person). Channel defaults to"
        + " UPLOAD; use HLS for Home Loan System requests with their Source Reference.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(CHANNEL, "Source channel (list SOURCE_CHANNEL)", "HLS"),
        BulkColumn.optional(REFERENCE, "Reference in the source system", "HLS-000123"),
        BulkColumn.optional(CLIENT, "Client or prospect code", ""),
        BulkColumn.optional(NAME, "Prospect name when no client code", "Reyes, Ana"),
        BulkColumn.optional(EMAIL, "Prospect e-mail", "ana.reyes@example.ph"),
        BulkColumn.optional(MOBILE, "Prospect mobile", "09171234567"),
        BulkColumn.optional(PRODUCT, "Requested product (risk code)", "PAR01"),
        BulkColumn.optional(SEGMENT, "Market segment", "CBG"),
        BulkColumn.required(COVER, "Requested cover", "Fire cover of a house and lot"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(REFERENCE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return support.check(
        () -> {
          LocalDate today = LocalDate.now(clock);
          lovs.requireValid("SOURCE_CHANNEL", channel(row), today);
          lovs.validateOptional("MARKET_SEGMENT", row.text(SEGMENT), today);
          if (row.text(CLIENT) != null) {
            clients.requireByCode(context.companyId(), row.text(CLIENT));
          } else if (row.text(NAME) == null) {
            return List.of("Give the Client Code or the Prospect Name");
          }
          if (row.text(PRODUCT) != null) {
            catalog.requireUsableProduct(row.text(PRODUCT));
          }
          if (requests.known(channel(row), row.text(REFERENCE))) {
            return List.of("Source reference " + row.text(REFERENCE) + " was already received");
          }
          return List.of();
        });
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return requests
        .receive(
            context.companyId(),
            new IncomingQuotationRequest(
                channel(row),
                row.text(REFERENCE),
                null,
                row.text(CLIENT),
                row.text(NAME),
                row.text(EMAIL),
                row.text(MOBILE),
                row.text(PRODUCT),
                row.text(SEGMENT),
                row.text(COVER)))
        .getRequestNo();
  }

  private static String channel(BulkRow row) {
    String given = row.text(CHANNEL);
    return given == null ? DEFAULT_CHANNEL : given;
  }
}
