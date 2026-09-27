package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.screening.report.StrRegisterReport;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.service.LegacyFileTable;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Default {@link StrFileSink} (SNSRP-706, until SQ16): the extraction file is archived under the
 * STR register report ({@code SCR-STR-REGISTER}) and downloaded from the STR screen by users who
 * may export the compliance reports. The file is stored under the record class {@code STR} (AMLA
 * hold and retention). Also declares the STR files archived before ST1, copied under the same class
 * before the general copy of the report archive.
 */
@Component
public class ArchiveStrFileSink implements StrFileSink {

  /** Record class of STR files. */
  public static final String RECORD_CLASS = "STR";

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
            null,
            RECORD_CLASS);
    return new Delivered(run.getId(), "Report archive run " + run.getId());
  }

  /**
   * STR files archived before ST1: copied under the record class {@value #RECORD_CLASS} before the
   * general copy of {@code report_run_file} (which would give them the report class).
   *
   * @return table
   */
  @Bean
  @Order(0)
  static LegacyFileTable strFileTable() {
    String from =
        " from report_run_file f join report_run r on r.id = f.run_id"
            + " join scr_str_extraction x on x.report_run_id = r.id";
    return new LegacyFileTable(
        "report_run_file (STR files)",
        ReportArchiveService.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select f.run_id as file_key, x.company_id, cast(r.id as varchar) as owner_id, '"
            + RECORD_CLASS
            + "' as record_class, r.report_code as document_type,"
            + " coalesce(r.file_name, x.file_name) as file_name, r.content_type,"
            + " cast(null as varchar) as recorded_sha256, f.content"
            + from
            + " where r.stored_file_id is null and f.run_id > ? order by f.run_id limit ?",
        "update report_run set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(r.stored_file_id) as moved" + from);
  }
}
