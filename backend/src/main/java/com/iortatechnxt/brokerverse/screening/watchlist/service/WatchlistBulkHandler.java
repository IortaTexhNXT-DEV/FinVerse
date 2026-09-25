package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk handler {@code SCR_WATCHLIST} (SNSRP-201, 203): the CSV / XLSX template of a list file. The
 * same columns are read by "Upload List File" on List Sources and Runs; through the generic bulk
 * upload each valid row becomes a PENDING addition or change of the source given as parameter
 * {@code source} (default INTERNAL), approved by a Compliance Checker.
 */
@Component
public class WatchlistBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "SCR_WATCHLIST";

  private final WatchlistService watchlists;

  /**
   * Creates the handler.
   *
   * @param watchlists watchlist maintenance
   */
  public WatchlistBulkHandler(WatchlistService watchlists) {
    this.watchlists = watchlists;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Watchlist entries (sanctions / PEP list file)";
  }

  @Override
  public String permission() {
    return ScreeningPermissions.LIST_MAINTAIN;
  }

  @Override
  public List<BulkColumn> columns() {
    return ListRecord.columns();
  }

  @Override
  public String instructions() {
    return "One row per listed person or entity. Reference identifies the record in its source:"
        + " a known reference changes the entry, a new one adds it. Aliases are separated by"
        + " semicolons; dates are yyyy-MM-dd. Invented names only in test files. Changes wait for"
        + " a Compliance Checker before screening uses them.";
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(ListRecord.REFERENCE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    ListRecord.read(row.rowNo(), row.values(), listType(context), errors);
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    ListRecord rec = ListRecord.read(row.rowNo(), row.values(), listType(context), errors);
    if (rec == null) {
      throw new BusinessRuleException("SCR_LIST_RECORD_INVALID", String.join("; ", errors));
    }
    String remarks = rec.remarks() == null ? "Bulk upload " + context.jobNo() : rec.remarks();
    watchlists.submit(context.parameter("source"), rec, remarks);
    return rec.reference();
  }

  private String listType(BulkContext context) {
    String source = context.parameter("source");
    return watchlists
        .source(source == null || source.isBlank() ? WatchlistService.INTERNAL : source)
        .getListType();
  }
}
