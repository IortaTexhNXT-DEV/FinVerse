package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.OutputKind;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput.OutputFile;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutputRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The end-of-day reports of a run (DIS 3.28.0, 3.28.2): Remittance, Refund, Summary, Payment to
 * supplier, Employee-related and Other disbursements, and the unregularised transactions, each
 * generated for the business date, archived in the report archive and kept with the run's outputs.
 * A report already produced for the run is not produced again.
 */
@Component
public class EodReports {

  /** The end-of-day report codes, in the order of Appendix B. */
  public static final List<String> CODES =
      List.of(
          "DSB-EOD-REMIT",
          "DSB-EOD-REFUND",
          "DSB-EOD-SUMMARY",
          "DSB-EOD-SUPPLIER",
          "DSB-EOD-EMPLOYEE",
          "DSB-EOD-OTHER",
          "DSB-UNREGULARIZED");

  private final ReportService reports;
  private final ReportArchiveService archive;
  private final EodOutputRepository outputs;
  private final EodOutputFiles outputFiles;

  /**
   * Creates the helper.
   *
   * @param reports report service
   * @param archive report archive (content of the archived files)
   * @param outputs run outputs
   * @param outputFiles output files (file store)
   */
  public EodReports(
      ReportService reports,
      ReportArchiveService archive,
      EodOutputRepository outputs,
      EodOutputFiles outputFiles) {
    this.outputFiles = outputFiles;
    this.reports = reports;
    this.archive = archive;
    this.outputs = outputs;
  }

  /**
   * Produces the missing end-of-day reports of a run.
   *
   * @param run end-of-day run
   * @return reports of the run
   */
  public int produce(EodRun run) {
    Map<String, String> params =
        Map.of(
            "companyId", run.getCompanyId().toString(),
            "from", run.getBusinessDate().toString(),
            "to", run.getBusinessDate().toString());
    for (String code : CODES) {
      if (outputs.existsByEodRunIdAndKindAndCode(run.getId(), OutputKind.REPORT, code)) {
        continue;
      }
      ReportRun archived = reports.generate(code, params, ExportFormat.XLSX, null);
      byte[] content = archive.content(archived);
      outputFiles.save(
          run,
          OutputKind.REPORT,
          code,
          new OutputFile(
              run.getRunNo() + "_" + archived.getFileName(), archived.getContentType(), content),
          archived.getRowCount());
    }
    run.reported(CODES.size());
    return CODES.size();
  }
}
