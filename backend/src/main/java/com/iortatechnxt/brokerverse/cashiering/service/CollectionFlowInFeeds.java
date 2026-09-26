package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInContext;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The Collection flow-in feeds cashiering receives as files until the Collection system interface
 * is known (OQ01, OQ45): each record of a {@code COLLECTION_*} feed goes through the same checks
 * and commit as the matching bulk upload, once per idempotency key. The files carry the bulk
 * columns plus {@value #COMPANY} (company code).
 */
public final class CollectionFlowInFeeds {

  /** Column of the company code. */
  public static final String COMPANY = "Company";

  private CollectionFlowInFeeds() {}

  /** A flow-in feed whose records are the rows of a bulk handler. */
  abstract static class BulkBackedFeed implements FlowInHandler {

    private final BulkImportHandler rows;
    private final BulkFileReader reader;
    private final CompanyRepository companies;
    private final Clock clock;

    BulkBackedFeed(
        BulkImportHandler rows, BulkFileReader reader, CompanyRepository companies, Clock clock) {
      this.rows = rows;
      this.reader = reader;
      this.companies = companies;
      this.clock = clock;
    }

    /**
     * Idempotency key of a row.
     *
     * @param row row
     * @return key
     */
    abstract String keyOf(BulkRow row);

    @Override
    public void handle(FlowInFile file, FlowInContext context) {
      ParsedFile parsed = reader.read(file.fileName(), file.content());
      for (ParsedFile.RawRow raw : parsed.rows()) {
        Map<String, String> values = new HashMap<>();
        raw.values().forEach((h, v) -> values.put(h, rows.sanitize(h, v)));
        BulkRow row = new BulkRow(raw.rowNo(), values);
        context.accept(keyOf(row), payload(values), () -> commit(row, context.runNo()));
      }
    }

    private String commit(BulkRow row, String runNo) {
      String code = row.text(COMPANY);
      Long companyId =
          companies
              .findByCode(code == null ? "" : code)
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "FLOW_IN_COMPANY_UNKNOWN", "Unknown company " + row.text(COMPANY)))
              .getId();
      BulkContext context = new BulkContext(companyId, runNo, LocalDate.now(clock), Map.of());
      List<String> errors = rows.validate(row, context);
      if (!errors.isEmpty()) {
        throw new BusinessRuleException("FLOW_IN_RECORD_INVALID", String.join("; ", errors));
      }
      return rows.commit(row, context);
    }

    private static String payload(Map<String, String> values) {
      return values.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue()))
          .collect(Collectors.joining(";"));
    }
  }

  /**
   * {@code COLLECTION_CWT2307} (CSHID.026, MKTID.013): BIR 2307 tags from Marketing Collection, in
   * the {@code CWT_TAGS} columns; one record per invoice.
   */
  @Component
  public static class Cwt2307Feed extends BulkBackedFeed {

    /**
     * Creates the feed.
     *
     * @param tags the BIR 2307 tagging rows
     * @param reader file reader
     * @param companies companies
     * @param clock clock
     */
    public Cwt2307Feed(
        CwtTagHandler tags, BulkFileReader reader, CompanyRepository companies, Clock clock) {
      super(tags, reader, companies, clock);
    }

    @Override
    public String feedCode() {
      return "COLLECTION_CWT2307";
    }

    @Override
    String keyOf(BulkRow row) {
      return "CWT/" + row.text("Invoice no");
    }
  }

  /**
   * {@code COLLECTION_COMMISSION_PAYMENT} (CSHID.007): commission payment details from Collection,
   * in the {@code COMMISSION_PAYMENT} columns, staged for "Issue Commission ORs"; one record per
   * payment reference and invoice.
   */
  @Component
  public static class CommissionPaymentFeed extends BulkBackedFeed {

    /**
     * Creates the feed.
     *
     * @param payments the commission payment rows
     * @param reader file reader
     * @param companies companies
     * @param clock clock
     */
    public CommissionPaymentFeed(
        CommissionPaymentHandler payments,
        BulkFileReader reader,
        CompanyRepository companies,
        Clock clock) {
      super(payments, reader, companies, clock);
    }

    @Override
    public String feedCode() {
      return "COLLECTION_COMMISSION_PAYMENT";
    }

    @Override
    String keyOf(BulkRow row) {
      return "COM/" + row.text("Payment ref") + "/" + row.text("Invoice no");
    }
  }
}
