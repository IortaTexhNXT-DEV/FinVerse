package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInContext;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload.FileKey;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload.Period;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload.RowCounts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Handler of the flow-in feed {@code INSURER_PRODUCTION} (PRCID.009/010/022): reads an insurer
 * production report in the register layout (XLSX, CSV or TXT), refuses a file identical to an
 * earlier upload, and takes each row into the open cycle of its insurer and production month as one
 * flow-in record, so that a bad row never stops the file. A file may hold several insurers or
 * months; each gets its own upload attempt. Manual upload is the transport until the insurer
 * channels are known (OQ29, {@code InsurerFileInbox}).
 */
@Component
public class InsurerProductionHandler implements FlowInHandler {

  /** Feed code. */
  public static final String FEED = "INSURER_PRODUCTION";

  private final BulkFileReader reader;
  private final ReconUploadRecorder recorder;
  private final ReconMatchingService matching;

  /**
   * Creates the handler.
   *
   * @param reader file reader
   * @param recorder upload attempts
   * @param matching matching engine
   */
  public InsurerProductionHandler(
      BulkFileReader reader, ReconUploadRecorder recorder, ReconMatchingService matching) {
    this.reader = reader;
    this.recorder = recorder;
    this.matching = matching;
  }

  @Override
  public String feedCode() {
    return FEED;
  }

  @Override
  public void handle(FlowInFile file, FlowInContext context) {
    process(null, file, context);
  }

  /**
   * Processes a file for a company (upload from the reconciliation screen) or, when the company is
   * not known (upload from Interfaces), for the company of the open cycle of each insurer and
   * month.
   *
   * @param companyId company, may be null
   * @param file file
   * @param context flow-in run
   */
  public void process(Long companyId, FlowInFile file, FlowInContext context) {
    FileKey key = new FileKey(file.fileName(), Sha256.hex(file.content()));
    Optional<String> duplicate = recorder.blockIfDuplicate(key, context.runNo());
    if (duplicate.isPresent()) {
      throw new BusinessRuleException(ReconUploadRecorder.DUPLICATE, duplicate.get());
    }
    ParsedFile parsed;
    try {
      parsed = reader.read(file.fileName(), file.content());
      requireLayout(parsed);
    } catch (BusinessRuleException ex) {
      recorder.failed(companyId, key, context.runNo(), ex.getMessage());
      throw ex;
    }
    Map<Period, List<RawRow>> groups = new LinkedHashMap<>();
    List<RawRow> incomplete = new ArrayList<>();
    for (RawRow row : parsed.rows()) {
      String insurer = InsurerRowParser.insurer(row);
      var month = InsurerRowParser.month(row);
      if (insurer == null || month == null) {
        incomplete.add(row);
      } else {
        groups.computeIfAbsent(new Period(insurer, month, null), k -> new ArrayList<>()).add(row);
      }
    }
    groups.forEach((period, rows) -> takeIn(companyId, key, period, rows, context));
    incomplete.forEach(row -> refuse(row, context));
  }

  private void takeIn(
      Long companyId, FileKey key, Period period, List<RawRow> rows, FlowInContext context) {
    ReconUpload upload = recorder.begin(companyId, key, period, context.runNo());
    int accepted = 0;
    for (RawRow row : rows) {
      boolean ok =
          context.accept(
              context.runNo() + ":" + period.insurerCode() + ":" + row.rowNo(),
              row.values().toString(),
              () ->
                  matching.addInsurerLine(
                      upload.getCycleId(), InsurerRowParser.parse(upload.getId(), row)));
      if (ok) {
        accepted++;
      }
    }
    recorder.finish(upload.getId(), new RowCounts(rows.size(), accepted, rows.size() - accepted));
  }

  private static void refuse(RawRow row, FlowInContext context) {
    context.accept(
        context.runNo() + ":ROW:" + row.rowNo(),
        row.values().toString(),
        () -> {
          throw new BusinessRuleException(
              "RECON_ROW_INCOMPLETE",
              "Row " + row.rowNo() + " has no insurer or production month (yyyy-MM)");
        });
  }

  private static void requireLayout(ParsedFile parsed) {
    List<String> headers = parsed.headers().stream().map(InsurerRowParser::key).toList();
    for (String wanted :
        List.of(RegisterLayout.INSURER, RegisterLayout.MONTH, RegisterLayout.INVOICE)) {
      if (!headers.contains(InsurerRowParser.key(wanted))) {
        throw new BusinessRuleException(
            "RECON_FILE_LAYOUT",
            "The file is not in the production register layout: column '" + wanted + "' missing");
      }
    }
  }
}
