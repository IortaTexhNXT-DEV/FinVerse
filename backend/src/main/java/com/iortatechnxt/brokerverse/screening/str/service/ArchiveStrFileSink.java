package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.screening.report.StrRegisterReport;
import org.springframework.stereotype.Component;

/**
 * Default {@link StrFileSink} (SNSRP-706, until SQ16): the extraction file is archived under the
 * STR register report ({@code SCR-STR-REGISTER}) and downloaded from the STR screen by users who
 * may export the compliance reports.
 */
@Component
public class ArchiveStrFileSink implements StrFileSink {

  private final ReportArchiveService archive;

  /**
   * Creates the sink.
   *
   * @param archive report archive
   */
  public ArchiveStrFileSink(ReportArchiveService archive) {
    this.archive = archive;
  }

  @Override
  public Delivered deliver(StrFile file) {
    ReportRun run =
        archive.archiveGenerated(
            StrRegisterReport.CODE,
            file.echo(),
            file.rows(),
            new RunFile("CSV", file.fileName(), file.contentType(), file.content()),
            null);
    return new Delivered(run.getId(), "Report archive run " + run.getId());
  }
}
